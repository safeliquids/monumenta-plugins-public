package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.cosmetics.skills.rogue.StarCosmeticsFunctions;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PPSpiral;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellSupernova extends Spell {
	private static final int LEVITATION_TIME = 3 * 20;
	private static final int START_DELAY = 30;
	private static final int SPIRAL_ROTATIONS = 5;
	private static final int DEGREE_INC = 3;
	private static final double K = Aurora.ARENA_RADIUS / (2.0 * SPIRAL_ROTATIONS) / Math.PI;
	private static final double HITBOX_SIZE = 0.3;
	private static final double DAMAGE_PERCENT = 0.08;
	private static final int ENRAGE_TIME = 40 * 20;
	private static final int FOCUS_RANGE = 8;
	private static final int HEALTH = 500;
	private static final String ENRAGE_NAME = "Supernova (☠)";
	private static final String SPELL_NAME = "Supernova";
	private static final List<List<Vector>> STARS = List.of(
		StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(4, 4.5, 1.7, false, false), 15),
		StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(4, 4.5, 2.0, false, false), 15),
		StarCosmeticsFunctions.interpolatePolygon(StarCosmeticsFunctions.generateStarVertices(5, 4.5, 1.5, false, false), 15)
	);
	private static final int BLAST_INTERVAL = 4 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Location mRaisedCenter;

	private final SpellStardustBlaster mStardustBlaster;
	private final ChargeUpManager mChargeUpManager;
	private final BossBar mFocusBar;

	public SpellSupernova(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRaisedCenter = mCenter.clone().add(0, 32, 0);
		mStardustBlaster = new SpellStardustBlaster(plugin, boss, mRaisedCenter, Aurora.ARENA_RADIUS, true, 1);
		mChargeUpManager = new ChargeUpManager(boss,
			ENRAGE_TIME,
			Component.text("Casting ", NamedTextColor.RED).append(Component.text(ENRAGE_NAME, NamedTextColor.LIGHT_PURPLE)),
			BossBar.Color.RED,
			BossBar.Overlay.PROGRESS,
			Aurora.DETECTION_RANGE
		);
		mFocusBar = BossBar.bossBar(Component.text("Delfia's Focus - 100%"), 1, BossBar.Color.PINK, BossBar.Overlay.PROGRESS);
	}

	@Override
	public void run() {
		run(() -> {
		});
	}

	public void run(Runnable onFinish) {
		mChargeUpManager.setTime(0);

		mBoss.setInvulnerable(true);
		mBoss.teleport(mRaisedCenter);

		new PartialParticle(Particle.SONIC_BOOM, mRaisedCenter).minimumCount(1).spawnAsBoss();
		new PartialParticle(Particle.SCULK_SOUL, mRaisedCenter)
			.count(150)
			.extra(0.5)
			.spawnAsBoss();
		new PartialParticle(Particle.END_ROD, mRaisedCenter)
			.count(100)
			.extra(0.4)
			.spawnAsBoss();

		EntityUtils.selfRoot(mBoss, ENRAGE_TIME);

		EntityUtils.getNearbyMobs(mCenter, Aurora.ARENA_RADIUS + 12, mBoss).forEach(Entity::remove);

		List<Player> players = Aurora.playersInRange(mCenter);
		double radianInc = 2 * Math.PI / players.size();

		World world = mBoss.getWorld();
		world.playSound(mRaisedCenter, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 9.0f, 0.9f);
		world.playSound(mRaisedCenter, Sound.BLOCK_PORTAL_TRAVEL, SoundCategory.HOSTILE, 9.0f, 0.9f);
		world.playSound(mRaisedCenter, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 9.0f, 1.25f);
		world.playSound(mRaisedCenter, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 9.0f, 0.5f);

		for (int i = 0; i < players.size(); i++) {
			Player player = players.get(i);
			mPlugin.mEffectManager.addEffect(player, "SpaceInversionImmunity", new DamageImmunity(LEVITATION_TIME, EnumSet.allOf(DamageEvent.DamageType.class)));
			mPlugin.mPotionManager.addPotion(player, PotionManager.PotionID.BOSS, new PotionEffect(PotionEffectType.LEVITATION, LEVITATION_TIME, 12));
			player.setVelocity(new Vector());

			// puts people in a safe spot
			Vector vec = new Vector(FastUtils.cos(radianInc * i), 0, -FastUtils.sin(radianInc * i)).multiply(Aurora.ARENA_RADIUS - i - 2);
			Location loc = mRaisedCenter.clone().add(vec);

			loc.setDirection(vec.multiply(-1));
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				player.teleport(loc);
				player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.5f, 0.1f);
				player.playSound(player, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.PLAYERS, 1.5f, 0.5f);

				player.setFireTicks(0);
				PotionUtils.clearNegatives(mPlugin, player);

				mPlugin.mPotionManager.addPotion(player, PotionManager.PotionID.BOSS, new PotionEffect(PotionEffectType.LEVITATION, ENRAGE_TIME, -1, false, false));
				player.sendMessage(Component.text("Aurora and Delfia are preparing a devastating attack, get to the center to break their focus!", NamedTextColor.GRAY));
				player.showBossBar(mFocusBar);
			}, LEVITATION_TIME);
		}

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 5.0f, 1.5f);
			world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 5.0f, 2.0f);
			world.playSound(mRaisedCenter, Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 5.0f, 0.1f);

			@Nullable
			Entity hitEntity = LibraryOfSoulsIntegration.summon(mRaisedCenter.clone().subtract(0, 1, 0), "DelfiasFocus");
			if (!(hitEntity instanceof LivingEntity livingEntity)) {
				MMLog.severe("Aurora: soul \"DelfiasFocus\" is not LivingEntity!");
				return;
			}

			double maxHp = Math.round(HEALTH * BossUtils.healthScalingCoef(players.size(), 0.3, 0.5));
			EntityUtils.setMaxHealthAndHealth(livingEntity, maxHp);
			BossManager.getInstance().manuallyRegisterBoss(livingEntity, new BossAbilityGroup(mPlugin, "boss_aurora_focus", livingEntity) {
				@Override
				public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
					if (LocationUtils.xzDistance(source.getLocation(), mRaisedCenter) > FOCUS_RANGE) {
						world.playSound(mRaisedCenter, Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 2.0f, 2.0f);
						new PartialParticle(Particle.FIREWORKS_SPARK, mRaisedCenter)
							.count(24)
							.extra(1)
							.spawnAsBoss();

						event.setCancelled(true);
						return;
					}
					world.playSound(mRaisedCenter, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 2.0f, 0.1f);
				}
			});

			Location particleCenter = mRaisedCenter.clone().add(0, 1, 0);

			new PPSpiral(Particle.END_ROD, particleCenter, Aurora.ARENA_RADIUS)
				.ticks(10)
				.curves(1)
				.count(1000)
				.curveAngle(360 * SPIRAL_ROTATIONS)
				.spawnAsBoss();

			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					double health = livingEntity.getHealth();
					double progress = Math.clamp(health / maxHp, 0, 1);
					mFocusBar.progress((float) progress);
					mFocusBar.name(Component.text(String.format("Delfia's Focus - %.0f%%", progress * 100), NamedTextColor.WHITE));

					int tick = mChargeUpManager.getTime();
					int angleOffset = DEGREE_INC * tick;
					if (tick % 3 == 0) {
						new PPSpiral(Particle.SOUL_FIRE_FLAME, particleCenter, Aurora.ARENA_RADIUS)
							.ticks(3)
							.curves(1)
							.count(360)
							.curveAngle(360 * SPIRAL_ROTATIONS)
							.angleOffset(angleOffset)
							.distanceFalloff(43)
							.spawnAsBoss();
					}

					new PPPillar(Particle.SCULK_CHARGE, mRaisedCenter, 2)
						.count((int) (60 * progress))
						.delta(0.5)
						.data(FastUtils.randomFloatInRange(0, 6.2f))
						.extra(0.1)
						.spawnAsBoss();

					new PPPillar(Particle.DAMAGE_INDICATOR, mRaisedCenter, 2)
						.count(20)
						.delta(1.25, 1, 1.25)
						.spawnAsBoss();

					new PPPillar(Particle.END_ROD, mRaisedCenter, 10)
						.count(10)
						.delta(0, 1, 0)
						.directionalMode(true)
						.extra(0.5)
						.spawnAsBoss();

					new PPPillar(Particle.END_ROD, mRaisedCenter.clone().subtract(0, 10, 0), 10)
						.count(10)
						.delta(0, -1, 0)
						.directionalMode(true)
						.extra(0.5)
						.spawnAsBoss();

					players.forEach(player -> {
						Location location = player.getLocation();
						if (Math.abs(location.getY() - mRaisedCenter.getY()) > 1) {
							location.setY(mRaisedCenter.getY());
							player.teleport(location);
						}
					});

					players.stream().filter(player -> {
						Vector displacement = player.getLocation().subtract(mRaisedCenter).toVector();
						Vector rotated = displacement.rotateAroundY(Math.toRadians(angleOffset));
						rotated.setY(0);
						double radial = rotated.length();
						if (radial <= 3.5) {
							return true;
						}
						if (radial >= Aurora.ARENA_RADIUS) {
							return false;
						}

						double angle = Math.atan2(rotated.getZ(), rotated.getX());
						double modulo = 2 * K * Math.PI;
						double rkt = (radial - K * angle) % modulo;
						if (rkt < 0) {
							rkt += modulo; // java's modulo sucks
						}
						if (rkt <= Math.PI) {
							return rkt <= HITBOX_SIZE;
						} else {
							return 2 * Math.PI * K - rkt <= HITBOX_SIZE;
						}
					}).forEach(player -> {
						BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT, SPELL_NAME);
						MovementUtils.knockAway(mRaisedCenter, player, 0.2f, 0, false);
					});
					players.removeIf(p -> !Aurora.isAlive(p));

					if (tick % BLAST_INTERVAL == 0) {
						mStardustBlaster.run();
					}

					if (health <= 0) {
						players.forEach(player -> {
							mPlugin.mEffectManager.addEffect(player, "SpaceInversionImmunity", new DamageImmunity(3 * 20, EnumSet.of(DamageEvent.DamageType.FALL)));
							mPlugin.mPotionManager.clearPotionEffectType(player, PotionEffectType.LEVITATION);
							player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 4 * 20, 0));
							player.setVelocity(new Vector(0, -1.0, 0));
						});
						onFinish.run();

						world.playSound(mRaisedCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4.0f, 2.0f);
						world.playSound(mRaisedCenter, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 4.0f, 0.8f);
						world.playSound(mRaisedCenter, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 4.0f, 0.1f);
						world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 4.0f, 1.0f);

						this.cancel();
						return;
					}
					if (mChargeUpManager.nextTick(2)) {
						world.playSound(mRaisedCenter, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4.0f, 0.1f);
						world.playSound(mRaisedCenter, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 4.0f, 1.0f);
						world.playSound(mRaisedCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 4.0f, 0.1f);
						world.playSound(mRaisedCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.5f, 500);
						world.playSound(mRaisedCenter, Sound.AMBIENT_CRIMSON_FOREST_MOOD, SoundCategory.HOSTILE, 9.0f, 1.5f, 500);

						new PartialParticle(Particle.EXPLOSION_LARGE, mRaisedCenter)
							.minimumCount(1)
							.extra(4)
							.spawnAsBoss();

						new PPExplosion(Particle.SOUL_FIRE_FLAME, mRaisedCenter)
							.count(350)
							.delta(0, 0.5, 0)
							.speed(2)
							.spawnAsBoss();

						new PPExplosion(Particle.SMOKE_LARGE, mRaisedCenter)
							.count(200)
							.delta(0, 1, 0)
							.speed(3)
							.spawnAsBoss();

						Bukkit.getScheduler().runTaskLater(mPlugin, () -> players.forEach(player -> PlayerUtils.killPlayer(player, mBoss, ENRAGE_NAME)), 20);
						this.cancel();
						return;
					}
					if (tick % 20 == 0) {
						double angle = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
						for (Vector vector : STARS.get((tick / 20) % 3)) {
							Location loc = mRaisedCenter.clone().add(vector.rotateAroundY(angle)).add(0, 1, 0);
							new PartialParticle(Particle.END_ROD, loc).spawnAsBoss();
						}
						world.playSound(mRaisedCenter, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.HOSTILE, 5.0f, 2.0f * tick / mChargeUpManager.getChargeTime());
						world.playSound(mRaisedCenter, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 5.0f, 1.0f * tick / mChargeUpManager.getChargeTime());
					}
				}


				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					hitEntity.remove();
					EntityUtils.cancelSelfRoot(mBoss);

					mChargeUpManager.remove();
					mStardustBlaster.cancel();
					mBoss.setGravity(true);
					mBoss.setInvulnerable(false);

					players.forEach(player -> player.hideBossBar(mFocusBar));
				}
			};
			runnable.runTaskTimer(mPlugin, START_DELAY, 2);
			mActiveRunnables.add(runnable);
		}, LEVITATION_TIME);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
