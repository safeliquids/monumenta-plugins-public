package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.google.common.collect.RangeSet;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
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

import static com.playmonumenta.plugins.cosmetics.skills.warrior.BruteForceCS.getBlockData;

public class PrestigiousShieldCS extends ShieldWallCS implements PrestigeCS {

	public static final String NAME = "Prestigious Wall";

	private static final Particle.DustOptions GOLD_DUST = new Particle.DustOptions(Color.fromRGB(192, 168, 32), 1.5f);
	private static final Particle.DustOptions LIGHT_DUST = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.25f);

	@Override
	protected Color startColor() {
		return Color.fromRGB(255, 247, 207);
	}

	@Override
	protected Color endColor() {
		return Color.fromRGB(192, 168, 32);
	}

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"A radiant crescent glows",
			"upon the hero's shield."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_CHESTPLATE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void shieldStartEffect(World world, Player player, Location loc, double radius, double angle, double height) {
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1f, 1.35f);
		world.playSound(loc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.9f, 0.75f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 1.25f, 0.55f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 0.4f);

		Location mCenter = loc.clone().add(0, 0.125, 0);
		PPCircle ppc = new PPCircle(Particle.REDSTONE, mCenter, 0).data(LIGHT_DUST);
		int rings = (int) Math.ceil(radius * 1.25);
		for (int i = 1; i <= rings; i++) {
			ppc.count(i * 15).radius(radius * i / rings).spawnAsPlayerActive(player);
		}

		// Draw 土
		int units1 = (int) Math.ceil(radius * 2.4);
		int units2 = (int) Math.ceil(radius * 3.2);
		Vector mFront = loc.getDirection().clone().setY(0).normalize().multiply(radius);
		ParticleUtils.drawCurve(mCenter, -units1, units1, mFront,
			t -> 0.125,
			t -> 0, t -> 0.625 * t / units1,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_DUST).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, -units2, units2, mFront,
			t -> -0.8,
			t -> 0, t -> 0.75 * t / units1,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_DUST).spawnAsPlayerActive(player)
		);
		ParticleUtils.drawCurve(mCenter, -units2, units2, mFront,
			t -> 0.8 * t / units2,
			t -> 0, t -> 0,
			(l, t) -> new PartialParticle(Particle.REDSTONE, l, 2, 0.2, 0, 0.2, 0, GOLD_DUST).spawnAsPlayerActive(player)
		);

		mLastTickLoc = loc;
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 40, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
		new PPCircle(Particle.BLOCK_CRACK, loc, 0.4)
			.data(getBlockData(loc))
			.count(30)
			.spawnAsPlayerActive(player);
		shieldOnHit(loc, subdivide(loc, radius, angle, height), null, radius, 1);
	}

	@Override
	public void shieldOnBlockSFX(Location projLoc) {
		World world = projLoc.getWorld();
		world.playSound(projLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.25f);
		world.playSound(projLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.85f, 0.75f);
	}

	@Override
	public void shieldOnHitSFX(Location center, @Nullable LivingEntity enemy, float multiplier) {
		World world = center.getWorld();
		if (enemy != null) {
			world.playSound(enemy.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.8f * multiplier, 1.4f);
			world.playSound(enemy.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.9f * multiplier, 1.6f);
			world.playSound(enemy.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 0.95f * multiplier, 1.75f);
		}
	}

	@Override
	void flashWall(Player player, Location center, RangeSet<Float> flashRange, double radius, double angle, double height) {
		float centerYaw = center.getYaw();

		for (double yaw = centerYaw - angle / 2; yaw <= centerYaw + angle / 2; yaw += 9) {
			if (!flashRange.contains((float) FastUtils.wrapMod(yaw, 360))) {
				continue;
			}
			Vector vec = VectorUtils.rotateYAxis(new Vector(0, 0, 1), yaw);
			Location loc = center.clone().add(vec.clone().multiply(radius)).add(0, -1, 0);
			double ratio = Math.abs(centerYaw - yaw) / angle * 2;

			new PPParametric(Particle.ELECTRIC_SPARK, loc, (progress, p) -> {
				boolean isSpark = progress * progress + ratio * ratio <= 1
					&& Math.pow(progress + 0.75, 2) + ratio * ratio >= 1.25 * 1.25;
				p.particle(isSpark ? Particle.ELECTRIC_SPARK : Particle.CRIT);
				p.location(loc.clone().add(0, (height + 1) * progress, 0));
				if (!isSpark) {
					p.offset(p.offsetX(), 1, p.offsetZ());
				}
			})
				.count(10)
				.minimumCount(4)
				.delta(vec.getX(), 0.4, vec.getZ())
				.directionalMode(true)
				.extra(0.4)
				.delay(4)
				.spawnAsPlayerActive(player);
		}
	}
}
