package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.listeners.SpawnerListener;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class LucidityOverride extends BaseOverride {
	private static final String LUCIDITY_NAME = "Lucidity";
	private static final String QUEST_NAME = "Quest225";
	private static final String QUEST_REPLAY_TAG = "Q225Replay";
	private static final int MIN_SCORE = 70;

	private static final String COOLDOWN_SOURCE = "LucidityCooldown";
	private static final int COOLDOWN = Constants.TICKS_PER_MINUTE;
	private static final double RANGE = 20;
	private static final Color COLOR = Color.fromRGB(0x39B14E);
	private static final TextColor TEXT_COLOR = TextColor.color(COLOR.asRGB());
	private static final BlockData SPAWNER_BLOCK_DATA = Material.SPAWNER.createBlockData();

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block ignored) {
		if (!ItemUtils.getPlainName(item).equals(LUCIDITY_NAME)) {
			return true;
		}
		if (ScoreboardUtils.getScoreboardValue(player, QUEST_NAME).orElse(0) < MIN_SCORE && !player.getScoreboardTags().contains(QUEST_REPLAY_TAG)) {
			player.sendMessage(Component.text("You must have completed A Union Cross in order to use this item.", NamedTextColor.RED));
			return false;
		}
		if (plugin.mEffectManager.hasEffect(player, COOLDOWN_SOURCE)) {
			player.sendMessage(Component.text("Lucidity is still on cooldown.", NamedTextColor.RED));
			return false;
		}

		World world = player.getWorld();
		Location playerLocation = player.getLocation();
		if (!revealSpawners(playerLocation, world)) {
			player.sendMessage(Component.text("No spawners appear to be in the area...", NamedTextColor.RED));
			return false;
		}

		player.sendMessage(Component.text("Your vision becomes more aware of the dream...", TEXT_COLOR));
		player.playSound(playerLocation, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.75f, 0.75f, 1);
		plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(COOLDOWN, item, item.getType(), plugin));

		return false;
	}

	private static boolean revealSpawners(Location playerLocation, World world) {
		HashMap<Location, BlockDisplay> displays = new HashMap<>(40);
		for (Block block : BlockUtils.getBlocksInCube(playerLocation, RANGE)) {
			if (block.getState() instanceof CreatureSpawner spawner) {
				if (spawner.getRequiredPlayerRange() <= 0) {
					continue;
				}
				Location loc = block.getLocation();
				BlockDisplay blockDisplay = world.spawn(loc, BlockDisplay.class, display -> {
					display.setBlock(SPAWNER_BLOCK_DATA);
					display.setGlowColorOverride(COLOR);
					display.setGlowing(true);
				});
				displays.put(loc, blockDisplay);
			}
		}
		SpawnerListener.addLucidityDisplays(displays);

		return !displays.isEmpty();
	}

}
