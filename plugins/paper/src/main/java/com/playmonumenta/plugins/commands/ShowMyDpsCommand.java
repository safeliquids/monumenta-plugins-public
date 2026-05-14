package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;

public class ShowMyDpsCommand {
	private static final String TAG = "ShowMyDps";
	private static final NumberFormat FORMAT = NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.SHORT);

	static {
		FORMAT.setMaximumFractionDigits(1);
	}

	private record DPS(double damage, Map<String, Double> perAbilityDamage,
	                   long startTime, long downTime, long lastHitTime) {
		public Component getMessage(Component bossName, long timeNow) {
			List<Component> abilities = new ArrayList<>();
			abilities.add(Component.text("Damage Breakdown", NamedTextColor.AQUA, TextDecoration.BOLD));

			for (Map.Entry<String, Double> entry : perAbilityDamage.entrySet()) {
				TextComponent element = Component.text(entry.getKey() + ": ", NamedTextColor.GRAY)
					.append(Component.text(FORMAT.format(entry.getValue()).toLowerCase(Locale.ROOT), NamedTextColor.WHITE));
				abilities.add(element);
			}

			long killTimeMillis = timeNow - startTime;
			double killTimeSeconds = Math.max(killTimeMillis / 50, 1) / 20.0;
			// For fancy display
			long minutes = (long) Math.floor(killTimeSeconds / 60);
			long seconds = Math.round(killTimeSeconds % 60);

			long dpsTimeMillis = killTimeMillis - downTime;
			double dpsTimeSeconds = (double) Math.max(dpsTimeMillis / 50, TICKS_PER_SECOND) / TICKS_PER_SECOND;

			return Component.empty()
				.append(Component.text("Damage Summary for ", NamedTextColor.GOLD)).append(bossName)
				.appendNewline()
				.append(Component.text("- Total Damage: ", NamedTextColor.RED)
					.append(Component.text(FORMAT.format(damage).toLowerCase(Locale.ROOT), NamedTextColor.WHITE))
					.append(Component.text(" (%,.1f)".formatted(damage), NamedTextColor.GRAY)))
				.appendNewline()
				.append(Component.text("- Damage per Second: ", NamedTextColor.DARK_RED)
					.append(Component.text(FORMAT.format(damage / dpsTimeSeconds).toLowerCase(Locale.ROOT), NamedTextColor.WHITE))
					.append(Component.text(" (%,.1f)".formatted(damage / dpsTimeSeconds), NamedTextColor.GRAY)))
				.appendNewline()
				.append(Component.text("- Kill Time: ", NamedTextColor.YELLOW)
					.append(Component.text("%d min, %d s".formatted(minutes, seconds), NamedTextColor.WHITE))
					.append(Component.text(" (%,.2fs)".formatted(killTimeSeconds), NamedTextColor.GRAY)))
				.hoverEvent(HoverEvent.showText(Component.join(JoinConfiguration.separator(Component.newline()), abilities)));
		}
	}

	// Map of boss entities to their map of players to damage
	private static final Map<UUID, Map<UUID, DPS>> PLAYER_DPS_MAP = new HashMap<>();

	public static void register() {
		new CommandAPICommand("showmydps")
			.withPermission("monumenta.command.showmydps")
			.executes((sender, args) -> {
				if (sender instanceof Player player) {
					if (player.getScoreboardTags().contains(TAG)) {
						player.sendMessage(
							Component.text("Boss DPS Logging: ", NamedTextColor.GOLD)
								.append(Component.text("Disabled", NamedTextColor.AQUA))
						);
						player.removeScoreboardTag(TAG);
					} else {
						player.sendMessage(
							Component.text("Boss DPS Logging: ", NamedTextColor.GOLD)
								.append(Component.text("Enabled", NamedTextColor.AQUA))
						);
						player.addScoreboardTag(TAG);
					}
				}
			})
			.withSubcommand(new CommandAPICommand("enable")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage(
							Component.text("Boss DPS Logging: ", NamedTextColor.GOLD)
								.append(Component.text("Enabled", NamedTextColor.AQUA))
						);
						player.addScoreboardTag(TAG);
					}
				}))
			.withSubcommand(new CommandAPICommand("disable")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage(
							Component.text("Boss DPS Logging: ", NamedTextColor.GOLD)
								.append(Component.text("Disabled", NamedTextColor.AQUA))
						);
						player.removeScoreboardTag(TAG);
					}
				}))
			.register();
	}

	public static void onDamage(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		if (EntityUtils.isBoss(damagee) && event.getSource() instanceof Player player) {
			Map<UUID, DPS> playerDPS = PLAYER_DPS_MAP.computeIfAbsent(damagee.getUniqueId(), uuid -> new HashMap<>());
			playerDPS.compute(player.getUniqueId(), (uuid, oldDps) -> {
				double finalDamage = event.getFinalDamage(true);
				// Likely is a taunt skill
				if (finalDamage < 0.1) {
					return oldDps;
				}

				@Nullable
				ClassAbility ability = event.getAbility();
				String abilityOrType = ability == null ? event.getType().getDisplay() : ability.getName();
				long currentTime = System.currentTimeMillis();

				if (oldDps == null) {
					HashMap<String, Double> perAbilityDamage = new HashMap<>();
					perAbilityDamage.put(abilityOrType, finalDamage);
					return new DPS(finalDamage, perAbilityDamage, currentTime, 0, currentTime);
				}

				Map<String, Double> perAbilityDamage = oldDps.perAbilityDamage;
				perAbilityDamage.compute(abilityOrType, (string, previousDamage) -> {
					if (previousDamage == null) {
						return finalDamage;
					}
					return previousDamage + finalDamage;
				});
				long interval = currentTime - oldDps.lastHitTime;
				// Record downtime exceeding 1 second
				long newDownTime = oldDps.downTime + Math.max(interval - 1000, 0);
				return new DPS(finalDamage + oldDps.damage, perAbilityDamage, oldDps.startTime, newDownTime, currentTime);
			});
		}
	}

	public static void onUnload(LivingEntity living) {
		PLAYER_DPS_MAP.remove(living.getUniqueId());
	}

	public static void onDeath(LivingEntity living) {
		@Nullable
		Map<UUID, DPS> playerDPS = PLAYER_DPS_MAP.remove(living.getUniqueId());
		if (playerDPS == null) {
			return;
		}
		playerDPS.forEach((uuid, dps) -> {
			@Nullable
			Player player = Bukkit.getPlayer(uuid);
			if (player == null || !player.getScoreboardTags().contains(TAG)) {
				return;
			}
			player.sendMessage(dps.getMessage(living.name(), System.currentTimeMillis()));
		});
	}
}
