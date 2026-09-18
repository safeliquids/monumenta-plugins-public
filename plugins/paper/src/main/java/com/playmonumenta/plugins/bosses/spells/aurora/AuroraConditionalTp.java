package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AuroraConditionalTp extends Spell {
	private final LivingEntity mBoss;
	private final Location mCenter;
	private final Consumer<Location> mTpFunc;

	private int mTpCd;

	public AuroraConditionalTp(LivingEntity boss, Location center, Consumer<Location> tpFunc) {
		mBoss = boss;
		mCenter = center;
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
		if (location.getY() <= mCenter.getY() - 5 ||
			LocationUtils.xzDistance(location, mCenter) >= Aurora.ARENA_RADIUS ||
			!location.add(0, 1, 0).getBlock().isPassable()
		) {
			mTpCd = 30;
			List<Player> players = Aurora.playersInRange(mCenter);
			if (players.isEmpty()) {
				return;
			}
			Location pLoc = FastUtils.getRandomElement(players).getLocation();
			Location safeLoc = LocationUtils.randomSafeLocationInDonut(pLoc, 4, 8, loc -> !Aurora.withSurfaceY(loc, mCenter).getBlock().isSolid());
			mTpFunc.accept(Aurora.withSurfaceY(safeLoc, mCenter));
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
