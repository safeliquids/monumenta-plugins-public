package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SoundBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_sound";

	public static class Parameters extends BossParameters {
		@BossParam(help = "sounds played when spawned")
		public SoundsList SPAWN_SOUND = SoundsList.EMPTY;

		@BossParam(help = "sounds played each AMBIENT_SOUND_TIMER ticks")
		public SoundsList AMBIENT_SOUND = SoundsList.EMPTY;

		@BossParam(help = "timer to play the sound AMBIENT_SOUND")
		public int AMBIENT_SOUND_TIMER = 20 * 10;

		@BossParam(help = "sounds played when hurt by a player")
		public SoundsList PLAYER_HURT = SoundsList.EMPTY;

		@BossParam(help = "sounds played when hurt by ambient")
		public SoundsList AMBIENT_HURT = SoundsList.EMPTY;

		@BossParam(help = "sounds played when hurt by other mobs")
		public SoundsList MOB_HURT = SoundsList.EMPTY;

		@BossParam(help = "sounds played each time the mobs does a step")
		public SoundsList STEP_SOUND = SoundsList.EMPTY;

		@BossParam(help = "distance the mobs must travel to play a footstep")
		public double STEP_SOUND_DISTANCE = 1F;

		@BossParam(help = "sounds played when the mob dies")
		public SoundsList DEATH_SOUND = SoundsList.EMPTY;
	}

	private final Parameters mParams;

	private final double DISTANCE_TRAVELED_FACTOR = 0.6;

	public SoundBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setSilent(true);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		mParams.SPAWN_SOUND.play(mBoss.getLocation());

		List<Spell> spellList;
		spellList = List.of(new Spell() {
			final boolean mHasLegs = !(EntityUtils.isFlyingMob(mBoss) || EntityUtils.isWaterMob(mBoss));
			int mHalfSecondTimer = 0;
			int mAmbientTimer = 0;
			double mDistanceTraveled = 0;
			double mNextStepSoundDistance = mParams.STEP_SOUND_DISTANCE;
			Vector mPreviousPosition = mBoss.getLocation().toVector();

			@Override
			public void run() {
				mHalfSecondTimer++;
				mAmbientTimer++;

				if (mHalfSecondTimer % 10 == 0) {
					mBoss.removeScoreboardTag("HasDoneSoundThisHalfSecond");
				}

				if (mAmbientTimer >= mParams.AMBIENT_SOUND_TIMER) {
					mAmbientTimer = 0;
					mParams.AMBIENT_SOUND.play(mBoss.getEyeLocation());
				}

				playStepSoundsIfApplicable();
			}

			private void playStepSoundsIfApplicable() {
				// If the entity should not make any sounds, skip this section.
				if (!mHasLegs || mParams.STEP_SOUND.isEmpty()) {
					return;
				}

				// Get horizontal velocity and add its magnitude to distance traveled.
				// The vanilla code multiplies this number by 0.6 for some reason.
				final boolean isOnGround = mBoss.isOnGround();
				final boolean isClimbing = mBoss.isClimbing();
				final Vector newPosition = mBoss.getLocation().toVector();
				final Vector delta = newPosition.clone().subtract(mPreviousPosition);
				mPreviousPosition = newPosition;
				if (isOnGround || !isClimbing) {
					delta.setY(0);
				}
				mDistanceTraveled += delta.length() * DISTANCE_TRAVELED_FACTOR;

				// If the entity has traveled far enough and is on ground, make a sound and advance
				// nextStepSound. (Vanilla logic is a bit more complicated. Sound is only played if the
				// 'landing block' is not air.)
				if (mDistanceTraveled < mNextStepSoundDistance || (!isOnGround && !isClimbing)) {
					return;
				}
				mNextStepSoundDistance = mDistanceTraveled + mParams.STEP_SOUND_DISTANCE;
				mParams.STEP_SOUND.play(mBoss.getLocation(), 0.15F);
			}

			@Override
			public int cooldownTicks() {
				return 1;
			}
		});

		super.constructBoss(SpellManager.EMPTY, spellList, -1, null, 0, 1);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (event.getFinalDamage(false) <= 0 || event.getFinalDamage(true) >= mBoss.getHealth()) {
			//no sound when no damage or on death
			return;
		}
		//simple way to don't spam sound when the boss get hit multiple time
		if (mBoss.getScoreboardTags().contains("HasDoneSoundThisHalfSecond")) {
			return;
		}

		LivingEntity source = event.getSource();
		if (source instanceof Player) {
			//player damage
			mParams.PLAYER_HURT.play(mBoss.getLocation());
		} else if (source != null) {
			//mobs damage
			mParams.MOB_HURT.play(mBoss.getLocation());
		} else {
			//ambient damage
			mParams.AMBIENT_HURT.play(mBoss.getLocation());
		}
		mBoss.addScoreboardTag("HasDoneSoundThisHalfSecond");
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mParams.DEATH_SOUND.play(mBoss.getLocation());
	}
}
