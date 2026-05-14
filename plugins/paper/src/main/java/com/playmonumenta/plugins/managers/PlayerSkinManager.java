package com.playmonumenta.plugins.managers;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.protocollib.CursedListener;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerSkinManager implements Listener {
	// villager2.png
	public static final SkinData FALLBACK_SKIN = new SkinData("fallback", "fallback", "ewogICJ0aW1lc3RhbXAiIDogMTU5MDE2NTA3MjI2MiwKICAicHJvZmlsZUlkIiA6ICJiNjJhM2Q2YmM0NGM0OGU4OTNjODViOTkxNzc1NWI2YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJQZXBlQ29tZVBhcGEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjg3OWUzNjYxYjUwZjIwMzE3ZmVhMTFhNGU3NzVjODBjMDU1OWMxMjQyZTY1NWY4MTY4MGUwOGE0ZWRlMzQzMiIKICAgIH0KICB9Cn0=", "uSOSe8xCAdZ1qRds1DY7AwLCUHsMRhdCIMglElGDACBBTvcZoKTw9HZDMXfa0bsfTK73vFIyyi/SZuItdopNOIA3zLSND+6aoaeeZiMJ2UjtAyh8Fo+rXtsHmUt9VkOY3ED/uE8fhumcNhcI1ahowBrCOCO2A5Gu/Ij3VP9y8XVl7d4P2qVBmeVpAvua+3IaCjSiiFPTWSbP56hfO/C+2yQcK03BPLmMvjSqH47V7UQ9LsbBC175UpxBRkQbZW9ZlRTAfR/1DTVyOOcBa0KIZn+V5tvLsJFkDIJXvcueEkIwFnBFj4CASTbpu7/gNqRl5C0owDhBz+zzak3gXT8/ufGTufFmriU3dBzJJ9b8nMc25UvSMWIMqqbaPVyg6kcwNE40OFkdKv2iRCHrjk3GQzENbG/lOcHtaO+0WM5iySe/6iXoQasgiSJtAuDphtCAri9ZKjKBnjEYQcUdg6DvPCkLezXD8HpDEMqIBiAU/odsrTzqhH++RBRAstP57ovEvJcMO2mabr4SphkHjWH7vctD1ByMohQknv9noB5Y7IRQlFEy3PHD/pUooJ3DWmcc3IslJDynws8pbepOb60f7Mq0wIJwyik09W+G9NtRjei9WLbHjdaeNIem75tqiLNAIPa2isxLvKQF4/ep3EWfqz6qCidcQ54D9RF0q3RhW5k=");
	public static ConcurrentHashMap<String, SkinData> textureMap = new ConcurrentHashMap<>();

	private final Path mFilePath;

	public record SkinData(String key, String name, String texture, String signature) {
		// sin for the bosstag parser
		@Override
		public @NotNull String toString() {
			return name;
		}
	}

	public record NpcData(String key, String name, Set<String> skins, Map<String, String> matches) {}

	public PlayerSkinManager(Plugin plugin) {
		mFilePath = plugin.getDataFolder().toPath().resolve("skins.json");

		readFile();

		long firstModMs;
		try {
			firstModMs = Files.getLastModifiedTime(mFilePath).toMillis();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		new BukkitRunnable() {
			private long mLastModMs = firstModMs;
			@Override
			public void run() {
				try {
					long modMs = Files.getLastModifiedTime(mFilePath).toMillis();
					if (modMs > mLastModMs) {
						readFile();
						mLastModMs = modMs;
					}
				} catch (IOException ex) {
					MMLog.severe("Caught exception while polling player skin file!", ex);
				}

			}
		}.runTaskTimer(plugin, 10 * Constants.TICKS_PER_SECOND, 10 * Constants.TICKS_PER_SECOND);
	}

	public static SkinData create(String key, JsonObject input) {
		String name = input.get("name").getAsString();
		String texture = input.get("texture").getAsString();
		String signature = input.get("signature").getAsString();
		return new SkinData(key, name, texture, signature);
	}

	public static JsonObject serialize(SkinData data) {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", data.name);
		obj.addProperty("texture", data.texture);
		obj.addProperty("signature", data.signature);
		return obj;
	}

	public boolean readFile() {
		JsonObject textures;
		try {
			JsonObject json = FileUtils.readJson(mFilePath.toString());
			textures = json.get("textures").getAsJsonObject();
		} catch (Exception ex) {
			MMLog.severe("Skins failed to load! :c " + ex);
			return false;
		}
		ConcurrentHashMap<String, SkinData> temp = new ConcurrentHashMap<>();
		for (Map.Entry<String, JsonElement> t : textures.entrySet()) {
			SkinData skinData = create(t.getKey(), t.getValue().getAsJsonObject());
			temp.put(skinData.name, skinData);
		}
		textureMap = temp;
		return true;
	}


	public boolean updateFile() {
		if (textureMap.isEmpty()) {
			MMLog.severe("Refusing to save empty player skin map!", new IllegalStateException("PlayerSkinManager textureMap should not be empty! "));
			return false;
		}
		JsonObject json;
		JsonObject newTextures;
		try {
			json = FileUtils.readJson(mFilePath.toString());
			newTextures = json.get("textures").getAsJsonObject();
		} catch (Exception ex) {
			MMLog.severe("Skins failed to load! :c " + ex);
			return false;
		}

		textureMap.forEach((key, skinData) -> {
			@Nullable
			JsonElement jsonElement = newTextures.get(key);
			JsonObject serialized = serialize(skinData);
			if (jsonElement == null) {
				newTextures.add(key, serialized);
				return;
			}
			JsonObject obj = jsonElement.getAsJsonObject();
			for (String objKey : serialized.keySet()) {
				obj.add(objKey, serialized.get(objKey));
			}
		});
		try {
			if (!Files.exists(mFilePath)) {
				Files.createDirectories(mFilePath.getParent());
			}

			try (var writer = Files.newBufferedWriter(mFilePath)) {
				GsonBuilder gsonBuilder = new GsonBuilder();
				gsonBuilder.disableHtmlEscaping();
				gsonBuilder.setPrettyPrinting();
				gsonBuilder.create().toJson(json, writer);
			}
		} catch (Exception ex) {
			MMLog.severe("Skins failed to save! :c " + ex);
			return false;
		}
		return true;
	}


	public static SkinData fetchSkin(Entity entity) {
		// TODO: check ScriptedQuests for their skin tag
		// or should I use PDC or a scoreboard tag to determine skin? so you can apply it to any mob...

		return fetchFallbackSkin();
	}

	public static SkinData fetchSkinByName(String name) {
		SkinData response = textureMap.get(name);
		return response != null ? response : fetchFallbackSkin();
	}

	// Calder's skin for now, should be swapped to a variety
	public static SkinData fetchFallbackSkin() {
		SkinData response = textureMap.get("villager174");
		return response != null ? response : FALLBACK_SKIN;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		Entity entity = event.getEntity();
		CursedListener.scheduleRemove(entity.getEntityId());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (!player.hasPermission(CursedListener.PERMISSON_STRING)) {
			return;
		}
		CursedListener.joinPlayer(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		CursedListener.leavePlayer(player.getUniqueId());
	}
}
