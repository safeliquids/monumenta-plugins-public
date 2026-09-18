package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
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
			DamageType newType = FastUtils.getRandomElement(POSSIBLE_DAMAGE_TYPES);
			event.setType(newType);
		}
	}

}
