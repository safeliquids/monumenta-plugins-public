package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.HitKnockbackVulnerability;
import com.playmonumenta.plugins.effects.ImpactVulnerability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Impact implements Enchantment {

	public static final EnumSet<DamageEvent.DamageType> TRIGGERING_DAMAGE_TYPES = EnumSet.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.PROJECTILE
	);
	public static final EnumSet<ClassAbility> TRIGGERING_ABILITIES = EnumSet.of(
		ClassAbility.ERUPTION,
		ClassAbility.QUIVER_STORM
	);

	private static final double DAMAGE_PER_LEVEL = 0.1;
	private static final Particle.DustOptions DUST_OPTIONS = new Particle.DustOptions(Color.WHITE, 1.2f);
	private static final String EFFECT_ID = "ImpactVulnerability";
	private static final String KB_EFFECT_ID = "ImpactKBVulnerability";
	private static final String PROJECTILE_METAKEY = "ImpactProjectileHitThisTick";

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.IMPACT;
	}

	@Override
	public String getName() {
		return "Impact";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.DAMAGING_ENCHANTMENT;
	}

	@Override
	public void onDamageDelayed(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (enemy instanceof Player
			|| event.isCancelled()
			|| event.getAbility() == ClassAbility.IMPACT
			|| (event.getType() == DamageEvent.DamageType.PROJECTILE
			&& !MetadataUtils.checkOnceThisTick(plugin, enemy, PROJECTILE_METAKEY)) // Prevent Volley from applying multiple Impacts
		) {
			return;
		}
		if (AbilityUtils.isChargedAspectTriggeringEvent(event, player)) {
			applyImpact(plugin, player, value, event.getDamage(), enemy);
		} else {
			return;
		}

		// The KB resistance needs to be applied before abilities are cast
		if (!EntityUtils.isCCImmuneMob(enemy) && !EntityUtils.isTrainingDummy(enemy) && enemy.hasGravity() && enemy.hasAI()) {
			plugin.mEffectManager.addEffect(enemy, KB_EFFECT_ID, new HitKnockbackVulnerability(80, -0.1 * value));
		}

	}


	private void applyImpact(Plugin plugin, Player player, double value, double damaage, LivingEntity enemy) {
		Vector kbDirection = player.getEyeLocation().getDirection();

		//Apply the effect 1 tick later to avoid making the attack that applies the effect cancel it
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mEffectManager.addEffect(enemy, EFFECT_ID, new ImpactVulnerability(80));
			}
		}.runTaskLater(plugin, 1);


		new BukkitRunnable() {

			double mFallDistanceLastTick = EntityUtils.getEntityStackBase(enemy).getFallDistance();

			@Override
			public void run() {

				Effect impactEffect = plugin.mEffectManager.getActiveEffect(enemy, EFFECT_ID);
				if (impactEffect == null || impactEffect.getDuration() <= 0) {
					this.cancel();
					return;
				}
				if (checkForImpact(mFallDistanceLastTick, kbDirection, enemy)) {
					onImpact(player, enemy, damaage, (int) value);
					plugin.mEffectManager.clearEffects(enemy, EFFECT_ID);
					this.cancel();
				}

				mFallDistanceLastTick = EntityUtils.getEntityStackBase(enemy).getFallDistance();

				double widthDelta = PartialParticle.getWidthDelta(enemy);
				double heightDelta = PartialParticle.getHeightDelta(enemy);

				new PartialParticle(
					Particle.REDSTONE,
					LocationUtils.getHeightLocation(enemy, 0.6),
					4,
					widthDelta,
					heightDelta / 2,
					widthDelta,
					DUST_OPTIONS
				).spawnAsEnemy();


			}

		}.runTaskTimer(plugin, 1, 2);
	}

	private boolean checkForImpact(double fallDistanceLastTick, Vector direction, LivingEntity target) {

		if (fallDistanceLastTick > 2.0 && target.isOnGround()) {

			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(target, 0.1),
				1
			).spawnAsEnemy();

			return true;
		}

		BoundingBox offsetBox = target.getBoundingBox().clone();
		offsetBox.shift(direction.getX(), 0.15, direction.getZ());
		offsetBox.expand(0.1, -0.4, 0.1);

		if (LocationUtils.collidesWithBlocks(offsetBox, target.getWorld())) {

			new PartialParticle(
				Particle.EXPLOSION_LARGE,
				LocationUtils.getHeightLocation(target, 0.65),
				1
			).spawnAsEnemy();

			return true;
		}


		return false;
	}

	private void onImpact(Player player, LivingEntity target, double originalDamage, int level) {

		double finalDamage = originalDamage * DAMAGE_PER_LEVEL * level;

		DamageUtils.damage(player, target, DamageEvent.DamageType.UNSCALABLE_ENCH, finalDamage, ClassAbility.IMPACT, true);

		World world = target.getWorld();
		world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.5f, 1.35f);
		world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.5f, 0.5f);
		world.playSound(target.getLocation(), Sound.BLOCK_POINTED_DRIPSTONE_LAND, SoundCategory.PLAYERS, 1.7f, 0.5f);
		world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.7f, 0.5f);

	}
}
