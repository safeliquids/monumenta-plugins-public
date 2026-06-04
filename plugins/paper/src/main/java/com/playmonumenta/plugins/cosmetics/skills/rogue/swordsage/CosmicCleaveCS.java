package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
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

public class CosmicCleaveCS extends DeadlyRondeCS {
	private int mMode = 0;

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Three slashes.",
			"Tear the skies asunder."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.AMETHYST_CLUSTER;
	}

	@Override
	public @Nullable String getName() {
		return "Cosmic Cleave";
	}

	@Override
	public void rondeHitEffect(World world, Player player, Entity enemy, double radius, double angle, boolean lvl2) {
		Location loc = player.getLocation();
		Vector viewDirection = loc.getDirection();
		switch (mMode) {
			case 0 -> {
				drawArc(player, viewDirection, Math.PI / 6, radius, angle);
				world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.6f);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.6f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 1.8f);
				world.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.7f, 1.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 1.7f);
				mMode = 1;
			}
			case 1 -> {
				drawArc(player, viewDirection, -Math.PI / 6, radius, angle);
				world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.6f);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.6f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 1.8f);
				world.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.7f, 1.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.5f, 1.7f);
				mMode = 2;
			}
			default -> {
				drawArc(player, viewDirection, Math.PI / 6, radius, angle);
				drawArc(player, viewDirection, -Math.PI / 6, radius, angle);
				world.playSound(loc, Sound.ENTITY_DROWNED_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.3f);
				world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1f, 0.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 0.5f);
				world.playSound(loc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1f, 1.5f);
				world.playSound(loc, Sound.BLOCK_CONDUIT_ACTIVATE, SoundCategory.PLAYERS, 0.7f, 1.2f);
				world.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.15f, 1.2f);
				world.playSound(loc, Sound.ITEM_TRIDENT_RIPTIDE_2, SoundCategory.PLAYERS, 0.7f, 2.0f);
				mMode = 0;
			}
		}
	}

	@Override
	public void rondeTickEffect(Player player, int charges, int mTicks) {
		// This is intentionally empty :)
	}

	@Override
	public void rondeGainStackEffect(Player player, Location loc) {
		World world = player.getWorld();
		world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.PLAYERS, 0.8f, 2.0f);
		new PartialParticle(Particle.REDSTONE, loc.add(0, 1, 0), 50, 0.35, 0.5, 0.35, 0, new Particle.DustOptions(rollCosmicColor(), 0.75f)).spawnAsPlayerActive(player);
	}

	private void drawArc(Player player, Vector viewDirection, double tilt, double radius, double angle) {
		Location eyeLocation = player.getEyeLocation();
		Vector[] axes = VectorUtils.getAxesFromNormal(viewDirection);
		Vector viewNormalUp = axes[1];
		Vector viewNormal = axes[0].multiply(-1);

		double signum = Math.signum(tilt);
		double radians = Math.toRadians(angle);
		new BukkitRunnable() {
			double mAngle = -radians;

			@Override
			public void run() {
				for (int i = 0; i < 6; i++) {
					if (mAngle > radians) {
						this.cancel();
						return;
					}
					Vector offsetX = viewDirection.clone().multiply(FastUtils.cos(signum * mAngle));
					Vector offsetZ = viewNormal.clone().multiply(FastUtils.sin(signum * mAngle));
					Vector offset = offsetX.add(offsetZ).rotateAroundAxis(viewDirection, tilt);
					Vector delta = viewNormalUp.clone().rotateAroundAxis(viewDirection, tilt).crossProduct(offset).multiply(-signum);
					Location critLoc = eyeLocation.clone().add(offset.clone().multiply(2.3));
					Location dustLoc = eyeLocation.clone().add(offset.clone().multiply(radius));

					if (i % 2 == 0) {
						new PPLine(Particle.CRIT_MAGIC, critLoc, dustLoc)
							.countPerMeter(1)
							.directionalMode(true)
							.delta(delta.getX(), delta.getY(), delta.getZ())
							.extra(1)
							.spawnAsPlayerActive(player);
						new PPLine(Particle.ELECTRIC_SPARK, critLoc, dustLoc, 0.06)
							.countPerMeter(0.5)
							.directionalMode(true)
							.delta(delta.getX(), delta.getY(), delta.getZ())
							.extra(1)
							.spawnAsPlayerActive(player);
					}
					new PPLine(Particle.REDSTONE, critLoc, dustLoc, 0.06)
						.countPerMeter(1)
						.data(new Particle.DustOptions(rollCosmicColor(), 0.9f))
						.spawnAsPlayerActive(player);
					mAngle += Math.PI / 45;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private Color rollCosmicColor() {
		return Color.fromRGB(80 + FastUtils.randomIntInRange(0, 160), 80, 200);
	}
}
