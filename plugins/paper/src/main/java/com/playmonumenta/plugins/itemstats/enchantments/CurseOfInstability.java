package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class CurseOfInstability implements Enchantment {

	private static final List<DamageType> POSSIBLE_DAMAGE_TYPES = List.of(
		DamageType.MELEE,
		DamageType.PROJECTILE,
		DamageType.MAGIC,
		DamageType.BLAST
	);

	// To fix weird interactions with Dodging and the like, Instability
	// will always roll the same type for a given player on a given tick.
	private static int lastCalculatedTick = -1;
	private static final Map<UUID, DamageType> typesThisTick = new HashMap<>();

	@Override
	public @NotNull String getName() {
		return "Curse of Instability";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_INSTABILITY;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		DamageType prevType = event.getType();
		if (POSSIBLE_DAMAGE_TYPES.contains(prevType)) {
			event.setType(getType(player, null));
		}
	}

	/**
	 * Instability will always roll the same type for a given player on a given tick.
	 * If the player has Instability, and this is a damage type randomizable by Instability,
	 * return this semi-random type; otherwise, return the original type.
	 * @param player The player being affected
	 * @param original The original damage type, to be returned if the
	 *                 player does not have Instability (or null to bypass
	 *                 this check)
	 * @return The new damage type to be used
	 */
	public static DamageType getType(Player player, @Nullable DamageType original) {
		if (original != null) {
			if (!POSSIBLE_DAMAGE_TYPES.contains(original)
				|| Plugin.getInstance().mItemStatManager.getEnchantmentLevel(player, EnchantmentType.CURSE_OF_INSTABILITY) == 0) {
				return original;
			}
		}
		if (Bukkit.getCurrentTick() != lastCalculatedTick) {
			typesThisTick.clear();
			lastCalculatedTick = Bukkit.getCurrentTick();
		}
		return typesThisTick.computeIfAbsent(player.getUniqueId(), p -> FastUtils.getRandomElement(POSSIBLE_DAMAGE_TYPES));
	}
}
