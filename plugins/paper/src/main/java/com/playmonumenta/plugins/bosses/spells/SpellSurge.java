package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.SurgeBoss;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.function.Supplier;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSurge extends Spell {
	// For dramatic effect
	public static double GRAVITY = 30.0 / 20 / 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final SurgeBoss.Parameters mP;
	private final EntityTargets mExplosionTargets;

	public SpellSurge(Plugin plugin, LivingEntity boss, SurgeBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mP = parameters;
		mExplosionTargets = mP.TARGETS.clone().setRange(mP.EXPLOSION_RADIUS);

		mP.PROJECTILE_INTERVAL = Math.max(0, mP.PROJECTILE_INTERVAL);
	}

	@Override
	public void run() {
		mP.TARGETS.getTargetsList(mBoss).forEach(e ->
			spawnSurge(mBoss.getLocation(), mP.TRACKING ? e::getLocation : mBoss::getLocation, mP.PROJECTILE_COUNT, mP.RANGE)
		);
	}

	private void spawnSurge(Location origin, Supplier<Location> targetLoc, int count, double range) {
		if (mP.PROJECTILE_INTERVAL <= 0) {
			mP.SOUND_THROW.play(origin);

			for (int i = 0; i < count; i++) {
				Location loc = targetLoc.get();

				Location fallLoc = LocationUtils.randomLocationInCircle(loc, range - mP.EXPLOSION_RADIUS);
				if (fallLoc.getBlock().isSolid()) {
					fallLoc = LocationUtils.emergeFromGround(fallLoc, fallLoc.getY() + 10);
				} else {
					fallLoc = LocationUtils.fallToGround(fallLoc, fallLoc.getY() - 10);
				}

				summonProjectile(origin, fallLoc);
			}
			return;
		}

		mActiveTasks.add(new BukkitRunnable() {
			private final int mChargeTime = count;
			int mTicks = 0;

			@Override
			public void run() {
				mP.SOUND_THROW.play(origin);

				Location fallLoc = LocationUtils.randomLocationInCircle(targetLoc.get(), range - mP.EXPLOSION_RADIUS);
				if (fallLoc.getBlock().isSolid()) {
					fallLoc = LocationUtils.emergeFromGround(fallLoc, fallLoc.getY() + 10);
				} else {
					fallLoc = LocationUtils.fallToGround(fallLoc, fallLoc.getY() - 10);
				}

				summonProjectile(origin, fallLoc);

				mTicks++;
				if (mTicks >= mChargeTime) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, mP.PROJECTILE_INTERVAL));
	}

	public void summonProjectile(Location startLoc, Location fallLoc) {
		mActiveTasks.add(new BukkitRunnable() {
			private final Location mCurrentLocation = startLoc.clone();
			private final Vector mVelocity = new Vector(
				(fallLoc.x() - startLoc.x()) / mP.DURATION,
				(fallLoc.y() - startLoc.y()) / mP.DURATION + GRAVITY / 2 * mP.DURATION,
				(fallLoc.z() - startLoc.z()) / mP.DURATION
			);
			private int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= mP.DURATION) {
					for (ParticlesList.CParticle particle : mP.PARTICLE_EXPLOSION.getParticleList()) {
						new PPCircle(particle.mParticle, fallLoc.clone().add(0, 0.15, 0), 0.5)
							.count(particle.mCount)
							.rotateDelta(true)
							.directionalMode(true)
							.delta(1, 0, 0)
							.extra(particle.mVelocity * mP.EXPLOSION_RADIUS)
							.spawnAsEntityActive(mBoss);
					}
					mP.SOUND_EXPLOSION.play(fallLoc);

					for (LivingEntity hitEntity : mExplosionTargets.getTargetsListByLocation(mBoss, fallLoc)) {
						if (mP.DAMAGE > 0) {
							if (mP.BLOCKABLE) {
								BossUtils.blockableDamage(mBoss, hitEntity, mP.DAMAGE_TYPE, mP.DAMAGE, !mP.RESPECT_IFRAMES, false, mP.SPELL_NAME, fallLoc);
							} else {
								DamageUtils.damage(mBoss, hitEntity, mP.DAMAGE_TYPE, mP.DAMAGE, null, !mP.RESPECT_IFRAMES, false, mP.SPELL_NAME);
							}
						}
						if (mP.DAMAGE_PERCENTAGE > 0) {
							BossUtils.bossDamagePercent(mBoss, hitEntity, mP.DAMAGE_PERCENTAGE, fallLoc, false, mP.SPELL_NAME, false);
						}
						MovementUtils.knockAway(fallLoc, hitEntity, mP.KB_XZ, mP.KB_Y);
						mP.SPAWNED_MOB_POOL.spawn(fallLoc);
					}

					this.cancel();
					return;
				}

				for (ParticlesList.CParticle particle : mP.PARTICLE_TELEGRAPH_CIRCLE.getParticleList()) {
					new PPCircle(particle.mParticle, fallLoc.clone().add(0, 0.15, 0), mP.EXPLOSION_RADIUS)
						.count(particle.mCount)
						.delta(particle.mDx, particle.mDy, particle.mDz)
						.extra(particle.mVelocity)
						.spawnAsEntityActive(mBoss);
				}

				Location prevLoc = mCurrentLocation.clone();
				Location newLoc = mCurrentLocation.add(mVelocity);
				for (ParticlesList.CParticle particle : mP.PARTICLE_PROJECTILE.getParticleList()) {
					new PPLine(particle.mParticle, prevLoc, newLoc)
						.countPerMeter(particle.mCount)
						.delta(particle.mDx, particle.mDy, particle.mDz)
						.extra(particle.mVelocity)
						.spawnAsBoss();
				}
				mVelocity.setY(mVelocity.getY() - GRAVITY);
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public int cooldownTicks() {
		return mP.COOLDOWN;
	}
}
