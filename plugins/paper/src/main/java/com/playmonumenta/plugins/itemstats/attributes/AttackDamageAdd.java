package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AttackDamageAdd implements Attribute {

	public static double CRIT_BONUS = 1.5;

	@Override
	public String getName() {
		return "Attack Damage Add";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.ATTACK_DAMAGE_ADD;
	}

	@Override
	public double getPriorityAmount() {
		return 2;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			// For some reason this is the case that attack damage is meant to be 1 more than the damage you do here
			event.setBaseDamage(player.getCooledAttackStrength(0) * value);

			// Centralise crit functionality
			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			event.setIsCrit(PlayerUtils.isFallingAttack(player) &&
				playerItemStats != null &&
				playerItemStats.getItemStats().get(EnchantmentType.CUMBERSOME) == 0
			);
		}
	}
}
