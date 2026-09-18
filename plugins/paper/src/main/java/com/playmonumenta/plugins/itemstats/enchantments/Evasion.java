package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Evasion implements Enchantment {
	public static final int DISTANCE = 4;

	@Override
	public @NotNull String getName() {
		return "Evasion";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EVASION;
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.DEFENSE_SITUATIONAL;
	}

	public static double applyEvasion(DamageEvent event, Plugin plugin, Player player) {
		LivingEntity source = event.getSource();
		if (source != null) {
			Location playerLoc = player.getLocation();
			Location mobLoc = source.getLocation();
			if (playerLoc.distance(mobLoc) >= DISTANCE) {
				return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.EVASION);
			}
		}
		return 0;
	}

}
