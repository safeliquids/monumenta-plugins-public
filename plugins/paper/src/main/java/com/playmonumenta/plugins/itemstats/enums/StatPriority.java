package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.itemstats.ItemStatManager;

/**
 * Priority is determined by</p>
 * <p>1) Ordinal in {@link StatPriority }.</p>
 * <p>2) Ordinal in {@link AttributeType}/{@link EnchantmentType}/{@link InfusionType}.</p>
 * @see ItemStatManager#ITEM_STATS
 */
public enum StatPriority {
	FIRST,
	ASPECTS,
	WEAPON_BASE_DAMAGE,
	DAMAGE_MULTIPLY,
	DAMAGING_ENCHANTMENT,
	BEFORE_DEFAULT,
	DEFAULT,
	AFTER_DEFAULT,
	DEFENSE_ENCHANTMENT,
	DEFENSE_SITUATIONAL,
	LATE_DEFENSE_SITUATIONAL,
	GEAR_DEFENSE,
	SHATTERED,
	REGION_SCALING,
	SECOND_WIND,
	ASHES_OF_ETERNITY,
	RESURRECTION,
	LAST,
}
