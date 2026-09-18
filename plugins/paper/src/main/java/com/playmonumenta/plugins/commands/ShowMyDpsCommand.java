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

public class ShowMyDpsCommand {
	private static final String TAG = "ShowMyDps";
	private static final NumberFormat FORMAT = NumberFormat.getCompactNumberInstance(Locale.getDefault(), NumberFormat.Style.SHORT);

	static {
		FORMAT.setMaximumFractionDigits(1);
	}

	private record DPS(double damage, long startTime, Map<ClassAbility, Double> perAbilityDamage) {
		public Component getMessage(Component bossName, long timeNow) {
			List<Component> abilities = new ArrayList<>();
			abilities.add(Component.text("Ability Damage", NamedTextColor.AQUA, TextDecoration.BOLD));

			for (Map.Entry<ClassAbility, Double> entry : perAbilityDamage.entrySet()) {
				TextComponent element = Component.text(entry.getKey().getName() + ": ", NamedTextColor.GRAY)
					.append(Component.text(FORMAT.format(entry.getValue()).toLowerCase(Locale.ROOT), NamedTextColor.WHITE));
				abilities.add(element);
			}

			// gets time to 0.1s
			double time = (timeNow - startTime) / 1000.0;
			return Component.empty()
				.append(Component.text("-".repeat(80), NamedTextColor.WHITE))
				.appendNewline()
				.append(Component.text("Damage Summary for ", NamedTextColor.GOLD)).append(bossName)
				.appendNewline()
				.append(Component.text("Total Damage: ", NamedTextColor.RED)
					.append(Component.text(FORMAT.format(damage).toLowerCase(Locale.ROOT), NamedTextColor.WHITE))
					.append(Component.text(" (%,.1f)".formatted(damage), NamedTextColor.GRAY))
					.hoverEvent(HoverEvent.showText(Component.join(JoinConfiguration.separator(Component.newline()), abilities))))
				.appendNewline()
				.append(Component.text("Damage per Second: ", NamedTextColor.DARK_RED)
					.append(Component.text(FORMAT.format(damage / time).toLowerCase(Locale.ROOT), NamedTextColor.WHITE))
					.append(Component.text(" (%,.1f)".formatted(damage / time), NamedTextColor.GRAY)))
				.appendNewline()
				.append(Component.text("-".repeat(80), NamedTextColor.WHITE));
		}
	}

	// Map of boss entities to their map of players to damage
	private static final Map<UUID, Map<UUID, DPS>> PLAYER_DPS_MAP = new HashMap<>();

	public static void register() {
		new CommandAPICommand("showmydps")
			.withPermission("monumenta.command.showmydps")
			.withSubcommand(new CommandAPICommand("enable")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage("Started logging your dps against bosses.");
						player.addScoreboardTag(TAG);
					}
				}))
			.withSubcommand(new CommandAPICommand("disable")
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						player.sendMessage("Stopped logging your dps against bosses.");
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
				@Nullable
				ClassAbility ability = event.getAbility();
				if (oldDps == null) {
					HashMap<ClassAbility, Double> perAbilityDamage = new HashMap<>();
					if (ability != null) {
						perAbilityDamage.put(ability, finalDamage);
					}
					return new DPS(finalDamage, System.currentTimeMillis(), perAbilityDamage);
				}

				Map<ClassAbility, Double> perAbilityDamage = oldDps.perAbilityDamage;
				if (ability != null) {
					perAbilityDamage.compute(ability, (classAbility, previousDamage) -> {
						if (previousDamage == null) {
							return finalDamage;
						}
						return previousDamage + finalDamage;
					});
				}
				return new DPS(finalDamage + oldDps.damage, oldDps.startTime, perAbilityDamage);
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
