package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.market.RedisItemDatabase;
import com.playmonumenta.plugins.utils.InventoryUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.inventory.ItemStack;

public class RedisItemDebugCommand extends GenericCommand {

	public static void register() {
		new CommandAPICommand("redisitemdebug")
			.withPermission("monumenta.command.redisitemdebug")
			.withSubcommand(
				new CommandAPICommand("get")
					.withArguments(new IntegerArgument("id"))
					.executesPlayer((player, args) -> {
						int id = args.getUnchecked("id");
						ItemStack item = RedisItemDatabase.getItemStackFromID(id);
						InventoryUtils.giveItem(player, item);
					})).register();
	}
}
