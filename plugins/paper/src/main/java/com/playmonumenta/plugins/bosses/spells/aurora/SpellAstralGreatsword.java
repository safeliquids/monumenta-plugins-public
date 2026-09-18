package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellAstralGreatsword extends Spell implements CooldownReducible {
	private static final String SPELL_NAME = "Astral Greatsword";
	private static final int DAMAGE = 50;
	private static final double DAMAGE_PERCENT = 0.2;
	private static final double HITBOX_SIZE = 2.5;
	private static final float KNOCKBACK = 1.1f;
	private static final int SPAWN_INTERVAL = 20;
	private static final int CHARGE_TIME = 10 * 20;
	private static final int HOLD_TIME = 2 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mRage;
	private final Aurora.BlockDestroyer mBlockDestroyer;

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellAstralGreatsword(Plugin plugin, LivingEntity boss, Location center, int rage, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mRage = rage;
		mBlockDestroyer = blockDestroyer;

		mSpellCooldownManager = new SpellCooldownManager(40 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}

	@Override
	public void reduceCooldown(int reduction) {
		mSpellCooldownManager.reduceCooldown(reduction);
	}

	@Override
	public void run() {
		mSpellCooldownManager.setOnCooldown();

		List<Player> players = Aurora.playersInRange(mCenter);
		int swordMultiplier = mRage >= 80 ? 2 : 1;
		int blades = (players.size() + 1) / 2 * swordMultiplier;

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks < blades) {
					Player target = players.get(mTicks / 2);
					spawnBlade(target);
					mTicks++;
				} else {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, SPAWN_INTERVAL);
	}

	private void spawnBlade(Player target) {
		World world = mCenter.getWorld();
		Location bladeLoc = Aurora.withSurfaceY(LocationUtils.randomSafeLocationInDonut(
			target.getLocation(), 3, 12,
			location -> LocationUtils.xzDistance(location, mCenter) <= Aurora.ARENA_RADIUS - 3
		), mCenter).add(0, 2.5, 0);

		world.playSound(bladeLoc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2.5f, 1.8f);
		world.playSound(bladeLoc, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 2.5f, 0.5f);
		world.playSound(bladeLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 3.0f, 2.0f);
		world.playSound(bladeLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 4.0f, 1.6f);

		new PartialParticle(Particle.EXPLOSION_NORMAL, bladeLoc)
			.count(20)
			.extra(2)
			.spawnAsBoss();

		ItemDisplay itemDisplay = world.spawn(bladeLoc, ItemDisplay.class, display -> {
			display.setItemStack(DisplayEntityUtils.generateRPItem(Material.STONE_SWORD, "Abisso Ancestrale"));
			// Look forwards
			display.setTransformation(new Transformation(
				new Vector3f(),
				new Quaternionf(0.27097052f, 0.6532104f, 0.27056867f, 0.6532104f),
				new Vector3f(8),
				new Quaternionf()
			));

			display.setTeleportDuration(3);

			GlowingManager.startGlowing(display, NamedTextColor.LIGHT_PURPLE, -1, GlowingManager.BOSS_SPELL_PRIORITY);
			EntityUtils.setRemoveEntityOnUnload(display);
		});

		@Nullable
		Entity hittable = LibraryOfSoulsIntegration.summon(bladeLoc.clone().subtract(0, 1, 0), "AstralGreatsword");
		if (!(hittable instanceof LivingEntity livingHittable)) {
			MMLog.severe("Aurora: \"AstralGreatsword\" failed to spawn!");
			return;
		}
		Aurora.rageBuff(livingHittable, mRage, true);

		// do not cancel this when phase changes
		new BukkitRunnable() {
			private final Location mSwordLoc = itemDisplay.getLocation();
			private Location mPlayerLoc = target.getLocation().add(0, 1, 0);
			private Vector mDir = mPlayerLoc.clone().subtract(mSwordLoc).toVector().normalize();
			private double mLength = LocationUtils.rayLengthToSphereSurface(mCenter, mSwordLoc, Aurora.ARENA_RADIUS);

			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mSwordLoc.setDirection(mDir);
				if (mTicks >= CHARGE_TIME) {
					world.playSound(bladeLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 3.5f, 1.2f);
					world.playSound(bladeLoc, Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 5.0f, 0.7f);
					world.playSound(bladeLoc, Sound.ENTITY_BREEZE_SHOOT, SoundCategory.HOSTILE, 3.5f, 0.5f);
					world.playSound(bladeLoc, Sound.BLOCK_TRIAL_SPAWNER_BREAK, SoundCategory.HOSTILE, 5.0f, 0.8f);

					Location prev = mSwordLoc.clone();
					Location current = prev.clone();
					mSwordLoc.add(mDir.clone().multiply(mLength));

					Set<Block> blocks = new HashSet<>();
					Set<Player> hitPlayers = new HashSet<>();

					for (double i = 0; i < mLength; i++) {
						current.add(mDir);
						blocks.addAll(BlockUtils.getBlocksInCube(current, HITBOX_SIZE - 1));
						hitPlayers.addAll(new Hitbox.SphereHitbox(current, HITBOX_SIZE).getHitPlayers(true));
					}
					hitPlayers.forEach(player -> {
						BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MELEE, DAMAGE, SPELL_NAME, prev);
						BossUtils.bossDamagePercent(mBoss, player, DAMAGE_PERCENT, prev, SPELL_NAME);
						MovementUtils.knockAway(prev, player, KNOCKBACK, false);
					});

					itemDisplay.teleport(mSwordLoc);
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						itemDisplay.remove();
						mBlockDestroyer.destroy(blocks);
					}, 3);

					// don't remove the display yet
					hittable.remove();
					super.cancel();
					return;
				}
				if (!hittable.isValid()) {
					new PartialParticle(Particle.EXPLOSION_NORMAL, mSwordLoc)
						.count(40)
						.extra(1.5)
						.spawnAsBoss();
					new PartialParticle(Particle.WAX_OFF, mSwordLoc)
						.count(60)
						.extra(15)
						.spawnAsBoss();

					world.playSound(mSwordLoc, Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 1.2f, 1.5f);
					world.playSound(mSwordLoc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.HOSTILE, 1.4f, 0.8f);
					world.playSound(mSwordLoc, Sound.ITEM_TOTEM_USE, SoundCategory.HOSTILE, 0.6f, 0.7f);

					this.cancel();
					return;
				}


				boolean stillAiming = mTicks < CHARGE_TIME - HOLD_TIME;

				Location targetLoc = target.getLocation().add(0, 1, 0);
				if (stillAiming && targetLoc.distanceSquared(mSwordLoc) >= 2) {
					mSwordLoc.setDirection(mDir);
					mPlayerLoc = targetLoc;
					mDir = mPlayerLoc.clone().subtract(mSwordLoc).toVector().normalize();
				}
				mLength = LocationUtils.rayLengthToSphereSurface(mCenter, mSwordLoc, Aurora.ARENA_RADIUS);

				new PPLine(Particle.REDSTONE, mSwordLoc.clone().add(mDir.clone().multiply(5)), mDir, mLength)
					.data(new Particle.DustOptions(stillAiming ? Color.FUCHSIA : Color.RED, 1.36f))
					.countPerMeter(2)
					.spawnAsBoss();

				if (stillAiming) {
					if (mTicks % 20 == 0) {
						world.playSound(target, Sound.BLOCK_LEVER_CLICK, SoundCategory.HOSTILE, 1.0f, 1.5f * mTicks / CHARGE_TIME);
					}
					mSwordLoc.subtract(mDir.clone().multiply(0.5 / (mTicks + 6)));
				} else {
					if (mTicks % 5 == 0) {
						world.playSound(target, Sound.BLOCK_LEVER_CLICK, SoundCategory.HOSTILE, 1.0f, 1.5f);
					}
					mSwordLoc.subtract(mDir.clone().multiply(3.0 / (mTicks - CHARGE_TIME + HOLD_TIME + 6)));

				}
				itemDisplay.teleport(mSwordLoc);
				hittable.teleport(mSwordLoc.clone().subtract(0, 1, 0));
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				itemDisplay.remove();
				hittable.remove();
				super.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return Aurora.SPELL_INTERVAL;
	}
}
