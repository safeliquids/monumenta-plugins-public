package com.playmonumenta.plugins.cosmetics.skills.warlock.reaper;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DarkPactCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DARK_PACT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_SAND;
	}

	public void onCast(Player player, World world, Location loc) {
		new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0.2, 0.1, 0.2, 1).spawnAsPlayerActive(player);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.PLAYERS, 1.3f, 1.0f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.7f, 0.2f);
		world.playSound(loc, Sound.ENTITY_STRAY_HURT, SoundCategory.PLAYERS, 0.8f, 0.1f);
	}

	public void tick(Player player, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		new PartialParticle(Particle.SPELL_WITCH, player.getLocation(), 3, 0.2, 0.2, 0.2, 0.2).spawnAsPlayerActive(player);
	}

	public void onKill(Player player, LivingEntity mob) {

	}

	public void loseEffect(Player player) {
		AbilityUtils.playPassiveAbilitySound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.35f, 0.75f);
	}

	public void deactivationDamageApplied(Player player, World world, Location loc, double radius) {
		world.playSound(loc, Sound.ENTITY_WITHER_BREAK_BLOCK, 0.4f, 1.5f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.4f, 0.5f);
		world.playSound(loc, Sound.ENTITY_PHANTOM_DEATH, SoundCategory.PLAYERS, 0.4f, 0.75f);
		new PPCircle(Particle.DAMAGE_INDICATOR, loc.clone().add(0, 0.2, 0), radius)
			.ringMode(false)
			.countPerMeter(10)
			.spawnAsPlayerActive(player);
	}

	public void deactivationDamageAppliedPerMob(Player player, LivingEntity mob) {
		Particle.DustTransition options = new Particle.DustTransition(
			Color.fromRGB(0xF0489E), Color.fromRGB(0x42142B), 1
		);
		new PartialParticle(Particle.DUST_COLOR_TRANSITION, mob.getEyeLocation().add(0, 0.25, 0), 12, 0.4, 0.5, 0.4, options).spawnAsPlayerActive(player);
	}
}
