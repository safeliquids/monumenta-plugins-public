package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PlayerPlacedBlocksCommand {

	public static final String COMMAND = "playerPlacedBlocks";
	public static final String PERMISSION = "monumenta.playerPlacedBlocks";

	public static void register() {
		ArgumentSuggestions<CommandSender> cachedWorldNameSuggestions = ArgumentSuggestions.strings((info) -> MonumentaWorldManagementAPI.getCachedAvailableWorlds());
		CommandPermission perms = CommandPermission.fromString(PERMISSION);

		Argument<String> cachedWorldNameArg = new StringArgument("worldName").replaceSuggestions(cachedWorldNameSuggestions);

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			// Methods that wipe locations
			.withSubcommand(
				new CommandAPICommand("clearShard")
					.executes((sender, args) -> {
						Plugin.getInstance().mPlacedBlocksListener.clearShard();
						sender.sendMessage("Removed all player-placed block markings. This goes across worlds!");
					})
			)
			.withSubcommand(
				new CommandAPICommand("clearWorld")
					.withArguments(cachedWorldNameArg)
					.executes((sender, args) -> {
						String worldName = args.getByArgument(cachedWorldNameArg);
						if (worldName == null) {
							sender.sendMessage(Component.text("Cannot clear locations of a world that doesn't exist!").color(NamedTextColor.RED));
						} else {
							World newWorld = Bukkit.getWorld(worldName);
							if (newWorld == null) {
								sender.sendMessage(Component.text("Cannot clear locations of a world that doesn't exist!").color(NamedTextColor.RED));
							} else {
								Plugin.getInstance().mPlacedBlocksListener.clearLoadedChunks(newWorld);
								sender.sendMessage("Removed all player-placed block markings in this world.");
							}
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("clearChunk")
					.withArguments(new LocationArgument("location"))
					.executes((sender, args) -> {
						Location location = args.getUnchecked("location");
						if (location == null) {
							sender.sendMessage(Component.text("Cannot clear a chunk using a location that doesn't exist!").color(NamedTextColor.RED));
						} else {
							Plugin.getInstance().mPlacedBlocksListener.clearChunk(location.getChunk());
							sender.sendMessage("Removed all player-placed block markings in this chunk.");
						}
					})
			)
			// Visualisation commands
			.withSubcommand(
				new CommandAPICommand("listLocationsInChunk")
					.withArguments(new LocationArgument("location"))
					.executes((sender, args) -> {
						Location location = args.getUnchecked("location");
						if (location == null) {
							sender.sendMessage(Component.text("Cannot show anything for a location that doesn't exist!").color(NamedTextColor.RED));
						} else {
							Collection<Vector> placedLocs = Plugin.getInstance().mPlacedBlocksListener.getPlayerPlacedBlocks(location.getChunk());
							if (placedLocs.isEmpty()) {
								sender.sendMessage(Component.text("Nothing!").color(NamedTextColor.GRAY));
							} else {
								for (Vector loc : placedLocs) {
									sender.sendMessage(Component.text("Location: " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()).color(NamedTextColor.GRAY));
								}
								sender.sendMessage(Component.text("Total " + placedLocs.size() + " locations in this chunk!"));
							}
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("showLocationsInChunkToPlayer")
					.withArguments(new LocationArgument("location"))
					.withArguments(new PlayerArgument("player"))
					.executes((sender, args) -> {
						Location location = args.getUnchecked("location");
						Player player = args.getUnchecked("player");
						if (location == null) {
							sender.sendMessage(Component.text("Cannot show anything for a location that doesn't exist!").color(NamedTextColor.RED));
						} else if (player == null) {
							sender.sendMessage(Component.text("Cannot send anything to a player that doesn't exist!").color(NamedTextColor.RED));
						} else {
							Plugin.getInstance().mPlacedBlocksListener.indicatePlayerPlacedBlocks(location.getChunk(), player);
							Collection<Vector> placedLocs = Plugin.getInstance().mPlacedBlocksListener.getPlayerPlacedBlocks(location.getChunk());
							sender.sendMessage(Component.text("Indicating total " + placedLocs.size() + " locations in this chunk!"));
						}
					})
			)
			.withSubcommand(
				new CommandAPICommand("countMarkedSubchunks")
					.executes((sender, args) -> {
						sender.sendMessage("Currently tracking " + Plugin.getInstance().mPlacedBlocksListener.loadedSubchunks() + " chunks!");
					})
			)
			.register();
	}
}
