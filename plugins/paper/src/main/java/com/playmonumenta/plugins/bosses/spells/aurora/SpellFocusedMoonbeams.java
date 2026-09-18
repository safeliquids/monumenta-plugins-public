package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SpellFocusedMoonbeams extends Spell {
	private static final int CRYSTAL_DELAY = 30;
	private static final int INTERVAL = 8;
	private static final int MARGIN = 9;
	private static final Vector MOON_DISPLACEMENT = new Vector(64, 15.5, 0);
	private static final double MOON_DISTANCE = MOON_DISPLACEMENT.length();

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Location mRaisedCenter;
	private final Location mMoonLoc;
	private final int mSliceCount;
	private final Aurora.BlockDestroyer mBlockDestroyer;

	@Nullable
	private Entity mDisplay;

	public SpellFocusedMoonbeams(Plugin plugin, LivingEntity boss, Location center, int sliceCount, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRaisedCenter = center.clone().add(0, 10, 0);
		mMoonLoc = center.clone().add(MOON_DISPLACEMENT);
		mBlockDestroyer = blockDestroyer;
		mSliceCount = sliceCount;
	}

	@Override
	public void run() {
		World world = mCenter.getWorld();
		world.playSound(mCenter, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 3.0f, 0.7f);
		world.playSound(mCenter, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 3.0f, 0.5f);
		world.playSound(mCenter, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 3.0f, 0.8f);
		world.playSound(mCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 3.0f, 0.5f);
		world.playSound(mRaisedCenter, Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 4.0f, 0.1f);
		world.playSound(mRaisedCenter, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 3.0f, 0.6f);
		world.playSound(mRaisedCenter, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.HOSTILE, 3.0f, 0.6f);

		Location tpLoc = mRaisedCenter.clone().add(0, 1.5, 0);
		Aurora.fancyTp(mBoss, tpLoc, 4, () -> {
			mBoss.setGravity(false);
		});
		mBoss.setInvulnerable(true);

		new BukkitRunnable() {
			private final Vector mMoonDir = mMoonLoc.clone().subtract(mRaisedCenter).toVector().normalize();
			private final Vector mDisplacement = mMoonDir.clone().multiply(MOON_DISTANCE / CRYSTAL_DELAY / 20);

			private Location mCurrentLoc = mRaisedCenter.clone().add(mMoonDir.clone().multiply(MOON_DISTANCE));
			private Vector mRotatingVec = new Vector(0, 0, 1).crossProduct(mMoonDir).multiply(1.5);

			int mTicks = 0;

			@Override
			public void run() {
				world.playSound(mCenter, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.HOSTILE, 2.0f, mTicks * 0.1f);

				for (int i = 0; i < 20; i++) {
					mCurrentLoc = mCurrentLoc.subtract(mDisplacement);
					mRotatingVec = mRotatingVec.rotateAroundAxis(mMoonDir, Math.PI / 90);

					new PartialParticle(Particle.END_ROD, mCurrentLoc)
						.distanceFalloff(MOON_DISTANCE)
						.spawnAsBoss();
					new PartialParticle(Particle.SOUL_FIRE_FLAME, mCurrentLoc.clone().add(mRotatingVec))
						.extra(0.03)
						.distanceFalloff(MOON_DISTANCE)
						.spawnAsBoss();
					new PartialParticle(Particle.SOUL_FIRE_FLAME, mCurrentLoc.clone().subtract(mRotatingVec))
						.extra(0.03)
						.distanceFalloff(MOON_DISTANCE)
						.spawnAsBoss();
				}

				mTicks++;
				if (mTicks >= CRYSTAL_DELAY) {
					world.playSound(mCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2.0f, 0.8f);
					world.playSound(mCenter, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 2.0f, 1.2f);
					world.playSound(mCenter, Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 2.0f, 0.9f);

					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			for (int i = 0; i < 4; i++) {
				ParticleUtils.drawParticleCircleExplosion(
					mBoss,
					mRaisedCenter,
					0,
					0.5,
					FastUtils.randomDoubleInRange(0, 360),
					FastUtils.randomDoubleInRange(0, 360),
					60 - i * 2,
					0.8f - i * 0.1f,
					true,
					0,
					Particle.SOUL_FIRE_FLAME
				);
			}

			mDisplay = world.spawn(mRaisedCenter.clone().subtract(0, 0.5, 0), EnderCrystal.class, display -> {
				display.setInvulnerable(true);
				display.setShowingBottom(false);

				EntityUtils.setRemoveEntityOnUnload(display);
			});
		}, CRYSTAL_DELAY));
	}

	public void slice(int extraSlices, Runnable onFinish) {
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				drawRandomLine();
				mTicks++;

				if (mTicks >= mSliceCount + extraSlices) {
					mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						if (mDisplay != null) {
							mDisplay.remove();
						}
						mBoss.setInvulnerable(false);
						mBoss.setGravity(true);

						onFinish.run();
					}, 2 * 20));
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, INTERVAL));
	}

	private void drawRandomLine() {
		Vector dir = VectorUtils.randomHorizontalUnitVector();
		Location pointOnLine = mCenter.clone().add(VectorUtils.randomHorizontalUnitVector().multiply(FastUtils.randomDoubleInRange(MARGIN, Aurora.ARENA_RADIUS - MARGIN))).setDirection(dir);
		Location pointOnCircle = pointOnLine.add(dir.clone().multiply(LocationUtils.rayLengthToSphereSurface(mCenter, pointOnLine, Aurora.ARENA_RADIUS)));

		Vector revDir = dir.multiply(-1);
		double length = LocationUtils.rayLengthToSphereSurface(mCenter, pointOnCircle.setDirection(revDir), Aurora.ARENA_RADIUS);

		World world = mCenter.getWorld();
		world.playSound(mCenter, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 2.0f, 0.6f);
		world.playSound(mCenter, Sound.ENTITY_FIREWORK_ROCKET_BLAST_FAR, SoundCategory.HOSTILE, 5.0f, 0.1f);
		world.playSound(mCenter, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.HOSTILE, 3.0f, 1.2f);
		world.playSound(mCenter, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.HOSTILE, 5.0f, 1.8f);
		world.playSound(mCenter, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 5.0f, 0.5f);

		ParticleUtils.drawParticleCircleExplosion(
			mBoss,
			mRaisedCenter,
			0,
			0.5,
			FastUtils.randomDoubleInRange(0, 360),
			FastUtils.randomDoubleInRange(0, 360),
			40,
			0.6f,
			true,
			0,
			Particle.FLAME
		);

		HashSet<Block> blocks = new HashSet<>();
		Vector displacement = revDir.clone().multiply(0.5);
		Location changingLoc = pointOnCircle.clone().add(0, 3, 0);
		for (double i = 0; i < length; i += 0.5) {
			changingLoc.add(displacement);
			Location current = Aurora.withSurfaceY(changingLoc, mCenter);
			if (i % 2 == 0) {
				blocks.addAll(BlockUtils.getBlocksInPillar(current.clone().subtract(0, 4, 0), 2, 8));
			}
			if (i % 4 == 0) {
				world.playSound(current, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.4f, 1.5f);
				world.playSound(current, Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.5f, 0.1f);

				new PPLine(Particle.FLAME, mRaisedCenter, current)
					.countPerMeter(4)
					.distanceFalloff(32)
					.spawnAsBoss();

				new PartialParticle(Particle.EXPLOSION_LARGE, current).extra(2).spawnAsBoss();
			}
		}
		mBlockDestroyer.destroy(blocks);
	}

	@Override
	public void cancel() {
		if (mDisplay != null && mDisplay.isValid()) {
			mDisplay.remove();
		}
		super.cancel();
	}

	@Override
	public int cooldownTicks() {
		return CRYSTAL_DELAY + INTERVAL * mSliceCount + Aurora.SPELL_INTERVAL;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
