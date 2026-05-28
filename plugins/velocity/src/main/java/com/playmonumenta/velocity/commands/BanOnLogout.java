package com.playmonumenta.velocity.commands;

import com.playmonumenta.velocity.handlers.JoinLeaveHandler;
import com.playmonumenta.velocity.integrations.NetworkRelayIntegration;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

/**
 * Bans a player on disconnecting from the server with arguments specified as
 * {@code banonlogout <player> [time spec] [reason] }.
 */
public class BanOnLogout implements SimpleCommand {
	private record BanInfo(CommandSource source, String banArgs) {
	}

	private static final Map<String, BanInfo> mBanMap = new ConcurrentHashMap<>();
	private final ProxyServer mServer;
	private final JoinLeaveHandler mJoinLeaveHandler;
	private final CommandManager mCommandManager;

	public BanOnLogout(ProxyServer server, JoinLeaveHandler joinLeaveHandler) {
		mServer = server;
		mJoinLeaveHandler = joinLeaveHandler;
		mCommandManager = server.getCommandManager();
	}

	@Override
	public void execute(Invocation invocation) {
		// Don't let the console run this command to prevent rogue mods from banning TM
		if (!(invocation.source() instanceof Player player)) {
			return;
		}
		// Bans should always have at least a reason, but I'm not gonna parse the timespec here!
		String[] arguments = invocation.arguments();
		if (arguments.length < 2) {
			player.sendMessage(Component.text("Usage: /banonlogout <player> [time spec] [reason]", NamedTextColor.RED));
			player.sendMessage(Component.text("Example: /banonlogout Player 7d [reason]", NamedTextColor.RED));
			return;
		}

		String playerName = arguments[0];
		// Don't ban on this proxy if another proxy already requested this player
		if (mJoinLeaveHandler.isTracked(playerName)) {
			player.sendMessage(Component.text("Player %s already will be banned on logout on another proxy!".formatted(playerName), NamedTextColor.RED));
			return;
		}
		String banArgs = String.join(" ", arguments);
		Optional<Player> optionalPlayer = mServer.getPlayer(playerName);

		NetworkRelayIntegration.setScore(playerName, "Sus", 1);
		mBanMap.put(playerName, new BanInfo(invocation.source(), banArgs));
		if (optionalPlayer.isEmpty()) {
			// If the player is not on this proxy, instead ask the other proxies to track them
			NetworkRelayIntegration.sendLogoutNotifyRequest(playerName);
		}

		player.sendMessage(Component.text("Player %s will be banned on logout by: /ban %s".formatted(playerName, banArgs), NamedTextColor.YELLOW));
	}

	@Override
	public List<String> suggest(Invocation invocation) {
		String[] arguments = invocation.arguments();
		List<String> suggestions = new ArrayList<>();
		String currentPlayerName;
		if (arguments.length == 0) {
			currentPlayerName = "";
		} else if (arguments.length == 1) {
			currentPlayerName = arguments[0];
		} else {
			return List.of();
		}

		for (Player player : mServer.getAllPlayers()) {
			String username = player.getUsername();
			if (username.startsWith(currentPlayerName)) {
				suggestions.add(username);
			}
		}
		return suggestions;
	}

	@Override
	public boolean hasPermission(Invocation invocation) {
		return invocation.source().hasPermission("litebans.ban");
	}

	public void onPlayerLogout(String playerName) {
		@Nullable
		BanInfo banInfo = mBanMap.remove(playerName);
		if (banInfo == null) {
			return;
		}

		mCommandManager.executeAsync(banInfo.source, "ban " + banInfo.banArgs).whenComplete((result, throwable) -> {
			if (throwable == null) {
				return;
			}
			String msg = "Failed to ban uuid %s on logout as the ban command was invalid: %s".formatted(playerName, throwable.getMessage());
			NetworkRelayIntegration.sendAdminMessage(msg);
		});
	}

	public boolean isTracked(String playerName) {
		return mBanMap.containsKey(playerName);
	}
}
