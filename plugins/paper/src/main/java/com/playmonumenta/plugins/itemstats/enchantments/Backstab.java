package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class Backstab implements Enchantment {
	private static final double DAMAGE_BONUS_PER_LEVEL = 0.15;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeTypes();

	@Override
	public String getName() {
		return "Backstab";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.BACKSTAB;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 31;
	}

	@Override
	public void onDamage(final Plugin plugin, final Player player, final double level, final DamageEvent event,
						 final LivingEntity target) {


		final DamageType type = event.getType(); //If the player is in view of mob, don't grant bonus, don't play sound.
		if (!AFFECTED_DAMAGE_TYPES.contains(type) || EntityUtils.isInFieldOfView(target, player)) {
			return;
		}
		final double mult;
		mult = 1 + DAMAGE_BONUS_PER_LEVEL * level;
		event.updateGearDamageWithMultiplier(mult, AFFECTED_DAMAGE_TYPES);
		target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1f, 1.35f);
		target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1f, 0.85f);
	}
}
