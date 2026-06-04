package com.playmonumenta.plugins.cosmetics.skills.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BodyguardCS implements CosmeticSkill {
	public static class BodyguardDisplay {
		private final ArrayList<ItemDisplay> mDisplayList;
		private final Player mPlayer;
		private final BukkitRunnable mRunnable;

		private int mT = 0;
		private int mDuration;

		public BodyguardDisplay(Player effectedPlayer, BodyguardCS cosmetic, int duration) {
			mDisplayList = new ArrayList<>();
			mPlayer = effectedPlayer;
			mDuration = duration;

			for (int i = 0; i < 3; i++) {
				Location loc = effectedPlayer.getLocation();
				ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class);
				EntityUtils.setRemoveEntityOnUnload(display);
				display.setItemStack(DisplayEntityUtils.generateRPItem(cosmetic.getActiveMaterial(), cosmetic.getActiveName()));
				display.setTransformation(
					new Transformation(
						new Vector3f(),
						new AxisAngle4f(),
						new Vector3f(0.5f, 0.5f, 0.5f),
						new AxisAngle4f()
					));
				display.setTeleportDuration(2);
				display.setInterpolationDelay(0);
				mDisplayList.add(display);
			}

			mRunnable = createRunnable();
			mRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		public BukkitRunnable createRunnable() {
			return new BukkitRunnable() {
				private int mRotate = 0;

				@Override
				public void run() {
					if (mT >= mDuration) {
						remove();
						this.cancel();
						return;
					}

					for (int i = 0; i < mDisplayList.size(); i++) {
						double angleOffset = 360 * (i / (double) mDisplayList.size());

						double mRotationAngle = 10 * mRotate + angleOffset;
						mRotationAngle %= 360;
						double toRadians = Math.toRadians(mRotationAngle);

						Location halfHeight = LocationUtils.getHalfHeightLocation(mPlayer);
						Location loc = halfHeight.clone().add(
							FastUtils.cos(toRadians) * 0.5,
							0.1,
							FastUtils.sin(toRadians) * 0.5
						);
						ItemDisplay display = mDisplayList.get(i);

						if (!display.isValid()) {
							continue;
						}

						Vector dir = LocationUtils.getVectorTo(halfHeight, display.getLocation());
						loc.setDirection(dir);
						loc.setPitch(0);

						display.teleport(loc);
					}

					mT++;
					mRotate++;
				}
			};
		}

		public void updateItemDisplay(Material material, String name) {
			mDisplayList.forEach(item -> item.setItemStack(DisplayEntityUtils.generateRPItem(material, name)));
		}

		public void extend(int newDuration) {
			mT = 0;
			mDuration = newDuration;
		}

		public void remove() {
			mRunnable.cancel();
			mDisplayList.forEach(ItemDisplay::remove);
		}
	}

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.BODYGUARD;
	}

	@Override
	public Material getDisplayItem() {
		return Material.IRON_CHESTPLATE;
	}

	public Material getInactiveMaterial() {
		return Material.APPLE;
	}

	public String getInactiveName() {
		return "BodyguardInactive";
	}

	public Material getActiveMaterial() {
		return Material.GOLDEN_APPLE;
	}

	public String getActiveName() {
		return "BodyguardActive";
	}

	public void onBodyguard(Player player, World world, Location loc) {
		generalBodyguardSounds(world, loc);
		new PartialParticle(Particle.FLAME, loc.clone().add(0, 0.15, 0), 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(player);
	}

	public void onBodyguardOther(Player player, Player target, World world) {
		new PPLine(Particle.FLAME, player.getEyeLocation(), target.getEyeLocation())
			.countPerMeter(12)
			.delta(0.25)
			.spawnAsPlayerActive(player);

		Location targetLoc = target.getLocation();

		new PPExplosion(Particle.FLAME, targetLoc.clone().add(0, 0.15, 0))
			.flat(true)
			.speed(1)
			.count(120)
			.extraRange(0.1, 0.4)
			.spawnAsPlayerActive(player);

		new PPExplosion(Particle.EXPLOSION_NORMAL, targetLoc.clone().add(0, 0.15, 0))
			.flat(true)
			.speed(1)
			.count(60)
			.extraRange(0.15, 0.5)
			.spawnAsPlayerActive(player);

		world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.PLAYERS, 2.0f, 0.8f);
		world.playSound(targetLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.7f, 0.1f);
		world.playSound(targetLoc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.PLAYERS, 1.0f, 0.1f);

		generalBodyguardSounds(world, targetLoc);
	}

	public void bodyguardLink(Player player, Player other) {
		Location playerLoc = LocationUtils.getHalfHeightLocation(player);
		Location otherLoc = LocationUtils.getHalfHeightLocation(other);

		new PPLine(Particle.REDSTONE, otherLoc, playerLoc)
			.count(15)
			.delta(0.1)
			.data(new Particle.DustOptions(Color.GRAY, 1.0f))
			.spawnAsPlayerActive(player);

		new PPLine(Particle.REDSTONE, otherLoc, playerLoc)
			.count(15)
			.delta(0.1)
			.data(new Particle.DustOptions(Color.fromRGB(70, 70, 70), 0.8f))
			.spawnAsPlayerActive(player);
	}

	public void damageTransfer(Player player, Player other) {
		Location playerLoc = LocationUtils.getHalfHeightLocation(player);
		Location otherLoc = LocationUtils.getHalfHeightLocation(other);
		World world = playerLoc.getWorld();

		world.playSound(playerLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.PLAYERS, 1, 2);
		world.playSound(otherLoc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1, 2);

		new PartialParticle(Particle.SMALL_FLAME, otherLoc, 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(player);

		new PPLine(Particle.FLAME, otherLoc, playerLoc)
			.count(15)
			.delta(0.1)
			.spawnAsPlayerActive(player);

		new PPLine(Particle.SMOKE_NORMAL, otherLoc, playerLoc)
			.count(15)
			.delta(0.1)
			.spawnAsPlayerActive(player);

		new PartialParticle(Particle.FLAME, playerLoc, 25, 0.2, 0, 0.2, 0.1).spawnAsPlayerActive(player);
	}

	public BodyguardDisplay bodyguardEffect(Player effectedPlayer, int duration) {
		return new BodyguardDisplay(effectedPlayer, this, duration);
	}

	private void generalBodyguardSounds(World world, Location loc) {
		world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ITEM_ARMOR_EQUIP_NETHERITE, SoundCategory.PLAYERS, 2.0f, 0.1f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 2.0f, 0.1f);
	}
}
