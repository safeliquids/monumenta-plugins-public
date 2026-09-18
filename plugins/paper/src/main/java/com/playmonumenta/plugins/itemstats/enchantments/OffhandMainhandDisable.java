package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;

public class OffhandMainhandDisable implements Enchantment {

	@Override
	public String getName() {
		return "OffhandMainhandDisable";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.OFFHAND_MAINHAND_DISABLE;
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.FIRST;
	}

}
