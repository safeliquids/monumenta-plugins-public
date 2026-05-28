package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EffectOnKillBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_effectonkill";

	public static class Parameters extends BossParameters {
		@BossParam(help = "A player must be within this range to load the tag.")
		public int DETECTION = 25;

		@BossParam(help = "The boss must kill the enemy within this range of itself for the effect to apply.")
		public double RADIUS = 12;

		@BossParam(help = "A player must be this close to the boss to receive effects.")
		public double PLAYER_RANGE = 16;

		@BossParam(help = "Effects applied to players hit.")
		public EffectsList PLAYER_EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "An enemy must be this close to the boss to receive effects.")
		public double MOB_RANGE = 16;

		@BossParam(help = "Effects applied to enemies hit.")
		public EffectsList ENEMY_EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Sounds played when the boss kills an entity.")
		public SoundsList SOUND_DEATH = SoundsList.EMPTY;

		@BossParam(help = "Particles summoned when the boss kills an entity.")
		public ParticlesList PARTICLE_DEATH = ParticlesList.EMPTY;

		@BossParam(help = "Sounds played when the boss applies effects to a player.")
		public SoundsList PLAYER_EFFECT_SOUNDS = SoundsList.EMPTY;

		@BossParam(help = "Particles summoned when the boss applies effects to a player.")
		public ParticlesList PLAYER_EFFECT_PARTICLES = ParticlesList.EMPTY;

		@BossParam(help = "Sounds played when the boss applies effects to an enemy.")
		public SoundsList ENEMY_EFFECT_SOUNDS = SoundsList.EMPTY;

		@BossParam(help = "Particles summoned when the boss applies effects to an enemy.")
		public ParticlesList ENEMY_EFFECT_PARTICLES = ParticlesList.EMPTY;
	}

	private final Parameters mParam;

	public EffectOnKillBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParam.DETECTION, null);
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public double nearbyEntityDeathMaxRange() {
		return mParam.RADIUS;
	}

	@Override
	public void nearbyEntityDeath(final EntityDeathEvent event) {
		if (event.isCancelled() || !mBoss.isValid() || mBoss.isDead()) {
			return;
		}

		// Only trigger when the player kills a mob within range
		final Location deadLoc = event.getEntity().getLocation();
		final Location bossLoc = mBoss.getLocation();
		final EntityDamageEvent lastEvent = event.getEntity().getLastDamageCause();
		if (lastEvent == null || lastEvent.getDamageSource().getCausingEntity() != mBoss || deadLoc.distanceSquared(bossLoc) > mParam.RADIUS * mParam.RADIUS) {
			return;
		}

		mParam.SOUND_DEATH.play(bossLoc, 0.1f, 0.8f);
		mParam.PARTICLE_DEATH.spawn(mBoss, LocationUtils.getEntityCenter(mBoss), 0.25, 0.45, 0.25, 1);

		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mParam.PLAYER_RANGE, true, false);
		for (Player player : players) {
			mParam.PLAYER_EFFECTS.apply(player, mBoss);
			mParam.PLAYER_EFFECT_SOUNDS.play(player, 1f, 1f);
			mParam.PLAYER_EFFECT_PARTICLES.spawn(player, LocationUtils.getEntityCenter(player));
		}

		List<LivingEntity> enemies = EntityUtils.getNearbyMobs(mBoss.getLocation(), mParam.MOB_RANGE);
		for (LivingEntity enemy : enemies) {
			mParam.ENEMY_EFFECTS.apply(enemy, mBoss);
			mParam.ENEMY_EFFECT_SOUNDS.play(enemy.getLocation(), 1f, 1f);
			mParam.ENEMY_EFFECT_PARTICLES.spawn(enemy, LocationUtils.getEntityCenter(enemy));
		}
	}
}
