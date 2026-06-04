package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SpellPower implements Attribute {

	@Override
	public String getName() {
		return "Spell Power";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.SPELL_DAMAGE;
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.WEAPON_BASE_DAMAGE;
	}

	@Override
	public double getDefaultValue() {
		return 1;
	}

	public static double getSpellDamage(Plugin plugin, @Nullable Player player, int damage) {
		return getSpellDamage(plugin, player, (float) damage);
	}

	public static double getSpellDamage(Plugin plugin, @Nullable Player player, double damage) {
		if (player == null) {
			return damage;
		}
		return getSpellDamage(plugin.mItemStatManager.getPlayerItemStats(player), damage);
	}

	public static double getSpellDamage(@Nullable ItemStatManager.PlayerItemStats playerItemStats, double damage) {
		if (playerItemStats == null) {
			return damage;
		}
		return damage * playerItemStats.getItemStats().get(AttributeType.SPELL_DAMAGE);
	}
}
