package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class AuroraConditionalTp extends Spell {
	private final LivingEntity mBoss;
	private final Location mSpawnLoc;
	private final Consumer<Location> mTpFunc;

	private int mTpCd;

	public AuroraConditionalTp(LivingEntity boss, Location spawnLoc, Consumer<Location> tpFunc) {
		mBoss = boss;
		mSpawnLoc = spawnLoc;
		mTpFunc = tpFunc;
		mTpCd = 0;
	}

	@Override
	public void run() {
		if (mTpCd >= 0) {
			mTpCd--;
			return;
		}

		Location location = mBoss.getLocation();
		if (location.getY() <= mSpawnLoc.getY() - 5 ||
			LocationUtils.xzDistance(location, mSpawnLoc) >= Aurora.ARENA_RADIUS ||
			location.add(0, 1, 0).getBlock().isSolid()
		) {
			mTpCd = 30;
			mTpFunc.accept(Aurora.getRandomArenaLocation(mSpawnLoc, 0));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
