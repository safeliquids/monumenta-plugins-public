package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.ItemCooldown;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LucidityOverride extends BaseOverride {
	private static final String LUCIDITY_NAME = "Lucidity";
	private static final String QUEST_NAME = "Quest225";
	private static final String QUEST_REPLAY_TAG = "Q225Replay";
	private static final int MIN_SCORE = 70;

	private static final String COOLDOWN_SOURCE = "LucidityCooldown";
	private static final int NO_SPAWNERS_COOLDOWN = 4 * Constants.TICKS_PER_SECOND;
	private static final int COOLDOWN = Constants.TICKS_PER_MINUTE;
	private static final double RANGE = 20;
	private static final Color COLOR = Color.fromRGB(0x39B14E);
	private static final TextColor TEXT_COLOR = TextColor.color(COLOR.asRGB());
	private static final BlockData DISPLAY_DATA = Material.TINTED_GLASS.createBlockData();

	private static final Map<Location, UUID> mLuciditySpawners = new HashMap<>();

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block ignored) {
		return tryLucidity(plugin, player, item);
	}

	@Override
	public boolean inventoryClickInteraction(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() == ClickType.RIGHT) {
			return tryLucidity(plugin, player, item);
		}
		return true;
	}

	private static boolean tryLucidity(Plugin plugin, Player player, ItemStack item) {
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
		// Prevent spamming Lucidity

		World world = player.getWorld();
		Location playerLocation = player.getLocation();
		if (!revealSpawners(playerLocation, world)) {
			player.sendMessage(Component.text("No spawners appear to be in the area...", NamedTextColor.RED));
			plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(NO_SPAWNERS_COOLDOWN, item, item.getType(), plugin));
			return false;
		}

		player.sendMessage(Component.text("Your vision becomes more aware of the dream...", TEXT_COLOR));
		player.playSound(playerLocation, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.75f, 0.75f, 1);
		plugin.mEffectManager.addEffect(player, COOLDOWN_SOURCE, new ItemCooldown(COOLDOWN, item, item.getType(), plugin));

		return false;
	}

	private static boolean revealSpawners(Location playerLocation, World world) {
		HashMap<Location, UUID> displays = new HashMap<>(40);
		for (Block block : BlockUtils.getBlocksInCube(playerLocation, RANGE)) {
			if (block.getState() instanceof CreatureSpawner spawner) {
				if (spawner.getRequiredPlayerRange() <= 0) {
					continue;
				}
				Location loc = block.getLocation();
				BlockDisplay blockDisplay = world.spawn(loc, BlockDisplay.class, display -> {
					display.setBlock(DISPLAY_DATA);
					display.setTransformation(new Transformation(
						new Vector3f(0.05f),
						new Quaternionf(),
						new Vector3f(0.9f),
						new Quaternionf()
					));
					display.setGlowColorOverride(COLOR);
					display.setGlowing(true);
				});
				displays.put(loc, blockDisplay.getUniqueId());
			}
		}
		displays.forEach((location, uuid) -> {
			@Nullable
			UUID existingDisplay = mLuciditySpawners.remove(location);
			if (existingDisplay != null) {
				@Nullable
				Entity displayEntity = Bukkit.getEntity(existingDisplay);
				if (displayEntity != null) {
					displayEntity.remove();
				}
			}

			mLuciditySpawners.put(location, uuid);
		});

		return !displays.isEmpty();
	}

	public static void removeDisplay(Block block) {
		@Nullable
		UUID remove = mLuciditySpawners.remove(block.getLocation());
		if (remove == null) {
			return;
		}
		@Nullable
		Entity displayEntity = Bukkit.getEntity(remove);
		if (displayEntity != null) {
			displayEntity.remove();
		}
	}

	// Runs if the server stops3
	public static void removeAllDisplays() {
		mLuciditySpawners.forEach((location, uuid) -> {
			@Nullable
			Entity displayEntity = Bukkit.getEntity(uuid);
			if (displayEntity != null) {
				displayEntity.remove();
			}
		});
		mLuciditySpawners.clear();
	}
}
