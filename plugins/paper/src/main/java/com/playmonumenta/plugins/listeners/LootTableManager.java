package com.playmonumenta.plugins.listeners;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.utils.NmsUtils;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

public class LootTableManager implements Listener {
	public static final LootTableManager INSTANCE = new LootTableManager();

	private final Object2BooleanMap<NamespacedKey> mHasBonus = new Object2BooleanOpenHashMap<>();
	private @Nullable List<NamespacedKey> mTables = null;

	private LootTableManager() {
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void serverResourcesReloadedEvent(ServerResourcesReloadedEvent event) {
		mHasBonus.clear();
		mTables = null;
	}

	private boolean hasBonus0(NamespacedKey key) {
		if (mHasBonus.containsKey(key)) {
			return mHasBonus.getBoolean(key);
		}

		final var table = Bukkit.getLootTable(key);

		if (table == null) {
			return false;
		}

		final var res = NmsUtils.getVersionAdapter().hasBonusRolls(table) ||
			NmsUtils.getVersionAdapter().lootTableChildren(table).anyMatch(this::hasBonus0);

		mHasBonus.put(key, res);
		return res;
	}

	/**
	 * Determines whether the loot table at {@code key} has bonus rolls.
	 * <p><i>Thread Safety: <b>unsafe</b></i></p>
	 *
	 * @param key the name of the loot table
	 * @return whether the table has bonus rolls, or false if the table isn't found
	 */
	public static boolean hasBonus(NamespacedKey key) {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "can't call LootTableManger#getTables() off-main");
		return INSTANCE.hasBonus0(key);
	}

	/**
	 * Obtains all active loot tables.
	 * <p><i>Thread Safety: <b>unsafe</b></i></p>
	 *
	 * @return a read-only view of all loot tables
	 */
	public List<NamespacedKey> getTables() {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "can't call LootTableManger#getTables() off-main");
		if (mTables == null) {
			mTables = NmsUtils.getVersionAdapter().allLootTables().toList();
		}

		return Collections.unmodifiableList(mTables);
	}
}
