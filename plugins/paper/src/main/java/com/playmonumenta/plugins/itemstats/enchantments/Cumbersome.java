package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

public class Cumbersome implements Enchantment {
	@Override
	public String getName() {
		return "Cumbersome";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CUMBERSOME;
	}

	@Override
	// handled in AttackDamageAdd
	public double getPriorityAmount() {
		return 5000;
	}
}
