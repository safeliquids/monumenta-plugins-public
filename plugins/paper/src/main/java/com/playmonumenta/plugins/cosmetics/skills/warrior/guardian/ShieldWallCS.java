package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.MathUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.cosmetics.skills.warrior.BruteForceCS.getBlockData;

public class ShieldWallCS implements CosmeticSkill {

	public static final double NOT_MOVING_COSMETIC_THRESHOLD_SQUARED = 0.2 * 0.2;

	@Nullable
	protected Location mLastTickLoc;
	protected RangeSet<Float> mWallFlashRange = TreeRangeSet.create();

	protected Color startColor() {
		return Color.fromRGB(230, 230, 255);
	}

	protected Color endColor() {
		return Color.fromRGB(176, 176, 183);
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.SHIELD_WALL;
	}

	@Override
	public Material getDisplayItem() {
		return Material.COBBLESTONE_WALL;
	}

	public void shieldStartEffect(World world, Player player, Location loc, double radius, double angle, double height) {
		world.playSound(loc, Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 0.7f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3f, 0.1f);
		world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 0.3f, 2.0f);
		world.playSound(loc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.4f, 0.1f);
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 0.8f, 0.1f);
		world.playSound(loc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.PLAYERS, 0.5f, 0.1f);
		mLastTickLoc = loc;
		new PartialParticle(Particle.FIREWORKS_SPARK, loc, 40, 0, 0, 0, 0.3f).spawnAsPlayerActive(player);
		new PPCircle(Particle.BLOCK_CRACK, loc, 0.4)
			.data(getBlockData(loc))
			.count(30)
			.spawnAsPlayerActive(player);
		shieldOnHit(loc, subdivide(loc, radius, angle, height), null, radius, 1);
	}

	public List<Pair<Float, Double>> wallParticles(Player player, Location center, double radius, double angle, double height, int ticks) {
		List<Pair<Float, Double>> arcHeights = subdivide(center, radius, angle, height);
		if (mLastTickLoc != null && mLastTickLoc.getWorld() != null && mLastTickLoc.getWorld().equals(center.getWorld()) && mLastTickLoc.distanceSquared(center) > NOT_MOVING_COSMETIC_THRESHOLD_SQUARED) {
			movingWallLine(player, center, arcHeights, radius, angle, height, ticks);
		} else {
			stationaryWallLine(player, center, arcHeights, radius, height);
		}
		mLastTickLoc = center;

		flashWall(player, center, mWallFlashRange, radius, angle, height);
		mWallFlashRange.clear();

		return arcHeights;
	}

	List<Pair<Float, Double>> subdivide(Location center, double radius, double angle, double height) {
		// 0 / 360 yaw points along the +z axis, increasing yaw rotates to the right / clockwise as seen from above.
		float midYaw = center.getYaw();
		if (midYaw < 0) {
			midYaw += 360;
		}
		float leftEndYaw = midYaw - (float) angle / 2;
		float rightEndYaw = midYaw + (float) angle / 2;
		if (angle >= 360) {
			leftEndYaw = 0;
			rightEndYaw = 360;
		} else {
			if (leftEndYaw < 0) {
				leftEndYaw += 360;
			} else if (rightEndYaw > 360) {
				rightEndYaw -= 360;
			}
		}

		ArrayList<Float> blockBoundaryYaws = new ArrayList<>((int) (4.5 * Math.PI * radius));
		// This implementation scans the whole 360 degree arc.
		// Scan in the z-direction first
		for (int z = (int) Math.ceil(center.getZ() - radius); z < center.getZ() + radius; z++) {
			float yawAngle = 90f + (float) Math.toDegrees(Math.asin((center.getZ() - z) / radius));
			if (isWithin(yawAngle, midYaw, angle)) {
				blockBoundaryYaws.add(yawAngle);
			}
			if (isWithin(360 - yawAngle, midYaw, angle)) {
				blockBoundaryYaws.add(360 - yawAngle);
			}
		}
		// Now the x-direction
		for (int x = (int) Math.ceil(center.getX() - radius); x < center.getX() + radius; x++) {
			float yawAngle = (float) Math.toDegrees(Math.asin((center.getX() - x) / radius));
			if (yawAngle >= 0) {
				if (isWithin(yawAngle, midYaw, angle)) {
					blockBoundaryYaws.add(yawAngle);
				}
			} else {
				if (isWithin(360 + yawAngle, midYaw, angle)) {
					blockBoundaryYaws.add(360 + yawAngle);
				}
			}
			if (isWithin(180 - yawAngle, midYaw, angle)) {
				blockBoundaryYaws.add(180 - yawAngle);
			}
		}

		blockBoundaryYaws.add(leftEndYaw);
		blockBoundaryYaws.add(rightEndYaw);

		for (int i = 0; i < blockBoundaryYaws.size(); i++) {
			if (blockBoundaryYaws.get(i) < leftEndYaw) {
				blockBoundaryYaws.set(i, 360 + blockBoundaryYaws.get(i));
			}
		}

		blockBoundaryYaws.sort(null);

		// This ArrayList of Pairs encodes the yaw of the left side of the arc as its Key and the height of the arc to draw as its Value.
		ArrayList<Pair<Float, Double>> arcHeights = new ArrayList<>();
		// Initialise
		Location firstRadialPoint = center.clone().add(VectorUtils.rotateYAxis(new Vector(0, 0.01, radius), leftEndYaw));
		double firstGroundY = firstRadialPoint.getBlock().isSolid() ?
			LocationUtils.emergeFromGround(firstRadialPoint, firstRadialPoint.getY() + height).getY() :
			LocationUtils.fallToGround(firstRadialPoint, firstRadialPoint.getY() - 3).getY();

		arcHeights.add(Pair.of(blockBoundaryYaws.getFirst(), firstGroundY));

		double lastGroundY = firstGroundY;
		for (int i = 1; i < blockBoundaryYaws.size() - 1; i++) { // Exclude leftEndYaw and rightEndYaw
			float yawMidpoint = (blockBoundaryYaws.get(i) + blockBoundaryYaws.get(i + 1)) / 2;
			Location radialPoint = center.clone().add(VectorUtils.rotateYAxis(new Vector(0, 0.01, radius), yawMidpoint));

			double groundY = radialPoint.getBlock().isSolid() ?
				LocationUtils.emergeFromGround(radialPoint, radialPoint.getY() + height).getY() :
				LocationUtils.fallToGround(radialPoint, radialPoint.getY() - 3).getY();

			// Don't add duplicate groundY in a row
			if (lastGroundY != groundY) {
				lastGroundY = groundY;
				arcHeights.add(Pair.of(blockBoundaryYaws.get(i), groundY));
			}
		}
		arcHeights.add(Pair.of(blockBoundaryYaws.getLast(), lastGroundY));

		return arcHeights;
	}

	public void stationaryWallLine(Player player, Location center, List<Pair<Float, Double>> arcHeights, double radius, double height) {
		// Indicate bottom arc
		for (int i = 0; i < arcHeights.size() - 1; i++) {
			Location circleCenter = center.clone();
			circleCenter.setY(arcHeights.get(i).second());
			new PPCircle(Particle.WHITE_SMOKE, circleCenter, radius)
				.axes(new Vector(0, 0, 1), new Vector(-1, 0, 0))
				.arcDegree(arcHeights.get(i).first(), arcHeights.get(i + 1).first())
				.countPerMeter(1.5)
				.delta(0, 0.006 * height, 0)
				.deltaVariance(true)
				.directionalMode(true)
				.extra(1)
				.offset(FastUtils.RANDOM.nextDouble())
				.includeEnd(false)
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.DUST_COLOR_TRANSITION, circleCenter.add(0, 0.1, 0), radius)
				.axes(new Vector(0, 0, 1), new Vector(-1, 0, 0))
				.arcDegree(arcHeights.get(i).first(), arcHeights.get(i + 1).first())
				.data(new Particle.DustTransition(startColor(), endColor(), 1.6f))
				.delta(0.3, 0, 0) // I don't know if this is necessary, but I'm too scared to touch it now
				.deltaVariance(true)
				.rotateDelta(true)
				.innerRadiusFactor(0.85)
				.countPerMeter(0.9)
				.offset(FastUtils.RANDOM.nextDouble())
				.includeEnd(false)
				.spawnAsPlayerActive(player);
		}
		// Sides
		if (arcHeights.getLast().first() - arcHeights.getFirst().first() < 355) {
			Location lowerLeft = center.clone().add(
				VectorUtils.rotateYAxis(new Vector(0, 0, 0.925 * radius), arcHeights.getFirst().first()));
			Location upperLeft = lowerLeft.clone().add(0, height, 0);
			lowerLeft.setY(arcHeights.getFirst().second());
			new PPLine(Particle.DUST_COLOR_TRANSITION, lowerLeft, upperLeft)
				.data(new Particle.DustTransition(startColor(), endColor(), 1.4f))
				.countPerMeter(0.8)
				.delta(0.1, 0.2, 0.1)
				.deltaVariance(true)
				.offset(FastUtils.RANDOM.nextDouble())
				.includeEnd(false)
				.spawnAsPlayerActive(player);

			Location lowerRight = center.clone().add(
				VectorUtils.rotateYAxis(new Vector(0, 0, 0.925 * radius), arcHeights.getLast().first()));
			Location upperRight = lowerRight.clone().add(0, height, 0);
			lowerRight.setY(arcHeights.getLast().second());
			new PPLine(Particle.DUST_COLOR_TRANSITION, lowerRight, upperRight)
				.data(new Particle.DustTransition(startColor(), endColor(), 1.4f))
				.countPerMeter(0.8)
				.delta(0.1, 0.2, 0.1)
				.deltaVariance(true)
				.offset(FastUtils.RANDOM.nextDouble())
				.includeEnd(false)
				.spawnAsPlayerActive(player);
		}

		// Top
		Location circleCenter = center.clone();
		circleCenter.add(0, height, 0);
		new PPCircle(Particle.DUST_COLOR_TRANSITION, circleCenter, radius)
			.axes(new Vector(0, 0, 1), new Vector(-1, 0, 0))
			.arcDegree(arcHeights.getFirst().first(), arcHeights.getLast().first())
			.data(new Particle.DustTransition(startColor(), endColor(), 1.6f))
			.delta(0.3, 0, 0)
			.deltaVariance(true)
			.rotateDelta(true)
			.innerRadiusFactor(0.85)
			.countPerMeter(0.8)
			.offset(FastUtils.RANDOM.nextDouble())
			.includeEnd(false)
			.spawnAsPlayerActive(player);
	}

	public void movingWallLine(Player player, Location center, List<Pair<Float, Double>> arcHeights, double radius, double angle, double height, int ticks) {
		float centerYaw = center.getYaw();
		float leftYaw = arcHeights.getFirst().first();
		float rightYaw = arcHeights.getLast().first();
		boolean bypassCheck = rightYaw - leftYaw >= 359.7;

		for (int sizeIndex = 0; sizeIndex < 3; sizeIndex++) {
			float baseAngle = ((27 + ticks - 3 * sizeIndex) * 4) % 40; // 27 is an arbitrary number larger than 4
			for (int symmetricIndex = 0; symmetricIndex < 9; symmetricIndex++) {
				float yaw = baseAngle + 40 * symmetricIndex;
				if (bypassCheck || isWithin(yaw, centerYaw, angle)) {
					int index = 0;
					for (int heightScanIndex = 0; heightScanIndex < arcHeights.size() - 1; heightScanIndex++) {
						float arcLeftYaw = arcHeights.get(heightScanIndex).first();
						float arcRightYaw = arcHeights.get(heightScanIndex + 1).first();
						if (isWithin(yaw, (arcLeftYaw + arcRightYaw) / 2, arcRightYaw - arcLeftYaw)) {
							index = heightScanIndex;
							break;
						}
					}
					Location radialLoc = center.clone().add(VectorUtils.rotateYAxis(new Vector(0, 0, radius), yaw));
					radialLoc.setY(arcHeights.get(index).second() + 0.1);
					Block blockUnderneath = radialLoc.clone().subtract(0, 0.5, 0).getBlock();
					Color endColor = blockUnderneath.isEmpty() ? endColor() : blockUnderneath.getBlockData().getMapColor();
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, radialLoc)
						.data(new Particle.DustTransition(
							startColor(),
							endColor,
							1.4f - 0.3f * sizeIndex))
						.count(1)
						.minimumCount(sizeIndex == 0 ? 1 : 0)
						.spawnAsPlayerActive(player);
				}
			}
		}
		if (ticks % 13 == 0) {
			// Sides
			if (arcHeights.getLast().first() - arcHeights.getFirst().first() < 355) {
				Location lowerLeft = center.clone().add(
					VectorUtils.rotateYAxis(new Vector(0, 0, 0.925 * radius), arcHeights.getFirst().first()));
				Location upperLeft = lowerLeft.clone().add(0, height, 0);
				lowerLeft.setY(arcHeights.getFirst().second());
				new PPLine(Particle.DUST_COLOR_TRANSITION, lowerLeft, upperLeft)
					.data(new Particle.DustTransition(startColor(), endColor(), 1.4f))
					.countPerMeter(2)
					.delta(0.1, 0.2, 0.1)
					.deltaVariance(true)
					.offset(FastUtils.RANDOM.nextDouble())
					.includeEnd(false)
					.spawnAsPlayerActive(player);

				Location lowerRight = center.clone().add(
					VectorUtils.rotateYAxis(new Vector(0, 0, 0.925 * radius), arcHeights.getLast().first()));
				Location upperRight = lowerRight.clone().add(0, height, 0);
				lowerRight.setY(arcHeights.getLast().second());
				new PPLine(Particle.DUST_COLOR_TRANSITION, lowerRight, upperRight)
					.data(new Particle.DustTransition(startColor(), endColor(), 1.4f))
					.countPerMeter(2)
					.delta(0.1, 0.2, 0.1)
					.deltaVariance(true)
					.offset(FastUtils.RANDOM.nextDouble())
					.includeEnd(false)
					.spawnAsPlayerActive(player);
			}

			// Top
			Location circleCenter = center.clone();
			circleCenter.add(0, height, 0);
			new PPCircle(Particle.DUST_COLOR_TRANSITION, circleCenter, radius)
				.axes(new Vector(0, 0, 1), new Vector(-1, 0, 0))
				.arcDegree(arcHeights.getFirst().first(), arcHeights.getLast().first())
				.data(new Particle.DustTransition(startColor(), endColor(), 1.4f))
				.delta(0.3, 0, 0)
				.deltaVariance(true)
				.rotateDelta(true)
				.innerRadiusFactor(0.85)
				.countPerMeter(2.4)
				.offset(FastUtils.RANDOM.nextDouble())
				.includeEnd(false)
				.spawnAsPlayerActive(player);
		}
	}

	private static boolean isWithin(float yaw, float midYaw, double angle) {
		return Math.min(FastUtils.wrapMod(midYaw - yaw, 360), FastUtils.wrapMod(yaw - midYaw, 360)) <= angle / 2;
	}

	public void shieldOnBlock(Player player, Location centerLoc, Location projLoc, double radius) {
		Vector radial = LocationUtils.getVectorTo(projLoc, centerLoc).setY(0).normalize();
		Vector tangential = radial.getCrossProduct(new Vector(0, 1, 0));
		Vector tiltedVertical = new Vector(0, 1, 0).add(radial.clone().multiply(-0.05)).normalize();
		Vector delta = tiltedVertical.clone().multiply(2).add(tiltedVertical.getCrossProduct(tangential).multiply(0.7));

		Location circleLoc = centerLoc.add(radial.clone().multiply(radius));
		circleLoc.setY(projLoc.getY()); // projLoc is inaccurate for high projectile speeds
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= 3) {
					this.cancel();
				}

				new PPCircle(Particle.CRIT_MAGIC, circleLoc, 0.1)
					.axes(tiltedVertical, tangential)
					.delta(delta)
					.extra(1)
					.directionalMode(true)
					.rotateDelta(true)
					.count(25 - 4 * mTicks)
					.spawnAsPlayerActive(player);

				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		shieldOnBlockSFX(projLoc);
	}

	public void shieldOnBlockSFX(Location projLoc) {
		World world = projLoc.getWorld();
		world.playSound(projLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 0.75f, 1.5f);
		world.playSound(projLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.PLAYERS, 0.85f, 1.5f);
	}

	public void shieldOnHit(Location center, List<Pair<Float, Double>> arcHeights, @Nullable LivingEntity enemy, double radius, float multiplier) {
		// Calculation
		float leftEndYaw = arcHeights.getFirst().first();
		float rightEndYaw = arcHeights.getLast().first();

		RangeSet<Float> shieldWallRange = TreeRangeSet.create();
		if (rightEndYaw >= 360) {
			rightEndYaw -= 360;
			shieldWallRange.add(Range.closed(leftEndYaw, 360f));
			shieldWallRange.add(Range.closed(0f, rightEndYaw));
		} else {
			shieldWallRange.add(Range.closed(leftEndYaw, rightEndYaw));
		}


		if (enemy != null) { // Passing null enemy flashes the whole wall
			RangeSet<Float> mobRange = TreeRangeSet.create();

			Vector toMob = LocationUtils.getVectorTo(enemy.getLocation(), center).setY(0).normalize();
			float mobYaw = (float) Math.toDegrees(MathUtils.normalizeAngle(Math.atan2(-toMob.getX(), toMob.getZ()), Math.PI));
			float leftMobYaw = mobYaw - (float) Math.min(Math.toDegrees(enemy.getWidth() / (2 * radius)), 164) - 15;
			float rightMobYaw = mobYaw + (float) Math.min(Math.toDegrees(enemy.getWidth() / (2 * radius)), 164) + 15;

			boolean wrapAroundZero = false;
			if (leftMobYaw < 0) {
				wrapAroundZero = true;
				leftMobYaw += 360;
			} else if (rightMobYaw >= 360) {
				wrapAroundZero = true;
				rightMobYaw -= 360;
			}

			if (wrapAroundZero) {
				mobRange.add(Range.closed(leftMobYaw, 360f));
				mobRange.add(Range.closed(0f, rightMobYaw));
			} else {
				mobRange.add(Range.closed(leftMobYaw, rightMobYaw));
			}
			shieldWallRange.removeAll(mobRange.complement());
		}
		for (Range<Float> r : shieldWallRange.asRanges()) {
			mWallFlashRange.add(r);
		}

		// SFX
		shieldOnHitSFX(center, enemy, multiplier);
	}

	public void shieldOnHitSFX(Location center, @Nullable LivingEntity enemy, float multiplier) {
		World world = center.getWorld();
		if (enemy != null) {
			if (multiplier == 1) {
				world.playSound(enemy.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 2, 0.5f);
			}
			world.playSound(enemy.getLocation(), Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, multiplier * 0.7f, 1.5f);
			world.playSound(enemy.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, multiplier * 0.65f, 1.5f);
		}
	}

	void flashWall(Player player, Location center, RangeSet<Float> flashRange, double radius, double angle, double height) {
		float centerYaw = center.getYaw();

		for (double yaw = centerYaw - angle / 2; yaw <= centerYaw + angle / 2; yaw += 9) {
			if (!flashRange.contains((float) FastUtils.wrapMod(yaw, 360))) {
				continue;
			}
			Vector radialDir = VectorUtils.rotateYAxis(new Vector(0, 0, 1), yaw);
			Location radialLoc = center.clone().add(radialDir.clone().multiply(radius));
			new PPLine(Particle.ELECTRIC_SPARK, radialLoc.clone().add(0, -1, 0), radialLoc.clone().add(0, height, 0))
				.count(10)
				.minimumCount(4)
				.delay(4)
				.delta(radialDir.getX(), 0.4, radialDir.getZ())
				.directionalMode(true)
				.includeEnd(false)
				.extra(0.4)
				.spawnAsPlayerActive(player);
		}
	}
}
