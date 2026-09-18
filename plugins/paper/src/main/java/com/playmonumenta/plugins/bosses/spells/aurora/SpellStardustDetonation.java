package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellStardustDetonation extends Spell implements CooldownReducible {
	private static final int DURATION = 4 * 20;
	private static final int DAMAGE = 50;
	private static final double DAMAGE_PERCENT = 0.15;
	private static final String SPELL_NAME = "Stardust Detonation";
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Aurora.BlockDestroyer mBlockDestroyer;
	private final double mRadius;

	private final SpellCooldownManager mSpellCooldownManager;

	public SpellStardustDetonation(Plugin plugin, LivingEntity boss, Location center, double radius, Aurora.BlockDestroyer blockDestroyer) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = center;
		mBlockDestroyer = blockDestroyer;
		mRadius = radius;

		mSpellCooldownManager = new SpellCooldownManager(35 * 20, boss::isValid, boss::hasAI);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}

	@Override
	public void run() {
		mSpellCooldownManager.setOnCooldown();

		List<LivingEntity> targets = new ArrayList<>(Aurora.playersInRange(mBoss.getLocation()));
		if (targets.size() <= 1) {
			targets.add(mBoss);
		}

		new PartialParticle(Particle.END_ROD, mBoss.getLocation())
			.count(32)
			.extra(1.2)
			.spawnAsBoss();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= DURATION) {
					impact(targets);
					this.cancel();
					return;
				}

				for (LivingEntity target : targets) {
					telegraph(target, mTicks);
				}
				targets.removeIf(target -> target instanceof Player player && !Aurora.isAlive(player));

				mTicks++;
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();
				targets.clear();
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void impact(List<LivingEntity> targets) {
		Set<Block> blocks = new HashSet<>();
		World world = mBoss.getWorld();

		for (LivingEntity target : targets) {
			Location location = Aurora.withSurfaceY(target.getLocation(), mCenter);
			boolean intersectsOther = false;

			for (LivingEntity other : targets) {
				if (other == target) {
					continue;
				}
				Location otherLoc = Aurora.withSurfaceY(other.getLocation(), mCenter);
				boolean intersects = LocationUtils.xzDistance(location, otherLoc) <= mRadius;
				if (intersects) {
					intersectsOther = true;

					new PPCircle(Particle.EXPLOSION_LARGE, location, mRadius)
						.count(20)
						.spawnAsBoss();

					if (target == mBoss) {
						continue;
					}

					DamageUtils.damage(mBoss, target, DamageEvent.DamageType.BLAST, DAMAGE, null, true, false, SPELL_NAME);
					DamageUtils.damagePercentHealth(mBoss, target, DAMAGE_PERCENT, false, false, SPELL_NAME);

					List<Block> blocksInSphere = BlockUtils.getBlocksInSphere(location, mRadius);
					blocksInSphere.removeIf(block -> block.getLocation().toCenterLocation().distanceSquared(otherLoc) > mRadius * mRadius);
					blocks.addAll(blocksInSphere);
				}
			}
			if (intersectsOther) {
				mBoss.swingMainHand();
				world.playSound(location, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 1.5f, 2.0f);
				world.playSound(location, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.HOSTILE, 1.5f, 1.0f);
				world.playSound(location, Sound.ENTITY_BLAZE_HURT, SoundCategory.HOSTILE, 1.0f, 1.5f);
				world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 1.25f);
			}
		}

		mBlockDestroyer.destroy(blocks);
	}

	private void telegraph(LivingEntity target, int ticks) {
		if (ticks % 5 == 0) {
			Location location = Aurora.withSurfaceY(target.getLocation(), mCenter);
			for (int i = 0; i < 4; i++) {
				new PPCircle(Particle.REDSTONE, location, mRadius)
					.count(18)
					.delta(0, 0.25, 0)
					.data(new Particle.DustOptions(rollSolarColor(), 1.8f))
					.spawnAsBoss();
			}
			target.getWorld().playSound(location, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1.5f, 1.2f * ticks / DURATION);
			target.getWorld().playSound(location, Sound.ITEM_FLINTANDSTEEL_USE, SoundCategory.HOSTILE, 1.5f, 1.5f * ticks / DURATION);
		}
	}

	private Color rollSolarColor() {
		int randColorGen = FastUtils.randomIntInRange(0, 160);
		return Color.fromRGB(240, randColorGen <= 120 ? 80 + randColorGen : 80, randColorGen > 120 ? randColorGen - 40 : 80);
	}

	@Override
	public int cooldownTicks() {
		return DURATION + Aurora.SPELL_INTERVAL;
	}

	@Override
	public void reduceCooldown(int reduction) {
		mSpellCooldownManager.reduceCooldown(reduction);
	}
}
