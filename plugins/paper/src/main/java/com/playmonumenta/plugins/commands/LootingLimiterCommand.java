package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ChestUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

public class LootingLimiterCommand {

	public static final String COMMAND = "antiLR";
	public static final String PERMISSION = "monumenta.antiLR";
	private static final String[] options = {"mobs", "spawners"};

	public static void register() {
		ArgumentSuggestions<CommandSender> optionsSuggestions = ArgumentSuggestions.strings(options);
		CommandPermission perms = CommandPermission.fromString(PERMISSION);

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("setBankedChests")
					.withArguments(new PlayerArgument("player"))
					.withArguments(new IntegerArgument("bankedChests"))
					.executes((sender, args) -> {
						Player player = args.getUnchecked("player");
						int bankedChests = args.getOrDefaultUnchecked("bankedChests", 0);
						if (player == null) {
							sender.sendMessage(Component.text("Cannot set anything of a player that doesn't exist!").color(NamedTextColor.RED));
						} else {
							Plugin.getInstance().mLootingLimiter.setBankedChests(player, bankedChests);
							sender.sendMessage("Set banked chests of player " + player + " to " + Math.clamp(0, bankedChests, ServerProperties.getLootingLimiterBankedChests()) + ".");
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("getBankedChests")
					.withArguments(new PlayerArgument("player"))
					.withArguments(new StringArgument("type").replaceSuggestions(optionsSuggestions))
					.executes((sender, args) -> {
						Player player = args.getUnchecked("player");
						if (player == null) {
							sender.sendMessage(Component.text("Cannot set anything of a player that doesn't exist!").color(NamedTextColor.RED));
						} else {
							String type = args.getUnchecked("type");
							if (type == null) {
								sender.sendMessage("Invalid type!");
							} else if (type.equals("mobs")) {
								int mobsKilled = Plugin.getInstance().mLootingLimiter.getMobsKilled(player);
								sender.sendMessage("This player has " + mobsKilled / ServerProperties.getLootingLimiterMobKills() + " banked mob chests and " + mobsKilled % ServerProperties.getLootingLimiterMobKills() + " mobs left over.");
							} else if (type.equals("spawners")) {
								int spawnersBroken = Plugin.getInstance().mLootingLimiter.getSpawnersBroken(player);
								sender.sendMessage("This player has " + spawnersBroken / ServerProperties.getLootingLimiterSpawners() + " banked spawner chests and " + spawnersBroken % ServerProperties.getLootingLimiterSpawners() + " spawners left over.");
							}
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("setNonLootLimitedChest")
					.withArguments(new LocationArgument("chestLoc"))
					.withOptionalArguments(new BooleanArgument("value"))
					.executes((sender, args) -> {
						Location chestLoc = args.getUnchecked("chestLoc");
						if (chestLoc == null) {
							sender.sendMessage("Invalid location!");
						} else {
							Block block = chestLoc.getBlock();
							if (block instanceof PersistentDataContainer) {
								boolean value = args.getOrDefaultUnchecked("value", true);
								ChestUtils.setNonLootLimitedChest(block, value);
								sender.sendMessage("This block will no" + (value ? "t" : "w") + " respect / contribute to antiLR.");
							} else {
								sender.sendMessage("This block is not a chest!");
							}
						}
					})
			)
			.register();
	}
}
