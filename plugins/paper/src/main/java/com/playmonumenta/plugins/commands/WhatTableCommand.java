package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.listeners.LootTableManager;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import it.unimi.dsi.fastutil.Pair;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public class WhatTableCommand {
	private static final String COMMAND = "findtable";

	private static final String PERMISSION = "monumenta.commands.whattable";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withOptionalArguments(new GreedyStringArgument("item name"))
			.executesPlayer((sender, args) -> {
				final var name = args.getOptionalByClass("item name", String.class);
				final String query;
				final Component prettyQuery;

				if (name.isPresent()) {
					query = name.get();
					prettyQuery = Component.text(query);
				} else {
					final ItemStack item = sender.getInventory().getItemInMainHand();

					if (item.getType() == Material.AIR) {
						sender.sendMessage(Component.text("This command requires you to hold an item.", NamedTextColor.RED));
					}

					if (!ItemUtils.isInteresting(item)) {
						sender.sendMessage(Component.text("This item is not interesting enough.", NamedTextColor.RED));
						return;
					}

					if (MasterworkUtils.isMasterwork(item)) {
						sender.sendMessage(Component.text("Your ", NamedTextColor.GREEN)
							.append(item.displayName())
							.append(Component.text(" lives in multiple places: ", NamedTextColor.GREEN)));

						buildOutputMessage(
							MasterworkUtils.getAllMasterworks(item, sender)
								.stream()
								.map(MasterworkUtils::getItemPath)
								.toList()
						).forEach(sender::sendMessage);

						return;
					}

					prettyQuery = ItemUtils.getDisplayName(item);
					query = MessagingUtils.plainText(prettyQuery);
				}

				searchForItemByName(query).whenComplete((keys, throwable) -> {
					if (throwable != null) {
						MMLog.severe("WhatTableCommand: exception caught when searching loot tables");
						sender.sendMessage(Component.text("(!) Internal error.", NamedTextColor.RED));
					}

					if (keys.isEmpty()) {
						sender.sendMessage(Component.text("(!) Could not find item in the datapack.", NamedTextColor.RED));
						return;
					}

					sender.sendMessage(Component.empty()
						.append(Component.text("Your ", NamedTextColor.GREEN))
						.append(prettyQuery)
						.append(Component.text(" lives in ", NamedTextColor.GREEN))
						.append(Component.text(
							keys.size() > 1 ? "multiple locations: " : keys.getFirst().asString(),
							NamedTextColor.WHITE
						))
					);

					if (keys.size() == 1) {
						final var key = keys.getFirst();

						sender.sendMessage(
							Component.text("[Click here to copy the path to your clipboard]", NamedTextColor.GREEN)
								.clickEvent(ClickEvent.copyToClipboard(key.asString()))
						);
						sender.sendMessage(
							Component.text("[Click here to run the loot table]", NamedTextColor.GOLD)
								.clickEvent(ClickEvent.runCommand("/loot give @s loot " + key.asString()))
						);
					} else {
						buildOutputMessage(keys.stream().map(NamespacedKey::asString).toList()).forEach(sender::sendMessage);
					}
				});
			}).register();
	}

	private static List<TextComponent> buildOutputMessage(List<String> paths) {
		return paths.stream()
			.map(x -> Component.text(x)
				.append(Component.text(" [Copy] ", NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(x)))
				.append(Component.text("[Give]", NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/loot give @s loot " + x))))
			.toList();
	}

	private static CompletableFuture<List<NamespacedKey>> searchForItemByName(String name) {
		final var tables = LootTableManager.INSTANCE.getTables();

		// TODO: cache the results here
		return CompletableFuture.supplyAsync(
			() -> tables.stream()
				.map(x -> Pair.of(x, Bukkit.getLootTable(x)))
				.map(x -> Pair.of(x.first(), NmsUtils.getVersionAdapter().materializeItemLikeLootTable(x.second())))
				.flatMap(x -> x.second().stream().map(y -> Pair.of(x.first(), y)))
				.filter(x -> ItemUtils.getPlainName(x.second()).contains(name))
				.map(Pair::first)
				.toList()
		);
	}
}
