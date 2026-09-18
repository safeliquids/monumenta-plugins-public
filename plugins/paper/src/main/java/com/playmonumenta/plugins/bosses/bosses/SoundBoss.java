package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

		@BossParam(help = "emit step sounds based on surface material")
		public boolean STEP_ON_BLOCKS = false;

		@BossParam(help = "sounds played when the mob dies")
		public SoundsList DEATH_SOUND = SoundsList.EMPTY;
	}

	private final Parameters mParams;

	private final double DISTANCE_TRAVELED_FACTOR = 0.6;
	// minimum number of ticks between subsequent amethyst chimes
	private final int AMETHYST_CHIME_DELAY = 20;

	public SoundBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mBoss.setSilent(true);
		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		mParams.SPAWN_SOUND.play(mBoss.getLocation());

		List<Spell> spellList;
		spellList = List.of(new Spell() {
			final boolean mHasLegs = EntityUtils.isWaterMobWithFootsteps(mBoss) || (!EntityUtils.isFlyingMob(mBoss) && !EntityUtils.isWaterMob(mBoss));
			int mHalfSecondTimer = 0;
			int mAmbientTimer = 0;
			int mAmethystChimeTimer = 0;
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
				if (!mHasLegs || (mParams.STEP_SOUND.isEmpty() && !mParams.STEP_ON_BLOCKS)) {
					return;
				}

				if (mParams.STEP_ON_BLOCKS) {
					mAmethystChimeTimer++;
				}

				// Get change in position (only horizontal if on ground), and add its length to mDistanceTraveled.
				// The vanilla code multiplies this number by 0.6 for some reason.
				final boolean isOnGround = mBoss.isOnGround();
				final boolean isClimbing = mBoss.isClimbing();
				final Location entityLocation = mBoss.getLocation();
				final Vector newPosition = entityLocation.toVector();
				final Vector delta = newPosition.clone().subtract(mPreviousPosition);
				mPreviousPosition = newPosition;
				if (isOnGround || !isClimbing) {
					delta.setY(0);
				}
				mDistanceTraveled += delta.length() * DISTANCE_TRAVELED_FACTOR;

				// If the entity has traveled far enough and is on ground, make a sound and advance
				// nextStepSound.
				if (mDistanceTraveled < mNextStepSoundDistance || (!isOnGround && !isClimbing)) {
					return;
				}
				mNextStepSoundDistance = mDistanceTraveled + mParams.STEP_SOUND_DISTANCE;
				if (!mParams.STEP_SOUND.isEmpty()) {
					mParams.STEP_SOUND.play(entityLocation, 0.15F);
				}

				// block step sounds
				if (!mParams.STEP_ON_BLOCKS) {
					return;
				}
				final Block entityBlock = entityLocation.getBlock();
				Block blockForStepSound = entityBlock;
				Block belowEntityBlock;
				if (isOnGround
						&& entityLocation.getY() - Math.floor(entityLocation.getY()) <= 0.2D
						&& !(belowEntityBlock = entityBlock.getRelative(BlockFace.DOWN)).getType().isAir()) {
					blockForStepSound = belowEntityBlock;
				}
				if (blockForStepSound.getType().isAir()) {
					return;
				}
				Sound steppingSound = blockForStepSound.getBlockSoundGroup().getStepSound();
				entityLocation.getWorld().playSound(entityLocation, steppingSound, SoundCategory.HOSTILE, 0.15F, 1.0F);
				// amethyst chime (very important)
				if (mAmethystChimeTimer >= AMETHYST_CHIME_DELAY && Tag.CRYSTAL_SOUND_BLOCKS.isTagged(blockForStepSound.getType())) {
					// in Vanilla, the volume slowly increases from 0.5 to 1.3 as the entity takes more steps
					// on amethyst, but maybe we don't need to go that far. Taking 0.66 because it is a constant
					// somewhere in the middle.
					entityLocation.getWorld().playSound(entityLocation, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.66F, FastUtils.randomFloatInRange(0.5F, 1.7F));
					mAmethystChimeTimer = 0;
				}
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
