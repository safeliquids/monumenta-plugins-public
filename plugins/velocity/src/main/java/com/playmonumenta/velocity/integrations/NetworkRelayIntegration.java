package com.playmonumenta.velocity.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.GatherRemotePlayerDataEventVelocity;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEventGeneric;
import com.playmonumenta.networkrelay.util.MMLog;
import com.playmonumenta.velocity.MonumentaVelocity;
import com.playmonumenta.velocity.voting.VoteManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.SamplePlayer;
import com.velocitypowered.api.util.GameProfile;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class NetworkRelayIntegration {
	public static final String VOTE_NOTIFY_CHANNEL = "Monumenta.Bungee.VoteNotify";
	public static final String BAN_CHANNEL = "Monumenta.Velocity.BanOnLogout";
	private static final String BAN_LOGOUT_LISTEN = "logoutListen";
	private static final String BAN_LOGOUT_ALERT = "logoutAlert";

	private final MonumentaVelocity mMain;
	private final Logger mLogger;
	private static @MonotonicNonNull NetworkRelayIntegration INSTANCE = null;
	private static int mCachedPlayerCount = 0;
	private static String mCachedShardName = "";
	private static long mNextFetchedTime = System.currentTimeMillis();

	public NetworkRelayIntegration(MonumentaVelocity main) {
		mMain = main;
		main.mLogger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = main.mLogger;
		INSTANCE = this;
	}

	@Subscribe(priority = Short.MIN_VALUE / 2)
	public void proxyPingEvent(ProxyPingEvent event) {
		int count = getCachedPlayerCount();
		ServerPing oldPing = event.getPing();
		ServerPing.Version oldVersion = oldPing.getVersion();
		ServerPing newPing = oldPing.asBuilder()
			.onlinePlayers(count)
			.maximumPlayers(mMain.mConfig.mMaxPlayerCount)
			.version(new ServerPing.Version(oldVersion.getProtocol(), mMain.mConfig.mVersionString))
			.samplePlayers(new SamplePlayer("<" + getProxyName() + ">: " + mMain.mServer.getPlayerCount(), UUID.randomUUID()))
			.build();
		event.setPing(newPing);
	}

	public void gatherPlayerDataEvent(GatherRemotePlayerDataEventVelocity event) {
		@Nullable Player player = mMain.mServer.getPlayer(event.mRemotePlayer.getUuid()).orElse(null);
		if (player == null) {
			return;
		}

		// code to get skin properties, there is a possibility other data is here in the future - usb
		GameProfile.Property skin = player.getGameProfileProperties().stream().filter(p -> p.getName().equals("textures")).findFirst().orElse(null);
		JsonObject data = new JsonObject();
		data.addProperty("ping", Math.toIntExact(player.getPing()));
		if (skin != null) {
			data.addProperty("signed_texture", skin.getValue() + ";" + skin.getSignature());
		}
		event.setPluginData("monumenta-velocity", data);
	}

	@Subscribe(priority = Short.MAX_VALUE / 2)
	public void networkRelayMessageEventVelocity(NetworkRelayMessageEventGeneric event) {
		String channel = event.getChannel();
		if (channel.equals(VOTE_NOTIFY_CHANNEL)) {
			JsonObject data = event.getData();
			if (!data.has("playerUUID") ||
				!data.get("playerUUID").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("playerUUID").isString()) {
				mLogger.error("VOTE_NOTIFY_CHANNEL failed to parse required string field 'playerUUID'");
				return;
			}

			if (!data.has("matchingSite") ||
				!data.get("matchingSite").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("matchingSite").isString()) {
				mLogger.error("VOTE_NOTIFY_CHANNEL failed to parse required int field 'matchingSite'");
				return;
			}

			if (!data.has("cooldownMinutes") ||
				!data.get("cooldownMinutes").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("cooldownMinutes").isNumber()) {
				mLogger.error("VOTE_NOTIFY_CHANNEL failed to parse required int field 'cooldownMinutes'");
				return;
			}

			UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
			String matchingSite = data.get("matchingSite").getAsString();
			long cooldownMinutes = data.get("cooldownMinutes").getAsLong();

			VoteManager.gotVoteNotifyMessage(uuid, matchingSite, cooldownMinutes);
		} else if (channel.equals(BAN_CHANNEL)) {
			JsonObject data = event.getData();
			if (!data.has("type") ||
				!data.has("player")
			) {
				mLogger.error("BAN_CHANNEL gave an invalid message");
				return;
			}
			if (mMain.mBanOnLogout == null || mMain.mJoinLeaveHandler == null) {
				mLogger.error("mBanOnLogout or mJoinLeaveHandler was not initialised!");
				return;
			}

			String type = data.get("type").getAsString();
			String playerName = data.get("player").getAsString();
			if (type.equals(BAN_LOGOUT_LISTEN) && !mMain.mBanOnLogout.isTracked(playerName)) {
				// Failsafe: Don't send logouts to another proxy if the player is already going to be banned from this one
				mMain.mJoinLeaveHandler.listenPlayerLogout(playerName);
			} else if (type.equals(BAN_LOGOUT_ALERT) && !mMain.mJoinLeaveHandler.isTracked(playerName)) {
				// Failsafe: Don't ban on this proxy if another proxy already requested this player
				mMain.mBanOnLogout.onPlayerLogout(playerName);
			}
		}
	}

	public static void sendVoteNotifyPacket(UUID playerUUID, String matchingSite, long cooldownMinutes) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("matchingSite", matchingSite);
			data.addProperty("cooldownMinutes", cooldownMinutes);
			try {
				/* Send this message to whatever this shard is called (likely "bungee") */
				NetworkRelayAPI.sendBroadcastMessage(VOTE_NOTIFY_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.error("Failed to send vote notify message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static String getProxyName() {
		if (!mCachedShardName.isEmpty()) {
			return mCachedShardName;
		}
		if (INSTANCE != null) {
			try {
				mCachedShardName = NetworkRelayAPI.getShardName();
			} catch (Exception exception) {
				mCachedShardName = "unknown";
			}
			return mCachedShardName;
		}
		mCachedShardName = "unknown";
		return "No NetworkRelay integration";
	}

	public static int getCachedPlayerCount() {
		if (INSTANCE == null) {
			return mCachedPlayerCount;
		}
		long now = System.currentTimeMillis();
		if (mCachedPlayerCount != 0 && mNextFetchedTime >= now) {
			return mCachedPlayerCount;
		}
		mNextFetchedTime = now + 1000;
		try {
			mCachedPlayerCount = NetworkRelayAPI.getVisiblePlayerNames().size();
		} catch (Exception ex) {
			mCachedPlayerCount = 0;
			// ignored
		}
		return mCachedPlayerCount;
	}

	public static void setScore(String scoreHolder, String score, int value) {
		try {
			NetworkRelayAPI.sendBroadcastCommand("execute if entity %1$s run scoreboard players set %1$s %2$s %3$d".formatted(scoreHolder, score, value), NetworkRelayAPI.ServerType.MINECRAFT);
		} catch (Exception e) {
			MMLog.severe("Failed to set score \"%s\" to %d for %s".formatted(score, value, scoreHolder));
		}
	}

	public static void sendAdminMessage(String message) {
		JsonObject data = new JsonObject();
		data.addProperty("message", message);
		try {
			// See MonumentaNetworkRelayIntegration
			NetworkRelayAPI.sendMessage("*", "Monumenta.Automation.AdminNotification", data);
		} catch (Exception ex) {
			// TODO: pls use MMLog that paper can use
			INSTANCE.mLogger.error("Failed to send admin alert message", ex);
		}
	}

	public static void sendLogoutNotifyRequest(String playerName) {
		JsonObject data = new JsonObject();
		data.addProperty("type", BAN_LOGOUT_LISTEN);
		data.addProperty("player", playerName);
		try {
			// See MonumentaNetworkRelayIntegration
			NetworkRelayAPI.sendMessage("*", BAN_CHANNEL, data);
		} catch (Exception ex) {
			// TODO: pls use MMLog that paper can use
			INSTANCE.mLogger.error("Failed to send logout notify request", ex);
		}
	}

	public static void sendLogoutAlert(String playerName) {
		JsonObject data = new JsonObject();
		data.addProperty("type", BAN_LOGOUT_ALERT);
		data.addProperty("player", playerName);
		try {
			// See MonumentaNetworkRelayIntegration
			NetworkRelayAPI.sendMessage("*", BAN_CHANNEL, data);
		} catch (Exception ex) {
			// TODO: pls use MMLog that paper can use
			MMLog.severe("Failed to send logout notify request");
			MMLog.severe(String.valueOf(ex.getMessage()));
		}
	}
}
