package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import org.jetbrains.annotations.NotNull;

public class Adaptability implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Adaptability";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ADAPTABILITY;
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.DEFENSE_SITUATIONAL;
	}
}
