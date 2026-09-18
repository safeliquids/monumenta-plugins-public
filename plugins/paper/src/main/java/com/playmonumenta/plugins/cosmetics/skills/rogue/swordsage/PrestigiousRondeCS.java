package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousRondeCS extends DeadlyRondeCS implements PrestigeCS {

	public static final String NAME = "Prestigious Ronde";

	private static final Particle.DustOptions GOLD_COLOR = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 1.0f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private static final Particle.DustOptions GOLD_TINY = new Particle.DustOptions(Color.fromRGB(255, 224, 48), 0.65f);
	private static final Particle.DustOptions LIGHT_TINY = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 0.65f);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A flurry of blows follows",
			"the master's blade."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.MUSIC_DISC_13;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void rondeHitEffect(World world, Player player, Entity enemy, double radius, double angle, boolean lvl2) {
		Vector mFront = player.getEyeLocation().getDirection();
		Location particleLoc = player.getEyeLocation().add(mFront.multiply(0.3 * radius));

		slash(world, player, particleLoc, radius, FastUtils.randomDoubleInRange(-angle, angle), GOLD_COLOR, GOLD_TINY);
		new BukkitRunnable() {
			@Override
			public void run() {
				slash(world, player, particleLoc, radius, FastUtils.randomDoubleInRange(-angle, angle), LIGHT_COLOR, LIGHT_TINY);
			}
		}.runTaskLater(Plugin.getInstance(), 3);
		if (lvl2) {
			new BukkitRunnable() {
				@Override
				public void run() {
					slash(world, player, particleLoc, radius, FastUtils.randomDoubleInRange(-angle, angle), GOLD_COLOR, LIGHT_TINY);
				}
			}.runTaskLater(Plugin.getInstance(), 5);
		}
	}

	private void slash(World world, Player player, Location pLoc, double radius, double angle, Particle.DustOptions color1, Particle.DustOptions color2) {
		double r = 0.05 * radius * Math.min(FastUtils.RANDOM.nextDouble() * FastUtils.RANDOM.nextDouble(), 0.75);
		double theta = FastUtils.RANDOM.nextDouble() * 2 * 3.1416;
		double dF = 0.048 * radius + FastUtils.RANDOM.nextDouble() * 0.032 * radius;
		double dR = r * FastUtils.cos(theta);
		double dU = r * FastUtils.sin(theta);
		double dX = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		double dY = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		double dZ = 1.4 * radius * Math.pow(FastUtils.RANDOM.nextDouble() - 0.5, 3);
		Location center = pLoc.clone().add(dX, dY, dZ);
		Vector direction = center.getDirection();
		Vector axisAngle = VectorUtils.getAxesFromNormal(direction)[1];
		direction = direction.rotateAroundAxis(axisAngle, Math.toRadians(angle));

		new PPLine(Particle.REDSTONE, center, direction, radius * 0.7)
			.countPerMeter(3)
			.delta(dF, dR, dU)
			.data(color1)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.REDSTONE, center, direction, radius * 0.7)
			.countPerMeter(6)
			.delta(dF, dR, dU)
			.data(color2)
			.spawnAsPlayerActive(player);
		new PPLine(Particle.SWEEP_ATTACK, center, direction, radius * 0.7)
			.countPerMeter(0.25)
			.delta(dF, dR, dU)
			.spawnAsPlayerActive(player);

		world.playSound(pLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.65f, 1.6f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.1f, 0.7f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.95f, 0.6f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.85f, 0.8f);
		world.playSound(pLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 0.75f, 0.75f);
	}

	@Override
	public void rondeGainStackEffect(Player player, Location loc) {
		player.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.35f, 0.7f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 1.1f, 1.6f);
		player.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 4f, 0.8f);
		player.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 0.9f, 1.7f);
	}

	@Override
	public void rondeTickEffect(Player player, int charges, int mTicks) {
		for (int i = 0; i < charges; i++) {
			double angle = 2 * 3.1416 * i / charges + 0.28 * 3.1416 * mTicks / (charges + 1);
			new PartialParticle(Particle.REDSTONE, player.getLocation().add(FastUtils.cos(angle), 0, -FastUtils.sin(angle)),
				(i + mTicks) % 3, 0.05, 0.45, 0.05, i % 2 == 0 ? GOLD_COLOR : LIGHT_COLOR).spawnAsPlayerBuff(player);
		}
	}
}
