package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Locale;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class MMLogCommand {
	public static void register() {
		new CommandAPICommand("mmlog")
			.withPermission("monumenta.command.mmlog")
			.withArguments(
				new MultiLiteralArgument("level",
					"trace", "debug", "info", "warning", "severe"),
				new GreedyStringArgument("message"))
			.executes((sender, args) -> {
				String level = Objects.requireNonNull(args.getUnchecked("level"));
				log(level, args.getUnchecked("message"), sender);
			}).register();
	}

	private static void log(String level, String message, CommandSender sender) {
		switch (level) {
			case "trace" -> MMLog.trace(message);
			case "debug" -> MMLog.debug(message);
			case "info" -> MMLog.info(message);
			case "warning" -> MMLog.warning(message);
			case "severe" -> MMLog.severe(message,
				new Throwable("Severe error thrown from /mmlog: " + message));
			default -> {
				// this will never happen
			}
		}
		sender.sendMessage(
			Component.text("Sent log at level %s: ".formatted(level.toUpperCase(Locale.ROOT)), NamedTextColor.GOLD)
			.append(Component.text(message, NamedTextColor.WHITE))
		);
	}
}
