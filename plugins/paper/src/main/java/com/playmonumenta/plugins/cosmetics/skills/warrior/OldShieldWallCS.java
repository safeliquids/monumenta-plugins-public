package com.playmonumenta.plugins.cosmetics.skills.warrior;

import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class OldShieldWallCS extends ShieldWallCS {
	@Override
	public @Nullable String getName() {
		return "Old Shield Wall";
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The fate of all things is to ",
			"grow old and decay. But there ",
			"is nobility in defying one's destiny.",
			"",
			"Warning: This cosmetic can cause ",
			"clientside lag, and is epilepsy-unfriendly!",
			"Do not use where possible!"
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.MOSSY_COBBLESTONE_WALL;
	}

	@Override
	public void shieldStartEffect(World world, Player player, Location loc, double radius, double angle, double height) {
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.4f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.5f, 0.1f);
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 70, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
	}

	@Override
	public List<Pair<Float, Double>> wallParticles(Player player, Location center, double radius, double angle, double height, int ticks) {
		float midYaw = center.getYaw();
		for (double y = -0.5; y <= height; y++) {
			new PPCircle(Particle.ELECTRIC_SPARK, center.clone().add(0, y, 0), radius)
				.axes(new Vector(0, 0, 1), new Vector(-1, 0, 0))
				.arcDegree(midYaw - angle / 2, midYaw + angle / 2)
				.countPerMeter(1.8)
				.extra(1000000)
				.spawnAsPlayerActive(player);
		}
		return new ArrayList<>();
	}

	@Override
	public void shieldOnBlock(Player player, Location centerLoc, Location projLoc, double radius) {
		player.getWorld().playSound(projLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.5f);
		new PartialParticle(Particle.FIREWORKS_SPARK, projLoc, 5, 0, 0, 0, 0.25f).spawnAsPlayerActive(player);
	}

	@Override
	public void shieldOnHit(Player player, Location center, List<Pair<Float, Double>> arcHeights, @Nullable LivingEntity enemy, double radius, float multiplier) {
		if (enemy == null) {
			return;
		}
		center.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, multiplier, 1f);
		new PartialParticle(Particle.EXPLOSION_NORMAL, enemy.getLocation(), (int) (50 * multiplier), 0, 0, 0, 0.35f).spawnAsPlayerActive(player);
	}
}
