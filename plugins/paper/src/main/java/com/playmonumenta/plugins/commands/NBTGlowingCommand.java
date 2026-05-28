package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Entity;

public class NBTGlowingCommand {
	public static void register() {
		new CommandAPICommand("nbtglowing")
			.withArguments(
				new EntitySelectorArgument.OneEntity("entity"),
				new BooleanArgument("glowing")
			)
			.executes((sender, args) -> {
				args.<Entity>getUnchecked("entity").setGlowing(args.getUnchecked("glowing"));
			})
			.register();
	}
}
