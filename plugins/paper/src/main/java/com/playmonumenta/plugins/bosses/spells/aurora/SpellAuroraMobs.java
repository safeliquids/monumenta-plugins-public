package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellCooldownManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellAuroraMobs extends Spell {
	public static final LoSPool NORMAL_POOL = new LoSPool.LibraryPool("~AuroraNormal");
	private static final int NORMAL_COUNT = 3;
	private static final int NORMAL_COUNT_200_RAGE = 5;
	private static final int SPAWN_DURATION = 2 * 20;
	private static final double HEIGHT = 10;

	private final SpellCooldownManager mSpellCooldownManager;

	private final Plugin mPlugin;
	private final Location mCenter;
	private final double mRage;

	public SpellAuroraMobs(Plugin plugin, LivingEntity boss, Location center, double spellCooldownMult, double rage) {
		mPlugin = plugin;
		mCenter = center;
		mRage = rage;
		mSpellCooldownManager = new SpellCooldownManager((int) (20 * 20 * spellCooldownMult), 5 * 10, boss::isValid, boss::hasAI);
	}

	@Override
	public boolean canRun() {
		return !mSpellCooldownManager.onCooldown();
	}

	@Override
	public void run() {
		if (!canRun()) {
			return;
		}
		mSpellCooldownManager.setOnCooldown();

		for (int i = 0; i < (mRage >= 200 ? NORMAL_COUNT_200_RAGE : NORMAL_COUNT); i++) {
			spawnMob();
		}
	}

	private void spawnMob() {
		Location spawnTarget = Aurora.getRandomArenaLocation(mCenter, 10);
		Location startLoc = spawnTarget.clone().add(0, HEIGHT, 0);

		Entity spawn = NORMAL_POOL.spawn(startLoc);
		if (!(spawn instanceof LivingEntity livingSpawn)) {
			return;
		}
		mPlugin.mEffectManager.addEffect(livingSpawn, "AuroraSpawn", new PercentDamageReceived(SPAWN_DURATION, -0.5));
		livingSpawn.setAI(false);
		ArrayList<LivingEntity> prior = new ArrayList<>();
		EntityUtils.getStackedMobsAbove(livingSpawn, prior);
		for (LivingEntity livingEntity : prior) {
			Aurora.rageBuff(livingEntity, mRage, false);
		}

		new BukkitRunnable() {
			private final Location mCurrentLoc = startLoc.clone();
			private final Vector mDisplacement = new Vector(0, -HEIGHT / SPAWN_DURATION, 0);
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= SPAWN_DURATION) {
					livingSpawn.setAI(true);

					this.cancel();
					return;
				}

				mCurrentLoc.add(mDisplacement);
				livingSpawn.teleport(mCurrentLoc);

				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
