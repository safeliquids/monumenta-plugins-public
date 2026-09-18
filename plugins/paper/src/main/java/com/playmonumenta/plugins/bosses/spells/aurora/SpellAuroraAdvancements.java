package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellAuroraAdvancements extends Spell {
	private final List<Player> mPlayers;
	private final long mBlocksThreshold;

	private final List<Player> mFragilePlayers = new ArrayList<>();
	private final Map<Player, Integer> mVoidCount = new HashMap<>();
	private boolean mNoAuroraLoom = true;
	private boolean mUniversalCoordination = true;
	private boolean mCheckNextShower = false;
	private boolean mClassCorrect = true;
	private long mBlocksDestroyed;

	public SpellAuroraAdvancements(Location center, List<Player> players) {
		mPlayers = players;
		long blockCount = BlockUtils.getBlocksInCylinder(center.clone().subtract(0, 1, 0), Aurora.ARENA_RADIUS, 10).stream()
			.filter(Block::isSolid)
			.count();
		mBlocksThreshold = blockCount * 9 / 10;
	}

	@Override
	public void run() {
		if (mPlayers.stream().anyMatch(player -> AbilityUtils.getSpecNum(player) != Mage.ARCANIST_SPEC_ID)) {
			mClassCorrect = false;
		}
	}

	public void bossDeath(int rage) {
		AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/aurora");

		AdvancementUtils.grantAdvancement(mFragilePlayers, "monumenta:challenges/r3/aurora/fragile_victory");
		if (rage >= 100) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/aurora_fury");
			if (mPlayers.size() == 2) {
				AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/enraged_duo");
			}
			if (mNoAuroraLoom) {
				AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/purist");
			}
		}
		if (rage >= 33 && mPlayers.size() == 3 && mClassCorrect) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/arcane_trio");
		}
		for (Map.Entry<Player, Integer> entry : mVoidCount.entrySet()) {
			if (entry.getValue() >= 10) {
				AdvancementUtils.grantAdvancement(entry.getKey(), "monumenta:challenges/r3/aurora/one_with_the_abyss");
			}
		}
	}

	public void usedLoamskattarCatalyst() {
		mNoAuroraLoom = false;
	}

	public void onStarShowerBreak(int count) {
		if (count != 4) {
			mUniversalCoordination = false;
		}
		if (mCheckNextShower && mUniversalCoordination) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/universal_coordination");
		}
	}

	public void enableCoordinationCheck() {
		mCheckNextShower = true;
	}

	public void addDestroyedBlocks(long count) {
		mBlocksDestroyed += count;
		if (mBlocksDestroyed >= mBlocksThreshold) {
			mBlocksDestroyed = Long.MIN_VALUE;
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/such_destruction");
		}
	}

	public void onVoid(Player player) {
		int voidCount = mVoidCount.getOrDefault(player, 0) + 1;
		mVoidCount.put(player, voidCount);
	}

	public void blackHoleEatMob(LivingEntity entity) {
		if (EntityUtils.isElite(entity)) {
			AdvancementUtils.grantAdvancement(mPlayers, "monumenta:challenges/r3/aurora/noble_spaghettification");
		}
	}

	public void onSurgeDebuff(Player player, double power) {
		if (power >= 1.0 && !mFragilePlayers.contains(player)) {
			mFragilePlayers.add(player);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
