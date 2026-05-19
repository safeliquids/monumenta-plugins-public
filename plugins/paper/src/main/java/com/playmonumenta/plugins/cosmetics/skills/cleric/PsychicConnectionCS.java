package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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

public class PsychicConnectionCS extends TouchofRadianceCS {
	public static final String NAME = "Psychic Connection";

	@Override
	public Material getDisplayItem() {
		return Material.TWISTING_VINES;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Evoke confusion in your enemies and courage",
			"in your allies, as even a tiny thought planted",
			"in the mind can grow to take full control."
		);
	}

	@Override
	public NamedTextColor glowColor() {
		return NamedTextColor.DARK_AQUA;
	}

	@Override
	public void tickEffect(LivingEntity entity) {
		PartialParticle crit = new PartialParticle(Particle.CRIT_MAGIC, LocationUtils.getEntityCenter(entity), 5).delta(0.7);
		PartialParticle glow = new PartialParticle(Particle.GLOW, LocationUtils.getEntityCenter(entity), 3).delta(1);
		if (entity instanceof Player player) {
			crit.spawnAsPlayerBuff(player);
			glow.spawnAsPlayerBuff(player);
		} else {
			crit.spawnAsEnemyBuff();
			glow.spawnAsEnemyBuff();
		}
	}

	@Override
	public void loseEffect(LivingEntity entity) {
		World world = entity.getWorld();
		Location loc = entity.getLocation();
		world.playSound(loc, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 0.6f, 1.0f);
		world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.1f, 0.9f);
	}

	@Override
	public void castOnAlly(Player player, LivingEntity target) {
		Location startLoc = player.getEyeLocation();
		Location endLoc = LocationUtils.getHalfHeightLocation(target);
		Vector dir = LocationUtils.getDirectionTo(endLoc, startLoc);
		startLoc.setDirection(dir);

		Location currLoc = startLoc.clone().add(dir);
		double distance = currLoc.distance(endLoc) - 1;
		for (int i = 0; i * 2 < distance; i++) {
			for (int j = 0; j < 5; j++) {
				int progress = 5 * i + j;
				if (0.4 * progress > distance) {
					break;
				}
				boolean flip = dir.getY() > 0;
				currLoc.add(dir.clone().multiply(0.4));
				Vector right = VectorUtils.rotateTargetDirection(dir, -90, -90);
				Vector left = VectorUtils.rotateTargetDirection(dir, -90, 90);
				Vector swirl1 = VectorUtils.rotateTargetDirection(right, (12 * progress) % 360, 0).multiply(0.9);
				Vector swirl2 = VectorUtils.rotateTargetDirection(left, (12 * progress) % 360, 0).multiply(0.9);
				Location swirlLoc1 = currLoc.clone().add(swirl1);
				Location swirlLoc2 = currLoc.clone().add(swirl2);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					// 4 particles per swirl per tick
					new PartialParticle(Particle.SCULK_CHARGE, swirlLoc1, 2).data((float) Math.toRadians((12 * progress) % 360) * (flip ? -1 : 1)).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SCULK_CHARGE, swirlLoc2, 2).data((float) (Math.toRadians((12 * progress) % 360) * (flip ? -1 : 1) + Math.PI)).spawnAsPlayerActive(player);
				}, i);
			}
		}

		Location pLoc = startLoc.clone();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();
		new PPCircle(Particle.SCRAPE, endLoc, 0.75).countPerMeter(5).extra(16)
			.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
			.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);
		new PPCircle(Particle.CRIT_MAGIC, endLoc, 0.75).countPerMeter(5).extra(0.4)
			.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
			.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);

		World world = player.getWorld();
		Location soundLoc = player.getLocation();
		world.playSound(soundLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.9f);
		world.playSound(soundLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.6f, 1.2f);
		world.playSound(soundLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.4f, 1f);
	}

	@Override
	public void castOnHeretic(Player player, LivingEntity target) {
		Location startLoc = player.getEyeLocation();
		Location endLoc = LocationUtils.getHalfHeightLocation(target);
		Vector dir = LocationUtils.getDirectionTo(endLoc, startLoc);
		startLoc.setDirection(dir);

		Location currLoc = startLoc.clone().add(dir);
		double distance = currLoc.distance(endLoc) - 1;
		for (int i = 0; i * 2 < distance; i++) {
			for (int j = 0; j < 5; j++) {
				int progress = 5 * i + j;
				if (0.4 * progress > distance) {
					break;
				}
				boolean flip = dir.getY() > 0;
				currLoc.add(dir.clone().multiply(0.4));
				Vector right = VectorUtils.rotateTargetDirection(dir, -90, -90);
				Vector left = VectorUtils.rotateTargetDirection(dir, -90, 90);
				Vector swirl1 = VectorUtils.rotateTargetDirection(right, (12 * progress) % 360, 0).multiply(0.9);
				Vector swirl2 = VectorUtils.rotateTargetDirection(left, (12 * progress) % 360, 0).multiply(0.9);
				Location swirlLoc1 = currLoc.clone().add(swirl1);
				Location swirlLoc2 = currLoc.clone().add(swirl2);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					// 4 particles per swirl per tick
					new PartialParticle(Particle.SCULK_CHARGE, swirlLoc1, 2).data((float) Math.toRadians((12 * progress) % 360) * (flip ? -1 : 1)).spawnAsPlayerActive(player);
					new PartialParticle(Particle.SCULK_CHARGE, swirlLoc2, 2).data((float) (Math.toRadians((12 * progress) % 360) * (flip ? -1 : 1) + Math.PI)).spawnAsPlayerActive(player);
				}, i);
			}
		}

		Location pLoc = startLoc.clone();
		pLoc.setPitch(pLoc.getPitch() + 90);
		Vector pVec = new Vector(pLoc.getDirection().getX(), pLoc.getDirection().getY(), pLoc.getDirection().getZ());
		pVec = pVec.normalize();
		new PPCircle(Particle.SCRAPE, endLoc, 0.75).countPerMeter(5).extra(16)
			.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
			.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);
		new PPCircle(Particle.CRIT_MAGIC, endLoc, 0.75).countPerMeter(5).extra(0.4)
			.delta(pVec.getX(), pVec.getY(), pVec.getZ()).directionalMode(true).rotateDelta(true)
			.axes(pVec, pVec.clone().crossProduct(startLoc.getDirection())).ringMode(true).spawnAsPlayerActive(player);

		new PartialParticle(Particle.EXPLOSION_LARGE, endLoc).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLASH, endLoc).spawnAsPlayerActive(player);

		World world = player.getWorld();
		Location soundLoc = player.getLocation();
		world.playSound(soundLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.8f);
		world.playSound(soundLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.PLAYERS, 0.6f, 1f);
		world.playSound(soundLoc, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 1.4f, 0.8f);
	}

	@Override
	public void applyWeakness(Player player, LivingEntity target, double radius) {
		new PPCircle(Particle.CRIT_MAGIC, target.getLocation().add(0, 0.25, 0), radius).countPerMeter(2.5).delta(0.5, 0.1, 0).extra(1).directionalMode(true).rotateDelta(true).spawnAsPlayerActive(player);
	}
}
