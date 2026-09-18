package com.playmonumenta.plugins.commands;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.PlayerSkinManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.scriptedquests.internal.com.google.api.client.http.HttpStatusCodes;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class PlayerSkinManagerCommand {

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.followRedirects(HttpClient.Redirect.NORMAL)
		.connectTimeout(Duration.ofSeconds(5))
		.build();
	private static final Gson GSON = new Gson();
	private static final String COMMAND = "playerskinmanager";

	private record IntermediateSkinData(String value, String signature) {

	}

	public static void register(Plugin plugin) {
		new CommandAPICommand(COMMAND)
			.withPermission("monumenta.command.playerskinmanager")
			.withSubcommand(new CommandAPICommand("forcereload")
				.executesPlayer((sender, args) -> {
					boolean success = plugin.mPlayerSkinManager.readFile();
					if (success) {
						sender.sendMessage(Component.text("PlayerSkinManager config reload was sucessful!", NamedTextColor.GREEN));
					} else {
						sender.sendMessage(Component.text("PlayerSkinManager config reload failed!", NamedTextColor.RED));
					}
				}))
			.register();

		if (ServerProperties.getSkinManagerWritable()) {
			new CommandAPICommand(COMMAND)
				.withSubcommand(new CommandAPICommand("add")
					.withArguments(new StringArgument("key"))
					.withArguments(new StringArgument("username"))
					.executesPlayer((sender, args) -> {
						String key = args.getUnchecked("key");
						String username = args.getUnchecked("username");
						if (key == null) {
							sender.sendMessage(Component.text("No key provided!", NamedTextColor.RED));
							return;
						} else if (PlayerSkinManager.textureMap.containsKey(key)) {
							sender.sendMessage(Component.text("Skin of key \"%s\" already exists!".formatted(key), NamedTextColor.RED));
							return;
						}
						if (username == null) {
							sender.sendMessage(Component.text("No username provided!", NamedTextColor.RED));
							return;
						}

						commonAddSkin(plugin, sender, args, username, key);
					}))
				.withSubcommand(new CommandAPICommand("update")
					.withArguments(new StringArgument("key"))
					.withArguments(new StringArgument("username"))
					.executesPlayer((sender, args) -> {
						String key = args.getUnchecked("key");
						String username = args.getUnchecked("username");
						if (key == null) {
							sender.sendMessage(Component.text("No key provided!", NamedTextColor.RED));
							return;
						} else if (!PlayerSkinManager.textureMap.containsKey(key)) {
							sender.sendMessage(Component.text("Skin of key \"%s\" does not exist!".formatted(key), NamedTextColor.RED));
							return;
						}
						if (username == null) {
							sender.sendMessage(Component.text("No username provided!", NamedTextColor.RED));
							return;
						}

						commonAddSkin(plugin, sender, args, username, key);
					}))
				.withSubcommand(new CommandAPICommand("delete")
					.withArguments(new StringArgument("key"))
					.executesPlayer((sender, args) -> {
						String key = args.getUnchecked("key");
						if (key == null) {
							sender.sendMessage(Component.text("No key provided!", NamedTextColor.RED));
							return;
						} else if (!PlayerSkinManager.textureMap.containsKey(key)) {
							sender.sendMessage(Component.text("Skin of key \"%s\" does not exist!".formatted(key), NamedTextColor.RED));
							return;
						}

						sender.sendMessage(Component.text("Are you sure you want to remove skin \"%s\" permanently?".formatted(key), NamedTextColor.RED));
						sender.sendMessage(Component.text("[REMOVE IT]", NamedTextColor.DARK_RED, TextDecoration.BOLD)
							.clickEvent(ClickEvent.runCommand("/playerskinmanager confirmremove %s".formatted(key)))
						);
					}))
				.withSubcommand(new CommandAPICommand("confirmremove")
					.withArguments(new StringArgument("key"))
					.executesPlayer((sender, args) -> {
						String key = args.getUnchecked("key");
						if (key == null) {
							sender.sendMessage(Component.text("No key provided!", NamedTextColor.RED));
							return;
						} else if (!PlayerSkinManager.textureMap.containsKey(key)) {
							sender.sendMessage(Component.text("Skin of key \"%s\" does not exist!".formatted(key), NamedTextColor.RED));
							return;
						}

						PlayerSkinManager.textureMap.remove(key);
						plugin.mPlayerSkinManager.updateFile();

						sender.sendMessage(Component.text("Skin \"%s\" has been removed.".formatted(key), NamedTextColor.YELLOW));
						sender.playSound(sender, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.6f);
					}))
				.register();
		}
	}

	private static void commonAddSkin(Plugin plugin, CommandSender sender, CommandArguments args, String username, String key) {
		if (Plugin.IS_PLAY_SERVER) {
			sender.sendMessage(Component.text("Cannot not send request on play server!"));
			return;
		}
		@Nullable
		UUID uuid = MonumentaRedisSyncAPI.cachedNameToUuid(username);
		if (uuid != null) {
			PlayerProfile playerProfile = Bukkit.createProfileExact(uuid, username);
			if (playerProfile.hasTextures()) {
				Set<ProfileProperty> properties = playerProfile.getProperties();

				for (ProfileProperty property : properties) {
					if (property.getName().equals("textures")) {
						updateTextureMap(plugin, key, property.getValue(), property.getSignature(), sender, username);
					}
				}
				return;
			}
		}
		// Send an actual async request as player is not in redis cache.
		sender.sendMessage(Component.text("Sending request...", NamedTextColor.YELLOW));

		if (uuid != null) {
			requestSkin(args, uuid.toString()).whenComplete((data, throwable) -> {
				if (throwable != null) {
					MMLog.severe("Error while sending request for %s".formatted(args.fullInput()), throwable);
					sender.sendMessage(Component.text(throwable.getMessage(), NamedTextColor.RED));
				}

				updateTextureMap(plugin, key, data.value, data.signature, sender, username);
			});
			return;
		}

		requestUUIDThenSkin(args, username).whenComplete((data, throwable) -> {
			if (throwable != null) {
				MMLog.severe("Error while sending request for %s".formatted(args.fullInput()), throwable);
				sender.sendMessage(Component.text(throwable.getMessage(), NamedTextColor.RED));
				return;
			}

			updateTextureMap(plugin, key, data.value, data.signature, sender, username);
		});
	}

	private static CompletableFuture<IntermediateSkinData> requestUUIDThenSkin(CommandArguments args, String username) {
		return HTTP_CLIENT.sendAsync(HttpRequest.newBuilder()
			.GET()
			.uri(URI.create("https://api.mojang.com/users/profiles/minecraft/").resolve(username))
			.build(), HttpResponse.BodyHandlers.ofString()
		).thenApply(stringHttpResponse -> {
			int statusCode = stringHttpResponse.statusCode();
			if (statusCode < HttpStatusCodes.STATUS_CODE_OK || statusCode > HttpStatusCodes.STATUS_CODE_ACCEPTED) {
				if (statusCode == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
					throw new IllegalStateException("Could not find player \"%s\"".formatted(username));
				}
				throw new IllegalStateException("Got bad status code %d while sending request for %s".formatted(statusCode, args.fullInput()));
			}

			return GSON.fromJson(stringHttpResponse.body(), JsonObject.class).get("id").getAsString();
		}).thenCompose(uuid -> requestSkin(args, uuid));
	}

	private static CompletableFuture<IntermediateSkinData> requestSkin(CommandArguments args, String uuid) {
		// Blame java for not adding an parameters argument to the builder
		return HTTP_CLIENT.sendAsync(HttpRequest.newBuilder()
			.GET()
			.uri(URI.create("http://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false".formatted(uuid)))
			.build(), HttpResponse.BodyHandlers.ofString()
		).thenApply(stringHttpResponse -> {
			int statusCode = stringHttpResponse.statusCode();
			if (statusCode < HttpStatusCodes.STATUS_CODE_OK || statusCode > HttpStatusCodes.STATUS_CODE_ACCEPTED) {
				throw new IllegalStateException("Got bad status code %d while sending request for %s".formatted(statusCode, args.fullInput()));
			}
			JsonArray properties = GSON.fromJson(stringHttpResponse.body(), JsonObject.class).get("properties").getAsJsonArray();
			for (JsonElement propertyElement : properties) {
				JsonObject property = propertyElement.getAsJsonObject();
				if (!property.get("name").getAsString().equals("textures")) {
					continue;
				}
				String base64Value = property.get("value").getAsString();
				String signature = property.get("signature").getAsString();
				return new IntermediateSkinData(base64Value, signature);
			}
			throw new IllegalStateException("Did not get a 'textures' property from mojang for %s.\n%s".formatted(args.fullInput(), stringHttpResponse.body()));
		});
	}

	private static void updateTextureMap(Plugin plugin, String key, String value, String signature, CommandSender sender, String username) {
		PlayerSkinManager.textureMap.put(key, new PlayerSkinManager.SkinData(key, key, value, signature));
		if (!plugin.mPlayerSkinManager.updateFile()) {
			sender.sendMessage(Component.text("Error while saving skins! See logs."));
		} else {
			sender.sendMessage(Component.text("Successfully added %s from the player %s.".formatted(key, username), NamedTextColor.GREEN));
		}
	}
}
