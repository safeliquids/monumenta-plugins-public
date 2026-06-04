package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;

public class PotionDamage implements Attribute {
	@Override
	public String getName() {
		return "Potion Damage";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.POTION_DAMAGE;
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.WEAPON_BASE_DAMAGE;
	}
}
