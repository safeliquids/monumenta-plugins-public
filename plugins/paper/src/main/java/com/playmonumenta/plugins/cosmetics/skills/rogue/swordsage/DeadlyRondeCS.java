package com.playmonumenta.plugins.cosmetics.skills.rogue.swordsage;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
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

public class DeadlyRondeCS implements CosmeticSkill {
	private static final Particle.DustOptions SWORDSAGE_COLOR = new Particle.DustOptions(Color.fromRGB(0x6b6082), 0.92f);

	private int mSign = 1;

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.DEADLY_RONDE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.BLAZE_ROD;
	}

	public void rondeHitEffect(World world, Player player, Entity enemy, double radius, double angle, boolean lvl2) {
		double distance = enemy.getLocation().distance(player.getLocation());
		Location eyeLocation = player.getEyeLocation();
		Vector viewDirection = eyeLocation.getDirection();
		Vector[] axes = VectorUtils.getAxesFromNormal(viewDirection);
		Vector viewNormalUp = axes[1];
		Vector viewNormal = axes[0];
		Location soundLoc = eyeLocation.clone().add(viewDirection.clone().multiply(distance));
		double tilt = mSign * Math.PI / 6;
		mSign *= -1;

		double signum = Math.signum(tilt);
		double radians = Math.toRadians(angle);
		new BukkitRunnable() {
			double mAngle = -radians;

			@Override
			public void run() {
				for (int i = 0; i < 8; i++) {
					if (mAngle > radians) {
						this.cancel();
						return;
					}
					Vector offsetX = viewDirection.clone().multiply(FastUtils.cos(signum * mAngle));
					Vector offsetZ = viewNormal.clone().multiply(FastUtils.sin(signum * mAngle));
					Vector offset = offsetX.add(offsetZ).rotateAroundAxis(viewDirection, tilt);
					Vector delta = viewNormalUp.clone().rotateAroundAxis(viewDirection, tilt).crossProduct(offset).multiply(signum);
					Location critLoc = eyeLocation.clone().add(offset.clone().multiply(1.6));
					double sweepDist = FastUtils.randomDoubleInRange(1.6, Math.max(1.8, radius));
					Location sweepLoc = eyeLocation.clone().add(offset.clone().multiply(sweepDist));
					Location endLoc = eyeLocation.clone().add(offset.clone().multiply(radius));

					new PartialParticle(Particle.CLOUD, sweepLoc)
						.directionalMode(true)
						.delta(delta.getX(), delta.getY(), delta.getZ())
						.extraRange(0.2, 0.3)
						.spawnAsPlayerActive(player);

					new PPLine(Particle.CRIT, critLoc, endLoc, 0.06)
						.countPerMeter(1)
						.extra(0.3)
						.spawnAsPlayerActive(player);
					mAngle += Math.PI / 45;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

		world.playSound(soundLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.1f);
		world.playSound(soundLoc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.4f);
		world.playSound(soundLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 2.0f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.5f, 2.0f);
		world.playSound(soundLoc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 0.4f, 2.0f);
		world.playSound(soundLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 2.0f);
		world.playSound(soundLoc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 0.5f, 1.6f);
		world.playSound(soundLoc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.7f);
		world.playSound(soundLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.5f, 2.0f);
	}

	public void rondeGainStackEffect(Player player, Location loc) {
		player.playSound(loc, Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.PLAYERS, 0.7f, 0.8f);
		player.playSound(loc, Sound.ENTITY_GLOW_SQUID_SQUIRT, SoundCategory.PLAYERS, 0.3f, 1.6f);
	}

	public void rondeTickEffect(Player player, int charges, int mTicks) {
		new PPCircle(Particle.REDSTONE, player.getLocation(), 0.5).count(4 * charges).delta(0.15).data(SWORDSAGE_COLOR).spawnAsPlayerBuff(player);

	}
}
