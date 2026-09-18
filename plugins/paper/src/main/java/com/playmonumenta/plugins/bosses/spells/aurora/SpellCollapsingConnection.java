package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCollapsingConnection extends Spell {
	private static final int GRACE_PERIOD = 6 * 20;
	private static final int RADIUS = 2;
	private static final double PULL_THRESHOLD = 17;
	private static final float PULL_SPEED = 0.1f;
	private static final int PULL_COOLDOWN = 2 * 20;
	private static final int METEOR_INTERVAL = 2 * 20;
	private static final int METEOR_DURATION = 2 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Aurora.BlockDestroyer mBlockDestroyer;
	private final Location mCircle1;
	private final Location mCircle2;
	private final int mMeteorCount;

	private final List<Player> mConnected = new ArrayList<>();
	private double mPullingMultiplier = 1;

	public SpellCollapsingConnection(Plugin plugin, LivingEntity boss, Location center, int meteorCount, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mCircle1 = mCenter.clone().add(4, 0, 4);
		mCircle2 = mCenter.clone().subtract(4, 0, 4);
		mBlockDestroyer = blockDestroyer;
		mMeteorCount = meteorCount;
	}

	@Override
	public void run() {
		World world = mCenter.getWorld();

		new PPPillar(Particle.SPELL_WITCH, mCenter, 40)
			.count(120)
			.distanceFalloff(45)
			.spawnAsBoss();

		List<Player> players = Aurora.playersInRange(mCenter);
		players.forEach(player -> player.showTitle(Title.title(
			Component.text("ʜᴇᴀʀᴛꜱ ᴄʜᴀɪɴᴇᴅ", Aurora.DELFIA_COLOR),
			Component.text("Minds Intertwined", Aurora.AURORA_COLOR),
			Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
		)));

		world.playSound(mCenter, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 3.5f, 0.4f);
		world.playSound(mCenter, Sound.BLOCK_TRIAL_SPAWNER_SPAWN_MOB, 3.5f, 0.8f);
		world.playSound(mCenter, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 5.0f, 0.1f);
		world.playSound(mCenter, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 5.0f, 0.6f);
		world.playSound(mCenter, Sound.ENTITY_BLAZE_AMBIENT, 2.5f, 0.5f);
		world.playSound(mCenter, Sound.ITEM_TRIDENT_RIPTIDE_1, 4.0f, 0.1f);
		world.playSound(mCenter, Sound.ENTITY_WITHER_HURT, 2.0f, 0.6f);

		if (players.size() == 1) {
			linkBoss(players.getFirst());
			return;
		}
		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				List<Player> players1 = new Hitbox.SphereHitbox(mCircle1, RADIUS).getHitPlayers(true);
				List<Player> players2 = new Hitbox.SphereHitbox(mCircle2, RADIUS).getHitPlayers(true);

				new PPCircle(players1.isEmpty() ? Particle.SPELL_INSTANT : Particle.VILLAGER_HAPPY, mCircle1, RADIUS)
					.count(25)
					.spawnAsBoss();
				new PPCircle(players2.isEmpty() ? Particle.SPELL_INSTANT : Particle.VILLAGER_HAPPY, mCircle2, RADIUS)
					.count(25)
					.spawnAsBoss();

				if (mTicks % METEOR_INTERVAL == 0 && mTicks >= GRACE_PERIOD - METEOR_DURATION) {
					summonMeteors();
				}

				if (!players1.isEmpty() && !players2.isEmpty()) {
					linkPlayers(players1.getFirst(), players2.getFirst());

					this.cancel();
				}
				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2));
	}

	public void onStartAbsorbPower() {
		mPullingMultiplier = 10;
	}

	public void onStopAbsorbPower() {
		mPullingMultiplier = 1;
	}

	public void onStarShower() {
		mPullingMultiplier = 1.5;
		mConnected.forEach(player -> {
			player.sendMessage(Component.text("The connection weakens under the harsh starlight.", NamedTextColor.AQUA, TextDecoration.ITALIC));
		});
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPullingMultiplier = 1.4, SpellStarShower.COMPLETE_DURATION - 3 * 20);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPullingMultiplier = 1.3, SpellStarShower.COMPLETE_DURATION - 2 * 20);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPullingMultiplier = 1.2, SpellStarShower.COMPLETE_DURATION - 20);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPullingMultiplier = 1.1, SpellStarShower.COMPLETE_DURATION - 10);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPullingMultiplier = 1, SpellStarShower.COMPLETE_DURATION);
	}

	private void linkPlayers(Player player1, Player player2) {
		mConnected.add(player1);
		mConnected.add(player2);

		MessagingUtils.sendBoldTitle(player1, Component.text("CONNECTION MADE", NamedTextColor.LIGHT_PURPLE), player2.name());
		MessagingUtils.sendBoldTitle(player2, Component.text("CONNECTION MADE", NamedTextColor.LIGHT_PURPLE), player1.name());

		mConnected.forEach(player -> {
			player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.4f);
		});

		BukkitRunnable pullTask = new BukkitRunnable() {
			int mCooldown = 0;
			int mTicks = 0;

			@Override
			public void run() {
				Location p2Loc = player2.getLocation();
				Location p1Loc = player1.getLocation();
				p2Loc.setY(p1Loc.getY());
				double pullThreshold = PULL_THRESHOLD * mPullingMultiplier;
				if (p1Loc.distance(p2Loc) > pullThreshold - 3) {
					mConnected.forEach(player -> {
						player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.25f, 1.1f);
					});
					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(player1), LocationUtils.getHalfHeightLocation(player2))
						.countPerMeter(3)
						.data(new Particle.DustOptions(Color.RED, 1.2f))
						.spawnAsBoss();
				} else {
					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(player1), LocationUtils.getHalfHeightLocation(player2))
						.countPerMeter(3)
						.data(new Particle.DustOptions(Color.GREEN, 1.2f))
						.spawnAsBoss();
				}

				if (mCooldown > 0) {
					mCooldown -= 2;
					return;
				}
				if (mTicks >= GRACE_PERIOD && p1Loc.distance(p2Loc) > pullThreshold) {
					mConnected.forEach(player -> {
						player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
					});
					MovementUtils.pullTowards(p2Loc, player1, PULL_SPEED);
					MovementUtils.pullTowards(p1Loc, player2, PULL_SPEED);
					summonMeteors();

					mCooldown = PULL_COOLDOWN;
				}

				mTicks += 5;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				player1.sendMessage(Component.text(String.format("You feel the presence of %s's mind leave... Your binds are broken!", player2.getName()), NamedTextColor.AQUA));
				player2.sendMessage(Component.text(String.format("You feel the Presence of %s's mind leave... Your binds are broken!", player1.getName()), NamedTextColor.AQUA));
				super.cancel();
			}
		};
		mActiveRunnables.add(pullTask);
		pullTask.runTaskTimer(mPlugin, 0, 5);
	}

	private void linkBoss(Player player1) {
		mConnected.add(player1);
		player1.playSound(player1.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 2.0f, 0.4f);

		BukkitRunnable pullTask = new BukkitRunnable() {
			int mCooldown = 0;
			int mTicks = 0;

			@Override
			public void run() {
				Location p2Loc = mBoss.getLocation();
				Location p1Loc = player1.getLocation();
				p2Loc.setY(p1Loc.getY());
				double pullThreshold = PULL_THRESHOLD * mPullingMultiplier;
				if (p1Loc.distance(p2Loc) > pullThreshold - 3) {
					player1.playSound(player1.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.25f, 1.1f);
					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(player1), LocationUtils.getHalfHeightLocation(mBoss))
						.countPerMeter(3)
						.data(new Particle.DustOptions(Color.RED, 1.2f))
						.spawnAsBoss();
				} else {
					new PPLine(Particle.REDSTONE, LocationUtils.getHalfHeightLocation(player1), LocationUtils.getHalfHeightLocation(mBoss))
						.countPerMeter(3)
						.data(new Particle.DustOptions(Color.GREEN, 1.2f))
						.spawnAsBoss();
				}

				if (mCooldown > 0) {
					mCooldown -= 2;
					return;
				}
				if (mTicks >= GRACE_PERIOD && p1Loc.distance(p2Loc) > pullThreshold) {
					player1.playSound(player1.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.0f, 0.5f);
					MovementUtils.pullTowards(p2Loc, player1, PULL_SPEED);
					summonMeteors();

					mCooldown = PULL_COOLDOWN;
				}

				mTicks += 5;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				player1.sendMessage("You feel the presence of Aurora's mind leave... Your bind is is broken!");
				super.cancel();
			}
		};
		mActiveRunnables.add(pullTask);
		pullTask.runTaskTimer(mPlugin, 0, 5);
	}

	@Override
	public void cancel() {
		mConnected.forEach(player -> {
			player.showTitle(Title.title(
				Component.text("ʜᴇᴀʀᴛs ᴀʟɪɢɴᴇᴅ", NamedTextColor.WHITE),
				Component.text("Minds Synced", NamedTextColor.GRAY),
				Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))
			));
		});
		mConnected.clear();
		super.cancel();
	}

	public void summonMeteors() {
		for (int i = 0; i < mMeteorCount; i++) {
			Location fallLoc = Aurora.getRandomArenaLocation(mCenter, Aurora.PROTECTED_RADIUS + RADIUS);
			Location endLoc = fallLoc.clone().subtract(0, 5, 0);
			World world = fallLoc.getWorld();

			new PPCircle(Particle.REDSTONE, fallLoc, 1)
				.data(new Particle.DustOptions(Color.RED, 1.4f))
				.count(50)
				.spawnAsBoss();

			mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(fallLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1.6f, 1.3f);

				new PPLightning(Particle.ELECTRIC_SPARK, fallLoc, 20, 1)
					.count(5)
					.extra(0.5)
					.duration(10)
					.spawnAsBoss();
			}, METEOR_DURATION - 10));

			mActiveTasks.add(Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				new PartialParticle(Particle.EXPLOSION_LARGE, fallLoc).minimumCount(1).spawnAsBoss();
				world.playSound(fallLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.9f, 0.5f);

				mBlockDestroyer.destroy(BlockUtils.getBlocksInPillar(endLoc, 0.6, 5));
			}, METEOR_DURATION));
		}
	}


	@Override
	public int cooldownTicks() {
		return GRACE_PERIOD;
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}
}
