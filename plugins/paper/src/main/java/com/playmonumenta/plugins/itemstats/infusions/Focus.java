package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Focus implements Infusion {

	public static final double[] DAMAGE_FOR_REGION = {0.01, 0.0125, 0.015}; // r1, r2, r3
	public static final EnumSet<DamageType> AFFECTED_TYPES = DamageType.getAllProjectileTypes();

	@Override
	public String getName() {
		return "Focus";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.FOCUS;
	}

	@Override
	public double getPriorityAmount() {
		return 24;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (AFFECTED_TYPES.contains(type)) {
			double damageBuffPct = value * getDamageForRegion(player);
			event.updateGearDamageWithMultiplier(1.0 + damageBuffPct, DamageType.getAllProjectileTypes());
		}
	}

	public static double getDamageForRegion(Player player) {
		if (ServerProperties.getAbilityEnhancementsEnabled(player)) {
			return DAMAGE_FOR_REGION[2];
		} else if (ServerProperties.getClassSpecializationsEnabled(player)) {
			return DAMAGE_FOR_REGION[1];
		} else {
			return DAMAGE_FOR_REGION[0];
		}
	}
}
