package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.HitKnockbackVulnerability;
import com.playmonumenta.plugins.effects.ImpactVulnerability;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Rebound implements Enchantment {
	private static final float DAMAGE_PER_LEVEL = 5f;
	public static final float KB_VEL = 0.8f;
	public static final float VERTICAL_LAUNCH = 0.2f;
	public static final int RADIUS = 4;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REBOUND;
	}

	@Override
	public String getName() {
		return "Rebound";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.isBlockedByShield()) {
			checkShieldStatus(plugin, player);
		}
	}

	@Override
	public void onDamageShielded(Plugin plugin, Player player, double value, DamageShieldedEvent event) {
		checkShieldStatus(plugin, player);
	}

	private void activate(Plugin plugin, Player player, double value, ItemStack shield) {
		//get enchant levels on shield. Consider offhand, but if your mainhand shield has rebound then consider the mainhand instead

		int fire = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.FIRE_ASPECT);
		int ice = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.ICE_ASPECT);
		int thunder = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.THUNDER_ASPECT);
		int decay = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.DECAY);
		int bleed = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.BLEEDING);
		int wind = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.WIND_ASPECT);
		int knockback = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.KNOCKBACK);
		int impact = ItemStatUtils.getEnchantmentLevel(shield, EnchantmentType.IMPACT);

		float speed = KB_VEL + 0.4f * knockback;

		List<LivingEntity> hitMobs = EntityUtils.getNearbyMobs(player.getLocation(), RADIUS);

		if (!hitMobs.isEmpty()) {
			for (LivingEntity enemy : hitMobs) {
				// damage
				double damage = DAMAGE_PER_LEVEL * value;
				DamageUtils.damage(player, enemy, DamageEvent.DamageType.UNSCALABLE_ENCH, damage, null, false, true);

				// manually apply aspect effects.
				// Note: because rebound uniquely works in either hand, I've opted to not have it formally
				// be an aspect triggering event, but rather something that applies their effects on its own.
				// Otherwise it starts to like double dip if you have 2 shields, or even just a shield offhand. -Slyyam

				applyAspectEffects(plugin, player, enemy, fire, ice, thunder, decay, bleed, wind, plugin.mItemStatManager.getPlayerItemStats(player));

				//special abominable impact case
				Vector kbDirection = player.getEyeLocation().getDirection();
				if (impact > 0) {
					if (!EntityUtils.isBoss(enemy) && !EntityUtils.isCCImmuneMob(enemy) && !EntityUtils.isTrainingDummy(enemy) && enemy.hasGravity() && enemy.hasAI()) {
						plugin.mEffectManager.addEffect(enemy, "ImpactKBVulnerability", new HitKnockbackVulnerability(80, -0.1 * value));
					}
					Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.mEffectManager.addEffect(enemy, "ImpactVulnerability", new ImpactVulnerability(80)), 1);
					Vector finalKbDirection = kbDirection;
					new BukkitRunnable() {

						double mFallDistanceLastTick = EntityUtils.getEntityStackBase(enemy).getFallDistance();

						@Override
						public void run() {

							Effect impactEffect = plugin.mEffectManager.getActiveEffect(enemy, "ImpactVulnerability");
							if (impactEffect == null || impactEffect.getDuration() <= 0) {
								this.cancel();
								return;
							}
							if (Impact.checkForImpact(mFallDistanceLastTick, finalKbDirection, enemy)) {
								Impact.onImpact(player, enemy, damage, (int) value);
								plugin.mEffectManager.clearEffects(enemy, "ImpactVulnerability");
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
								new Particle.DustOptions(Color.WHITE, 1.2f)
							).spawnAsEnemy();
						}

					}.runTaskTimer(plugin, 1, 2);
				}

				//knockback (knocks forward, akin to aeroblast)
				if (kbDirection.length() < 0.001) {
					kbDirection = new Vector(0, VERTICAL_LAUNCH, 0);
				} else {
					kbDirection.normalize()
						.multiply(speed)
						.setY(VERTICAL_LAUNCH);
				}
				MovementUtils.knockAwayDirection(kbDirection, enemy, 0.5f);
			}
		}

		//visual and sound effects
		Location front = LocationUtils.getHalfHeightLocation(player).clone().add(player.getLocation().getDirection().normalize().multiply(0.5));
		player.getWorld().playSound(front, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.2f, 0.5f);
		new PartialParticle(Particle.EXPLOSION_LARGE, front, 10, 1.2, 1.2, 1.2).spawnAsPlayerActive(player);

		if (fire + ice + thunder + decay + bleed + wind == 0) {
			player.getWorld().playSound(front, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 2f);
			player.getWorld().playSound(front, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.6f);
			player.getWorld().playSound(front, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.5f, 1f);

			new PPExplosion(Particle.SMOKE_LARGE, front)
				.extra(0.7)
				.count(20)
				.spawnAsPlayerActive(player);
		}
		if (fire > 0) {
			player.getWorld().playSound(front, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1f);
			player.getWorld().playSound(front, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5f, 0.5f);

			new PartialParticle(Particle.LAVA, front, 25, 1, 1, 1).spawnAsPlayerActive(player);
		}
		if (ice > 0) {
			player.getWorld().playSound(front, Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.5f, 0.5f);
			player.getWorld().playSound(front, Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.5f, 1f);
			player.getWorld().playSound(front, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2f, 1.6f);
			player.getWorld().playSound(front, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.PLAYERS, 1.5f, 0.7f);

			new PPExplosion(Particle.ITEM_CRACK, front)
				.data(new ItemStack(Material.ICE))
				.extra(0.7)
				.count(80)
				.spawnAsPlayerActive(player);
		}
		if (thunder > 0) {
			player.getWorld().playSound(front, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 1f, 1f);
			player.getWorld().playSound(front, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS, 1f, 1.5f);
			player.getWorld().playSound(front, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 0.5f);

			new PartialParticle(Particle.FLASH, front, 1, 0, 0, 0).spawnAsPlayerActive(player);
			new PPExplosion(Particle.END_ROD, front)
				.extra(0.5)
				.count(30)
				.spawnAsPlayerActive(player);
		}
		if (decay > 0) {
			player.getWorld().playSound(front, Sound.ENTITY_SHULKER_DEATH, SoundCategory.PLAYERS, 1.5f, 1f);
			player.getWorld().playSound(front, Sound.ENTITY_SHULKER_BULLET_HURT, SoundCategory.PLAYERS, 1.5f, 0.5f);
			player.getWorld().playSound(front, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 1.2f, 1.6f);

			new PartialParticle(Particle.SQUID_INK, player.getLocation(), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);

			new PPExplosion(Particle.SQUID_INK, front)
				.extra(0.5)
				.count(40)
				.spawnAsPlayerActive(player);
		}
		if (bleed > 0) {
			player.getWorld().playSound(front, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.5f, 0.5f);
			player.getWorld().playSound(front, Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, SoundCategory.PLAYERS, 1.5f, 1f);
			player.getWorld().playSound(front, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.5f, 0.5f);

			new PPExplosion(Particle.ITEM_CRACK, front)
				.data(new ItemStack(Material.REDSTONE_BLOCK))
				.extra(0.5)
				.count(80)
				.spawnAsPlayerActive(player);
		}
		if (wind > 0) {
			player.getWorld().playSound(front, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.8f);
			player.getWorld().playSound(front, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1f, 2f);

			new PPExplosion(Particle.CLOUD, front)
				.extra(0.5)
				.count(30)
				.spawnAsPlayerActive(player);
		}
	}

	private void checkShieldStatus(Plugin plugin, Player player) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player.getCooldown(Material.SHIELD) > 0) {
				if (ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInMainHand(), EnchantmentType.REBOUND) > 0) {
					activate(plugin, player, ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInMainHand(), EnchantmentType.REBOUND), player.getInventory().getItemInMainHand());
				} else if (player.getInventory().getItemInMainHand().getType() != Material.SHIELD) {
					activate(plugin, player, ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInOffHand(), EnchantmentType.REBOUND), player.getInventory().getItemInOffHand());
				}
			}
		});
	}

	private void applyAspectEffects(Plugin plugin, Player player, LivingEntity enemy, int fire, int ice, int thunder, int decay, int bleed, int wind, @Nullable ItemStatManager.PlayerItemStats playerItemStats) {
		if (fire > 0) {
			FireAspect.apply(plugin, player, playerItemStats, FireAspect.FIRE_ASPECT_DURATION, enemy, DamageEvent.DamageType.UNSCALABLE_ENCH);
		}

		if (ice > 0) {
			IceAspect.apply(plugin, player, ice, IceAspect.ICE_ASPECT_DURATION, enemy, true);
		}

		if (thunder > 0) {
			//like eruption, guaranteed
			EntityUtils.applyStun(plugin, 6 * thunder, enemy);
		}

		if (decay > 0) {
			Decay.apply(plugin, enemy, Decay.DURATION, decay, player, DamageEvent.DamageType.UNSCALABLE_ENCH);
		}

		if (bleed > 0) {
			EntityUtils.applyBleed(plugin, player, enemy, bleed);
		}

		if (wind > 0) {
			WindAspect.launch(plugin, player, enemy, wind, DamageEvent.DamageType.UNSCALABLE_ENCH, false);
		}
	}
}
