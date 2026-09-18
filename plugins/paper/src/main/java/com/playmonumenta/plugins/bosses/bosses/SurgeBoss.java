package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellSurge;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class SurgeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_surge";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Name of the spell")
		public String SPELL_NAME = "Energy Surge";

		@BossParam(help = "Range of the surge")
		public int RANGE = 10;

		@BossParam(help = "Radius in blocks that this boss will check before doing anything")
		public int DETECTION = 24;

		@BossParam(help = "Initial delay in ticks for the first cast of this spell")
		public int DELAY = 20 * 2;

		@BossParam(help = "Period in ticks between the start of the last charge and next start.")
		public int COOLDOWN = 20 * 12;

		@BossParam(help = "Damage type for the damage")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.BLAST;

		@BossParam(help = "Damage to apply to a hit entity")
		public int DAMAGE = 10;

		@BossParam(help = "Whether the explosion is blockable with a shield")
		public boolean BLOCKABLE = true;

		@BossParam(help = "The horizontal knockback applied by the explosion")
		public float KB_XZ = 0.5f;

		@BossParam(help = "The vertical knockback applied by the explosion")
		public float KB_Y = 0.3f;

		@BossParam(help = "The entities to be targeted by the projectiles")
		public EntityTargets TARGETS = EntityTargets.GENERIC_SELF_TARGET.clone();

		@BossParam(help = "Delay between projectiles (0 means instant)")
		public int PROJECTILE_INTERVAL = 1;

		@BossParam(help = "Should the boss center the surge on the target?")
		public boolean TRACKING = false;

		@BossParam(help = "Should the surge respect player's immunity frames?")
		public boolean RESPECT_IFRAMES = true;

		@BossParam(help = "LibraryOfSouls pool spawned when the explosion occurs")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "Percent true damage to apply to a hit entity")
		public double DAMAGE_PERCENTAGE = 0.0;

		@BossParam(help = "Number of projectiles")
		public int PROJECTILE_COUNT = 12;

		@BossParam(help = "Radius of the explosion")
		public double EXPLOSION_RADIUS = 2.5;

		@BossParam(help = "Duration of the particles in the air")
		public int DURATION = 30;

		@BossParam(help = "Telegraph circle particle")
		public ParticlesList PARTICLE_TELEGRAPH_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 5))
			.build();

		@BossParam(help = "Projectile particle")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.ELECTRIC_SPARK, 2, 0, 0, 0, 0.06))
			.build();

		@BossParam(help = "Telegraph circle particle")
		public ParticlesList PARTICLE_EXPLOSION = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.END_ROD, 40, 0, 0, 0, 0.08))
			.build();

		@BossParam(help = "Sound played when each projectile is thrown")
		public SoundsList SOUND_THROW = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_THROW, 1.2f, 1.0f))
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_THROW, 1.5f, 0.6f))
			.build();

		@BossParam(help = "Sound of projectile explosion")
		public SoundsList SOUND_EXPLOSION = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f))
			.add(new SoundsList.CSound(Sound.ENTITY_EVOKER_CAST_SPELL, 1.3f, 1.0f))
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 2f))
			.add(new SoundsList.CSound(Sound.ENTITY_GLOW_SQUID_SQUIRT, 1f, 2f))
			.add(new SoundsList.CSound(Sound.ENTITY_PUFFER_FISH_BLOW_UP, 0.8f, 1.2f))
			.build();
	}

	public SurgeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters parameters = BossParameters.getParameters(boss, identityTag, new Parameters());
		constructBoss(new SpellManager(List.of(new SpellSurge(plugin, boss, parameters))), List.of(), parameters.DETECTION, null, parameters.DELAY);
	}
}
