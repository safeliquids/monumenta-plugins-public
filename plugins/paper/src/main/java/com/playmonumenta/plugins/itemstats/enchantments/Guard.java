package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.attributes.Agility;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Guard implements Enchantment {
	public static final double HEALTH_RATIO = 0.35;
	public static final int PAST_HIT_DURATION_TIME_MAINHAND = 6 * 20;
	public static final int PAST_HIT_DURATION_TIME_OFFHAND = 4 * 20;
	public static final int PAST_HIT_DURATION_TIME_HEALTH = 2 * 20;
	private static final String GUARD_EFFECT_NAME = "GuardEffect";

	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.LATE_DEFENSE_SITUATIONAL; // make sure to capture all other situational enchantments first
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GUARD;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		double damageTaken = event.getFinalDamage(true);
		if (!event.getType().isDefendable()) {
			return;
		}
		double armor = plugin.mItemStatManager.getAttributeAmount(player, AttributeType.ARMOR);
		double agility = plugin.mItemStatManager.getAttributeAmount(player, AttributeType.AGILITY);

		// counts everything but guard as guard is 0 damage rn
		boolean adaptability = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADAPTABILITY) > 0;
		if (agility > 0 && armor <= 0) {
			damageTaken *= Armor.getDamageMultiplier(0, Armor.getSecondaryEnchantsMod(event, plugin, player),
				agility, Agility.getSecondaryEnchantsLevel(event, plugin, player),
				Armor.getSecondaryEnchantCap(player), Armor.getSecondaryEHPMultiplier(player), adaptability, 0, event.getType().getDefenseModifier());
		} else if (armor > 0) {
			damageTaken *= Armor.getDamageMultiplier(armor, Armor.getSecondaryEnchantsMod(event, plugin, player),
				agility, Agility.getSecondaryEnchantsLevel(event, plugin, player),
				Armor.getSecondaryEnchantCap(player), Armor.getSecondaryEHPMultiplier(player), adaptability, 0, event.getType().getDefenseModifier());
		}

		if (damageTaken / EntityUtils.getMaxHealth(player) >= HEALTH_RATIO) {
			addEffect(plugin, player, PAST_HIT_DURATION_TIME_HEALTH);
		}
	}

	@Override
	public void onDamageShielded(Plugin plugin, Player player, double value, DamageShieldedEvent event) {
		addEffect(plugin, player, player.getInventory().getItemInMainHand().getType() == Material.SHIELD ? PAST_HIT_DURATION_TIME_MAINHAND : PAST_HIT_DURATION_TIME_OFFHAND);
	}

	public static void addEffect(Plugin plugin, Player player, int duration) {
		plugin.mEffectManager.addEffect(player, GUARD_EFFECT_NAME, new OnHitTimerEffect(duration));
		new PartialParticle(Particle.REDSTONE, player.getLocation().add(0, 1, 0), 24, 0.4, 0.5, 0.4, new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f)).spawnAsPlayerBuff(player);
		player.sendActionBar(Component.text("Guard", NamedTextColor.RED));
	}

	public static double applyGuard(DamageEvent event, Plugin plugin, Player player) {
		NavigableSet<Effect> guard = plugin.mEffectManager.getEffects(player, GUARD_EFFECT_NAME);
		if (guard != null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.GUARD);
		}
		return 0;
	}

}
