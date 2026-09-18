package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Blindness;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;


public final class Tactician implements Enchantment {
	private static final double GREATER_DAMAGE_BONUS_PER_LEVEL = 0.125;
	private static final double LESSER_DAMAGE_BONUS_PER_LEVEL = 0.075;
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeProjectileAndMagicTypes();

	@Override
	public String getName() {
		return "Tactician";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.TACTICIAN;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 30;
	}

	@Override
	public void onDamage(final Plugin plugin, final Player player, final double level, final DamageEvent event,
						 final LivingEntity target) {

		final DamageType type = event.getType();
		if (!AFFECTED_DAMAGE_TYPES.contains(type)) {
			return;
		}

		final double mult;
		final Effect rooted = plugin.mEffectManager.getActiveEffect(target, EntityUtils.SLOW_EFFECT_NAME);
		if (EntityUtils.isStunned(target)) {
			mult = 1 + GREATER_DAMAGE_BONUS_PER_LEVEL * level;
		} else if (EntityUtils.isParalyzed(plugin, target)
			|| EntityUtils.isFrozen(target)
			|| (rooted != null && rooted.getMagnitude() == 1)
			|| isBlinded(plugin, target)
			|| EntityUtils.isStaggered(target)) {
			mult = 1 + LESSER_DAMAGE_BONUS_PER_LEVEL * level;
		} else {
			return;
		}
		event.updateGearDamageWithMultiplier(mult, AFFECTED_DAMAGE_TYPES);
		if (type == DamageType.MELEE) {
			target.getWorld().playSound(target.getLocation(), Sound.BLOCK_NETHERITE_BLOCK_HIT, SoundCategory.PLAYERS, 0.8f, 0.55f);
		}
	}

	@Override
	public void onProjectileLaunch(final Plugin plugin, final Player player, final double value,
								   final ProjectileLaunchEvent event, final Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			final Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.BLOCK_NETHERITE_BLOCK_HIT, 1.5f, 0.8f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ITEM_TRIDENT_RETURN, 0.7f, 0.8f);
		}
	}

	//this should turn into a map in EntityUtils!
	public static boolean isBlinded(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, Blindness.class);
	}
}
