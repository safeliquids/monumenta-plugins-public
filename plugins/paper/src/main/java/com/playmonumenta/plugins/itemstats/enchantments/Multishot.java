package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.abilities.scout.ranger.GaleShot;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.MultishotEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;

public class Multishot implements Enchantment {

	public static final String MULTISHOT_SIDE_METADATA = "MultishotSideProjectile";
	public static final String MULTISHOT_CENTRAL_METADATA = "MultishotCentralProjectile";
	public static final String MULTISHOT_HIT_ENEMY_METADATA = "MultishotStruckEnemy";
	public static final int MULTISHOT_SPACING = 10;

	private static final Set<String> DISALLOWED_METADATA = Set.of(
		MULTISHOT_CENTRAL_METADATA,
		MULTISHOT_SIDE_METADATA,
		QuiverStorm.ARROW_METADATA,
		GaleShot.GALE_SHOT_PROJECTILE_METAKEY
	);

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MULTISHOT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public String getName() {
		return "Multishot";
	}

	@Override
	public double getPriorityAmount() {
		return 473; // Before Piercing
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (Volley.isVolleyShot(player) || event.isCancelled()) {
			return;
		}
		for (String metadata : DISALLOWED_METADATA) {
			if (projectile.hasMetadata(metadata)) {
				return;
			}
		}
		int effectiveLevel = (int) value;
		MultishotEffect multishotEffect = EffectManager.getInstance().getActiveEffect(player, MultishotEffect.class);
		if (multishotEffect != null && MultishotEffect.PERMISSIBLE_PROJECTILE_TYPES.contains(projectile.getType())) {
			effectiveLevel += (int) multishotEffect.getMagnitude();
		}

		if (effectiveLevel < 1) {
			return;
		}
		// Technically this should be wrapped in a check (disable-relative-projectile-velocity is false)
		// But on Monumenta servers this setting is always false
		Vector playerVelocity = player.getVelocity();
		if (PlayerUtils.isOnGround(player)) {
			playerVelocity.setY(0);
		}
		Vector centralDirection = projectile.getVelocity();
		centralDirection.subtract(playerVelocity);

		for (int i = 1; i <= effectiveLevel; i++) {
			for (int j = -1; j <= 1; j += 2) {
				double yaw = MULTISHOT_SPACING * j * i;

				Location loc = player.getEyeLocation();

				// Start with the assumption the player is facing due South (yaw 0.0, pitch 0.0, no offset, speed of 1.0)
				Vector dir = new Vector(0.0, 0.0, 1.0);
				// Apply yaw offset to get arrow pattern
				dir = VectorUtils.rotateYAxis(dir, yaw);
				// Apply player pitch/yaw to rotate that pattern to match the arrow's direction
				double[] originalDir = VectorUtils.vectorToRotation(centralDirection);
				dir = VectorUtils.rotateXAxis(dir, originalDir[1]); // Pitch
				dir = VectorUtils.rotateYAxis(dir, originalDir[0]); // Yaw

				// Change the location's direction to match the arrow's direction
				loc.setDirection(dir);

				// Spawn the arrow at the specified location, direction, and speed
				// Duplicate the entity via NMS to preserve metadata (earrows, qstorm, etc)
				Projectile multishotProj = NmsUtils.getVersionAdapter().duplicateEntity(projectile);
				MetadataUtils.removeMetadata(multishotProj, DamageListener.DO_NOT_REPLACE_METADATA);
				double speed = ItemUtils.getVanillaProjectileSpeed(player.getInventory().getItemInMainHand())
					* PlayerUtils.calculateBowDraw(projectile, 1);
				Vector velocity = dir.normalize().multiply(speed);
				multishotProj.setVelocity(velocity);
				multishotProj.setShooter(player);

				// Technically this should be wrapped in a check (disable-relative-projectile-velocity is false)
				// But on Monumenta servers this setting is always false
				multishotProj.setVelocity(multishotProj.getVelocity().add(playerVelocity));

				AbilityUtils.inheritProjectileStats(player, multishotProj, projectile);
				MetadataUtils.setMetadata(multishotProj, MULTISHOT_SIDE_METADATA, Bukkit.getServer().getCurrentTick());

				if (multishotProj instanceof AbstractArrow arrow) {
					arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
					arrow.setCritical(projectile instanceof AbstractArrow projectileArrow && projectileArrow.isCritical());
				} else if (multishotProj instanceof ThrowableProjectile throwable && projectile instanceof ThrowableProjectile oldThrowable) {
					ItemUtils.setSnowballItem(throwable, oldThrowable.getItem());
				}

				ProjectileLaunchEvent multishotLaunch = new ProjectileLaunchEvent(multishotProj);
				Bukkit.getPluginManager().callEvent(multishotLaunch); // ProjectileSpeed modifies the velocity here
			}
		}

		MetadataUtils.setMetadata(projectile, MULTISHOT_CENTRAL_METADATA, Bukkit.getServer().getCurrentTick());
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		Entity proj = event.getDamager();
		int projLaunchTick = -1;
		if (proj instanceof Projectile) {
			if (proj.hasMetadata(MULTISHOT_SIDE_METADATA)) {
				projLaunchTick = proj.getMetadata(MULTISHOT_SIDE_METADATA).getFirst().asInt();
			} else if (proj.hasMetadata(MULTISHOT_CENTRAL_METADATA)) {
				projLaunchTick = proj.getMetadata(MULTISHOT_CENTRAL_METADATA).getFirst().asInt();
			}
		}
		if (projLaunchTick != -1) {
			if (enemy.hasMetadata(MULTISHOT_HIT_ENEMY_METADATA)
				&& enemy.getMetadata(MULTISHOT_HIT_ENEMY_METADATA).getFirst().asInt() == projLaunchTick) {
				event.setCancelled(true);
			} else {
				MetadataUtils.setMetadata(enemy, MULTISHOT_HIT_ENEMY_METADATA, projLaunchTick);
			}
		}
	}
}
