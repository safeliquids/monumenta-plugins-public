package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.DoublePredicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Slab;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSpatialShattering extends Spell implements CooldownReducible {
	private static final int FIRST_TELEGRAPH_GRACE = 20;
	private static final int TELEGRAPH_DURATION = 2 * 20;
	private static final int ATTACK_INTERVAL = TELEGRAPH_DURATION + 10;
	private static final int HALF_HEIGHT = 5;
	private static final int COUNT = 3;
	private static final int DAMAGE = 50;
	private static final double DAMAGE_PERCENT = 0.25;
	private static final String SPELL_NAME = "Spatial Shattering";
	private static final String SLOW_SOURCE = "SpatialShatteringSlow";
	private static final String RESIST_SOURCE = "SpatialShatteringResist";
	private static final Particle.DustOptions TELEGRAPH_COLOR = new Particle.DustOptions(Color.fromRGB(0xef3642), 1.8f);
	private static final BlockData FULL_DATA = Material.CRIMSON_HYPHAE.createBlockData();
	private static final BlockData SLAB_DATA = Material.CRIMSON_SLAB.createBlockData();

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final float mYawOffset;

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellSpatialShattering(Plugin plugin, LivingEntity boss, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mYawOffset = mCenter.getYaw() + 22.5f;
		mSpellCooldownManager = new SpellCooldownManager(60 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}

	@Override
	public void run() {
		mSpellCooldownManager.setOnCooldown();

		World world = mBoss.getWorld();
		int duration = FIRST_TELEGRAPH_GRACE + COUNT * ATTACK_INTERVAL;
		mPlugin.mEffectManager.addEffect(mBoss, SLOW_SOURCE, new PercentSpeed(duration, -0.6f, SLOW_SOURCE));
		mPlugin.mEffectManager.addEffect(mBoss, RESIST_SOURCE, new PercentDamageReceived(duration, 0.5f));
		mBoss.setPose(Pose.SNEAKING, true);

		world.playSound(mCenter, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 5.0f, 1.5f);
		world.playSound(mCenter, Sound.BLOCK_END_PORTAL_FRAME_FILL, SoundCategory.HOSTILE, 3.0f, 1.7f);
		world.playSound(mCenter, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.HOSTILE, 3.0f, 0.6f);
		world.playSound(mCenter, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.HOSTILE, 2.5f, 0.1f);
		world.playSound(mCenter, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 4.0f, 1.6f);
		world.playSound(mCenter, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 4.0f, 0.9f);

		new PartialParticle(Particle.END_ROD, mBoss.getLocation())
			.count(32)
			.extra(1.2)
			.spawnAsBoss();

		world.playSound(mCenter, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 4.0f, 0.6f);
		world.playSound(mCenter, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.HOSTILE, 4.0f, 0.6f);
		world.playSound(mCenter, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 4.0f, 1.6f);
		world.playSound(mCenter, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 4.0f, 0.8f);

		startSlash(true, true);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 1;

			@Override
			public void run() {
				if (mTicks >= COUNT) {
					this.cancel();
					return;
				}
				startSlash(mTicks % 2 == 0, false);
				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				mBoss.setPose(Pose.STANDING);
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, FIRST_TELEGRAPH_GRACE + ATTACK_INTERVAL, ATTACK_INTERVAL);
	}

	private void startSlash(boolean invert, boolean first) {
		World world = mBoss.getWorld();

		world.playSound(mCenter, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 2.5f, 1.33f);
		world.playSound(mCenter, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 2.0f, 1f);
		world.playSound(mCenter, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.HOSTILE, 1.8f, 0.5f);
		world.playSound(mCenter, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2.2f, 1.65f);
		world.playSound(mCenter, Sound.ENTITY_WARDEN_DEATH, SoundCategory.HOSTILE, 1.8f, 1.7f);
		world.playSound(mCenter, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 2.0f, 0.6f);

		List<Block> blocks = BlockUtils.getBlocksInCylinder(mCenter.clone().add(0, 1, 0), Aurora.ARENA_RADIUS, 3).stream()
			.filter(block -> Aurora.isBreakableMaterial(block.getType()) && !block.getRelative(BlockFace.UP).isSolid())
			.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
		Collections.shuffle(blocks);

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = first ? -FIRST_TELEGRAPH_GRACE : 0;

			@Override
			public void run() {
				DoublePredicate anglePredicate = angle -> FastUtils.wrapMod(angle - mYawOffset, 90) < 45 ^ invert;

				if (mTicks >= TELEGRAPH_DURATION) {
					mBoss.swingMainHand();

					world.playSound(mCenter, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 5.0f, 0.33f);
					world.playSound(mCenter, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 5.0f, 1.4f);
					world.playSound(mCenter, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 5.0f, 1.3f);
					world.playSound(mCenter, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 5.0f, 0.6f);
					world.playSound(mCenter, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 5.0f, 1.7f);

					new PPCircle(Particle.SQUID_INK, mCenter.clone().add(0, 0.0, 0), Aurora.ARENA_INNER_RADIUS)
						.countPerMeter(7)
						.innerRadiusFactor(0)
						.anglePredicate(anglePredicate)
						.delta(0, 1, 0)
						.extra(0.3)
						.spawnAsBoss();

					//-------------------//

					new PPCircle(Particle.SQUID_INK, mCenter.clone().add(0, 1.0, 0), Aurora.ARENA_SEMI_OUTER_RADIUS)
						.countPerMeter(7)
						.innerRadiusFactor((double) Aurora.ARENA_INNER_RADIUS / Aurora.ARENA_SEMI_OUTER_RADIUS)
						.anglePredicate(anglePredicate.negate())
						.delta(0, 1, 0)
						.extra(0.3)
						.spawnAsBoss();

					//-------------------//

					new PPCircle(Particle.SQUID_INK, mCenter.clone().add(0, 2.0, 0), Aurora.ARENA_RADIUS)
						.countPerMeter(7)
						.innerRadiusFactor((double) Aurora.ARENA_SEMI_OUTER_RADIUS / Aurora.ARENA_RADIUS)
						.anglePredicate(anglePredicate)
						.extra(0.3)
						.spawnAsBoss();

					//-------------------//

					new PPPillar(Particle.END_ROD, mCenter.clone(), HALF_HEIGHT)
						.count(35)
						.spawnAsBoss();

					Aurora.playersInRange(mCenter).stream().filter(player -> {
						double dist = LocationUtils.xzDistance(player.getLocation(), mCenter);
						Vector vector = player.getLocation().subtract(mCenter).toVector().setY(0);
						boolean inAnglePrediacte = anglePredicate.test(Math.toDegrees(Math.atan2(vector.getX(), vector.getZ())));

						return inAnglePrediacte == (dist <= Aurora.ARENA_INNER_RADIUS || dist > Aurora.ARENA_SEMI_OUTER_RADIUS);
					}).forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, true, SPELL_NAME);
						DamageUtils.damagePercentHealth(mBoss, player, DAMAGE_PERCENT, false, false, SPELL_NAME);
					});

					this.cancel();
					return;
				}
				if (mTicks >= 0) {
					world.playSound(mCenter, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, SoundCategory.HOSTILE, 5.0f, 1.5f * mTicks / TELEGRAPH_DURATION);
					if (mTicks % 5 == 0) {
						world.playSound(mCenter, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 5.0f, 1.2f * mTicks / TELEGRAPH_DURATION);
					}
				}
				if (mTicks % 10 == 0) {
					new PPCircle(Particle.REDSTONE, mCenter.clone().add(0, 0.1, 0), Aurora.ARENA_INNER_RADIUS)
						.data(TELEGRAPH_COLOR)
						.countPerMeter(5)
						.distanceFalloff(Aurora.ARENA_RADIUS)
						.innerRadiusFactor(0)
						.anglePredicate(anglePredicate)
						.randomizeAngle(false)
						.spawnAsBoss();

					new PPCircle(Particle.REDSTONE, mCenter.clone().add(0, 1.1, 0), Aurora.ARENA_SEMI_OUTER_RADIUS)
						.data(TELEGRAPH_COLOR)
						.countPerMeter(5)
						.distanceFalloff(Aurora.ARENA_RADIUS)
						.innerRadiusFactor((double) Aurora.ARENA_INNER_RADIUS / Aurora.ARENA_SEMI_OUTER_RADIUS)
						.anglePredicate(anglePredicate.negate())
						.randomizeAngle(false)
						.spawnAsBoss();

					new PPCircle(Particle.REDSTONE, mCenter.clone().add(0, 2.1, 0), Aurora.ARENA_RADIUS)
						.data(TELEGRAPH_COLOR)
						.countPerMeter(5)
						.distanceFalloff(Aurora.ARENA_RADIUS)
						.innerRadiusFactor((double) Aurora.ARENA_SEMI_OUTER_RADIUS / Aurora.ARENA_RADIUS)
						.anglePredicate(anglePredicate)
						.randomizeAngle(false)
						.spawnAsBoss();
				}
				if (mTicks >= 0 && mTicks % 5 == 0) {
					int size = blocks.size();
					int fromIndex = (int) (size * Math.sqrt((double) mTicks / TELEGRAPH_DURATION));
					int toIndex = Math.min((int) (size * Math.sqrt((double) (mTicks + 5) / TELEGRAPH_DURATION)), size);

					for (Block block : blocks.subList(fromIndex, toIndex)) {
						Location displacement = block.getLocation().add(0.5, 0, 0.5).subtract(mCenter);
						displacement.setY(0);
						double dist = displacement.length();
						boolean inAnglePredicate = anglePredicate.test(Math.toDegrees(Math.atan2(displacement.x(), displacement.z())));
						if ((dist <= Aurora.ARENA_INNER_RADIUS - 0.5 || dist > Aurora.ARENA_SEMI_OUTER_RADIUS - 0.5) == inAnglePredicate) {
							TemporaryBlockChangeManager.INSTANCE.changeBlock(block, block.getBlockData() instanceof Slab ? SLAB_DATA : FULL_DATA, TELEGRAPH_DURATION - mTicks);
						}
					}
				}

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public void cancel() {
		super.cancel();
		mPlugin.mEffectManager.clearEffects(mBoss, SLOW_SOURCE);
		mPlugin.mEffectManager.clearEffects(mBoss, RESIST_SOURCE);
	}

	@Override
	public int cooldownTicks() {
		return COUNT * TELEGRAPH_DURATION + Aurora.SPELL_INTERVAL;
	}

	@Override
	public void reduceCooldown(int reduction) {
		mSpellCooldownManager.reduceCooldown(reduction);
	}
}
