package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;

// This is handled in AttackDamageAdd
public class Cumbersome implements Enchantment {
	@Override
	public String getName() {
		return "Cumbersome";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CUMBERSOME;
	}
}
