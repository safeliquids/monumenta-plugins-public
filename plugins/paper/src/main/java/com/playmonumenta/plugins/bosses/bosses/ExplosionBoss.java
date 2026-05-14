package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class ExplosionBoss extends BossAbilityGroup {
	private static final String METAKEY_TAG = "ExplosionBossExplodedThisTick";
	public static final String identityTag = "boss_explosion";
	private final BukkitRunnable mExplosionRunnable;

	// A creeper will begin priming if a player is in 3 blocks with LoS
	// Will stop priming if the target player is not within 7 blocks of LoS. (ExtraRadius)

	private static final EntityTargets CREEPER_TARGET =
		new EntityTargets(EntityTargets.TARGETS.PLAYER, 3,
			EntityTargets.Limit.DEFAULT,
			List.of(EntityTargets.PLAYERFILTER.IS_TARGET, EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT),
			EntityTargets.TagsListFiter.DEFAULT);

	private final EntityTargets mTargetsExtraRadius;

	@BossParam(help = "Causes the mob to act like a creeper")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Power of the explosion")
		public double POWER = 3;

		@BossParam(help = "Should the explosion cause fire?")
		public boolean FIRE = false;

		@BossParam(help = "Should the explosion break blocks?")
		public boolean GRIEF = true;

		@BossParam(help = "Delay before the first explosion")
		public int DELAY = 40;

		@BossParam(help = "Delay before the explosion")
		public int DURATION = 40;

		@BossParam(help = "Damage of the explosion. Note that this is damage at point blank range.")
		public double DAMAGE = -1;

		@BossParam(help = "Range in blocks that the boss searches for players before casting")
		public int DETECTION = 0;

		@BossParam(help = "Range in blocks that the boss searches for players before casting")
		public EntityTargets TARGETS = CREEPER_TARGET;

		@BossParam(help = "Should it move while preparing the explosion")
		public boolean CAN_MOVE = false;

		@BossParam(help = "Effect applied to players hit by the nova")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "The extra radius in order to leave the trigger radius")
		public double EXTRA_RADIUS = 4;

		@BossParam(help = "The radius of the particle ring")
		public double PARTICLE_RADIUS = 5;

		@BossParam(help = "Sound used when charging the ability. If 0, automatically adjust based on charge.")
		public SoundsList SOUND_CHARGE = SoundsList.EMPTY;

		@BossParam(help = "Frequency for sound charge")
		public int SOUND_CHARGE_FREQUENCY = 1;

		@BossParam(help = "Particle summon on the air")
		public ParticlesList PARTICLE_AIR = ParticlesList.EMPTY;

		@BossParam(help = "Particle summon around the boss when loading the spell")
		public ParticlesList PARTICLE_LOAD = ParticlesList.EMPTY;

		@BossParam(help = "Sound used when the spell is casted (when explode)")
		public SoundsList SOUND_CAST = SoundsList.EMPTY;

		@BossParam(help = "Particle summoned when the spell explode")
		public ParticlesList PARTICLE_EXPLODE = ParticlesList.EMPTY;
	}

	ExplosionBoss.Parameters mP = BossParameters.getParameters(mBoss, identityTag, new ExplosionBoss.Parameters());

	public ExplosionBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mExplosionRunnable = createRunnable();
		mExplosionRunnable.runTaskTimer(mPlugin, mP.DELAY, 1);

		mTargetsExtraRadius = mP.TARGETS.clone().setRange(mP.TARGETS.getRange() + mP.EXTRA_RADIUS);

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mP.DETECTION, null);
	}

	private BukkitRunnable createRunnable() {
		return new BukkitRunnable() {
			int mT = 0;
			boolean mCharging = false;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				if (mT >= mP.DURATION) {
					explode();
					this.cancel();
					return;
				}

				Location loc = mBoss.getLocation();
				double ratio = 1 - (double) mT / mP.DURATION;

				if (mT > 0) {
					if (!mP.CAN_MOVE) {
						EntityUtils.selfRoot(mBoss, 3);
					}

					mP.PARTICLE_LOAD.spawn(mBoss, particle -> new PPCircle(particle, loc.clone().add(0, 1, 0),
						mP.PARTICLE_RADIUS * ratio));

					mP.PARTICLE_AIR.spawn(mBoss, loc.clone().add(0, 1, 0));
				}

				EntityTargets target = mCharging ? mTargetsExtraRadius : mP.TARGETS;
				boolean shouldTick = !EntityUtils.shouldPauseSpells(mBoss)
					&& !EntityUtils.shouldCancelSpells(mBoss)
					&& !target.getTargetsList(mBoss).isEmpty();

				if (shouldTick) {
					if (mT % mP.SOUND_CHARGE_FREQUENCY == 0) {
						mP.SOUND_CHARGE.playSoundsModified(cSound -> {
							double pitch = cSound.getPitch();
							cSound.setPitch((float) (pitch != 0 ? pitch : 2 * ratio));
						}, loc);
					}
					mCharging = true;
					mT++;
				} else {
					mCharging = false;
					mT = Math.max(0, mT - 1);
				}
			}
		};
	}

	private void explode() {
		if (mP.POWER > 0) {
			MetadataUtils.markThisTick(mPlugin, mBoss, METAKEY_TAG);
			mBoss.getLocation().createExplosion(mBoss, (float) mP.POWER, mP.FIRE, mP.GRIEF);
		}

		mP.SOUND_CAST.play(mBoss.getLocation());

		if (mP.DURATION != 0) {
			new BukkitRunnable() {
				final Location mLoc = mBoss.getLocation();
				double mBurstRadius = 0;

				@Override
				public void run() {
					for (int j = 0; j < 2; j++) {
						mBurstRadius += 1.5;
						mP.PARTICLE_EXPLODE.spawn(mBoss,
							particle -> new PPCircle(particle, mLoc, mBurstRadius));
					}
					if (mBurstRadius >= mP.PARTICLE_RADIUS) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}

		mBoss.remove();
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (event.getType().equals(DamageEvent.DamageType.BLAST)
			&& MetadataUtils.happenedThisTick(mBoss, METAKEY_TAG)) {
			double damage = EntityUtils.getAdjustedBlastDamage(mP.POWER, event.getBaseDamage(), mP.DAMAGE);
			mP.EFFECTS.apply(damagee, mBoss);
			event.setBaseDamage(damage);
		}
	}

	@Override
	public void unload() {
		super.unload();
		mExplosionRunnable.cancel();
	}
}
