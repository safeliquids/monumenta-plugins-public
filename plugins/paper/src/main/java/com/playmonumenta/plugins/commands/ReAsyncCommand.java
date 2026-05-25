package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.executors.CommandExecutor;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.util.concurrent.Executor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class ReAsyncCommand {
	private static void execute(CommandSender sender, Executor async, Executor sync, boolean rescan) {
		sender.sendMessage(Component.text("Begin reload async..."));

		NmsUtils.getVersionAdapter().reloadAsync(
			ServerResourcesReloadedEvent.Cause.COMMAND,
			async,
			rescan
		).whenCompleteAsync((unused, throwable) -> {
			if (throwable != null) {
				MMLog.severe("Failed to execute reload", throwable);
				sender.sendMessage(Component.translatable("commands.reload.failure", NamedTextColor.RED));
			} else {
				sender.sendMessage(Component.text("Done reload async..."));
			}
		}, sync);
	}

	public static void register(Plugin plugin) {
		final Executor async = x -> Bukkit.getScheduler().runTaskAsynchronously(plugin, x);
		final Executor sync = x -> Bukkit.getScheduler().runTask(plugin, x);

		new CommandAPICommand("reasync")
			.withPermission(CommandPermission.OP)
			.executes((CommandExecutor) (sender, args) -> execute(sender, async, sync, false))
			.withSubcommand(
				new CommandAPICommand("rescan")
					.executes((CommandExecutor) (sender, args) -> execute(sender, async, sync, true))
			)
			.register();
	}
}
