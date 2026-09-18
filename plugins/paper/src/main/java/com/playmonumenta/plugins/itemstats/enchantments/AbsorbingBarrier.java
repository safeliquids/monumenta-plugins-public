package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class AbsorbingBarrier implements Enchantment {
	private static final int HIT_DURATION_REQUIREMENT = Constants.TICKS_PER_SECOND * 4;
	private static final int BARRIER_FULL_RECHARGE_TIME = 3;
	private static final double HEALTH_REQ = 0.75;
	private static final double ABSORPTION_PERCENTAGE_PER_LEVEL = 0.1;

	private static final HashMap<UUID, Integer> BARRIER_TIME_MAP = new HashMap<>();
	private static final HashMap<UUID, Double> BARRIER_AMOUNT_MAP = new HashMap<>();

	@Override
	public String getName() {
		return "Absorbing Barrier";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ABSORBING_BARRIER;
	}

	@Override
	public void onEquipmentUpdate(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ABSORBING_BARRIER);

		if (level <= 0) {
			Integer time = BARRIER_TIME_MAP.remove(player.getUniqueId());
			if (time != null) {
				Double amount = BARRIER_AMOUNT_MAP.remove(player.getUniqueId());
				AbsorptionUtils.subtractAbsorption(player, amount != null ? amount : 0);
			}
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// Debuffs/Environmental hazards count as a hit
		if (event.isBlocked()) {
			return;
		}

		// Ensure hit only occurs once a tick
		UUID uuid = player.getUniqueId();
		if (MetadataUtils.checkOnceThisTick(plugin, player, "AbsorbingBarrierHit")) {
			int tick = BARRIER_TIME_MAP.getOrDefault(uuid, 0);
			boolean isAboveReq = player.getHealth() >= EntityUtils.getMaxHealth(player) * HEALTH_REQ;

			if (tick >= HIT_DURATION_REQUIREMENT && isAboveReq) { // Audio when taking damage with barrier up
				player.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_HURT, 1f, 1.4f);
			}

			BARRIER_TIME_MAP.put(uuid, 0);
		}

		double absorpDamage = event.getFinalDamage(false) - event.getFinalDamage(true);
		BARRIER_AMOUNT_MAP.put(uuid, Math.max(0, BARRIER_AMOUNT_MAP.getOrDefault(uuid, 0.0) - absorpDamage));
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (oneHertz && player.getGameMode() != GameMode.SPECTATOR) {
			UUID uuid = player.getUniqueId();

			int tick = BARRIER_TIME_MAP.merge(uuid, Constants.TICKS_PER_SECOND, Integer::sum);
			double maxHealth = EntityUtils.getMaxHealth(player);

			// Restart timer if lower than the health requirement
			if (player.getHealth() <= maxHealth * HEALTH_REQ) {
				BARRIER_TIME_MAP.put(uuid, 0);
				return;
			}

			if (tick >= HIT_DURATION_REQUIREMENT) {
				double maxAbsorption = maxHealth * level * ABSORPTION_PERCENTAGE_PER_LEVEL;
				double partAmount = maxAbsorption / BARRIER_FULL_RECHARGE_TIME;

				if (AbsorptionUtils.getAbsorption(player) < maxAbsorption - 0.05) {
					player.playSound(player.getLocation(), Sound.ENTITY_GUARDIAN_AMBIENT, 1f, 2f);

					// Need to track absorption so that it can be removed when attempted hot-swapping

					BARRIER_AMOUNT_MAP.put(player.getUniqueId(),
						Math.min(maxAbsorption, BARRIER_AMOUNT_MAP.getOrDefault(player.getUniqueId(), 0.0) + partAmount));
				}

				AbsorptionUtils.addAbsorption(player, partAmount, maxAbsorption, Constants.TICKS_PER_SECOND * 25);
			}
		}
	}
}
