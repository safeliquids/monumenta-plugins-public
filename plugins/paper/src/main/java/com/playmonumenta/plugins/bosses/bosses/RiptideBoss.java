package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRiptide;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

public class RiptideBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_riptide";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Name of the riptide damage spell")
		public String SPELL_NAME = "";
		@BossParam(help = "Time before the spell is first cast")
		public int DELAY = 20;
		@BossParam(help = "Range under which the boss will not riptide")
		public int MIN_RANGE = 0;
		@BossParam(help = "Boss detection range")
		public int DETECTION = 32;
		@BossParam(help = "Duration the boss telegraphs for")
		public int TELEGRAPH_DURATION = 20;
		@BossParam(help = "Maximum duration of the riptide")
		public int DURATION = 200;
		@BossParam(help = "Time between riptides")
		public int COOLDOWN = 20 * 8;
		@BossParam(help = "Velocity of the riptide")
		public double VELOCITY = 1.2;
		@BossParam(help = "size of hitbox when doing riptide damage")
		public double HITBOX_SIZE = 0.75;
		@BossParam(help = "Whether the damage bypass iframes")
		public boolean BYPASS_IFRAMES = false;
		@BossParam(help = "Whether the hit is blockable with a shield")
		public boolean BLOCKABLE = true;
		@BossParam(help = "Damage type dealt")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MELEE;
		@BossParam(help = "Damage dealt")
		public double DAMAGE = 20;
		@BossParam(help = "Percent health True damage dealt")
		public double DAMAGE_PERCENT = 0;
		@BossParam(help = "Effects applied to players hit by the pounce")
		public EffectsList EFFECTS = EffectsList.EMPTY;
		@BossParam(help = "Knockback applied in the direction of the riptide")
		public float KNOCKBACK = 0.5f;
		@BossParam(help = "Targets for the riptide to aim at")
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET.clone();
		@BossParam(help = "Whether to use the boss target instead of targets parameter")
		public boolean PREFER_TARGET = true;

		@BossParam(help = "Telegraph Sound")
		public SoundsList SOUND_TELEGRAPH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_RETURN, 1.0f, 0.5f))
			.add(new SoundsList.CSound(Sound.BLOCK_BELL_RESONATE, 1.5f, 2.0f))
			.build();
		@BossParam(help = "Jump Start Sound")
		public SoundsList SOUND_JUMP_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 0.9f))
			.build();
		@BossParam(help = "Sounds played when the launcher collides with a hit player or block")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_HIT, 1.3f, 0.6f))
			.build();
		@BossParam(help = "Jump Landing Sound")
		public SoundsList SOUND_LANDING = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_HORSE_GALLOP, 1.3f, 0.8f))
			.build();


		@BossParam(help = "Starting Particles")
		public ParticlesList PARTICLE_TELEGRAPH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 1))
			.build();
		@BossParam(help = "Starting Particles on the Ground")
		public ParticlesList PARTICLE_START = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 8, 0.4, 0.0, 0.4, 0.1))
			.build();
		@BossParam(help = "Air Particles")
		public ParticlesList PARTICLE_AIR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.DUST_PLUME, 3, 0.5, 0.5, 0.5, 0.2))
			.build();
		@BossParam(help = "Particles when hitting an entity")
		public ParticlesList PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 30, 0.0, 0.0, 0.0, 1.5))
			.add(new ParticlesList.CParticle(Particle.SWEEP_ATTACK, 5, 0.2, 0.4, 0.2, 0.5))
			.build();
		@BossParam(help = "Landing Particles on the ground")
		public ParticlesList PARTICLE_LAND_GROUND = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 1, 0.1, 0.1, 0.1, 0.1))
			.build();
	}

	public RiptideBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell jump = new SpellRiptide(plugin, boss,
			p.COOLDOWN, p.MIN_RANGE, p.TARGETS, p.PREFER_TARGET, p.VELOCITY, p.HITBOX_SIZE, p.TELEGRAPH_DURATION, p.DURATION,
			p.SOUND_TELEGRAPH, p.SOUND_JUMP_START, p.SOUND_HIT, p.SOUND_LANDING,
			p.PARTICLE_TELEGRAPH, p.PARTICLE_START, p.PARTICLE_AIR, p.PARTICLE_HIT, p.PARTICLE_LAND_GROUND,
			(damageLocation, player) -> {
				if (p.DAMAGE > 0.0) {
					if (p.BLOCKABLE) {
						BossUtils.blockableDamage(mBoss, player, p.DAMAGE_TYPE, p.DAMAGE, p.BYPASS_IFRAMES, false, p.SPELL_NAME, damageLocation);
					} else {
						DamageUtils.damage(boss, player, p.DAMAGE_TYPE, p.DAMAGE, null, p.BYPASS_IFRAMES, false, p.SPELL_NAME);
					}
				}
				if (p.DAMAGE_PERCENT > 0.0) {
					BossUtils.bossDamagePercent(boss, player, p.DAMAGE_PERCENT, p.SPELL_NAME);
				}
				p.EFFECTS.apply(player, boss);
				MovementUtils.knockAway(damageLocation, player, p.KNOCKBACK);
			});
		super.constructBoss(jump, p.DETECTION, null, p.DELAY);
	}


}
