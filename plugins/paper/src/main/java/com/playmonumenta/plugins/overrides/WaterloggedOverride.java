package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class WaterloggedOverride extends BaseOverride {
	public static final Component WATERLOGGED_LORE = Component.text("Waterlogged", NamedTextColor.BLUE)
		.decoration(TextDecoration.ITALIC, false);
	public static final Component NOT_WATERLOGGED_LORE = Component.text("Not Waterlogged", NamedTextColor.BLUE)
		.decoration(TextDecoration.ITALIC, false);
	public static final List<Component> WATERLOG_RELATED_LORE = List.of(WATERLOGGED_LORE, NOT_WATERLOGGED_LORE);
	public static String BLOCK_STATE_TAG_KEY = "BlockStateTag";
	public static String WATERLOGGED_KEY = "waterlogged";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (item == null) {
			return true;
		}
		Material mat = item.getType();
		ItemMeta meta = item.getItemMeta();
		if (
			!(meta instanceof BlockDataMeta blockDataMeta) ||
			!blockDataMeta.hasBlockData() ||
			!(blockDataMeta.getBlockData(mat) instanceof Waterlogged waterloggable) ||
			!waterloggable.isWaterlogged()
		) {
			return true;
		}

		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL) {
			BlockState replacedBlock = event.getBlockReplacedState();
			if (ZoneUtils.isInPlot(replacedBlock.getLocation())) {
				return ZoneUtils.isInPlot(player);
			}
			return BlockUtils.isWaterSource(replacedBlock);
		}

		return false;
	}

	@Override
	public boolean inventoryClickEvent(Plugin plugin, Player player, ItemStack item, InventoryClickEvent event) {
		if (event.getClick() != ClickType.RIGHT) { // We only care about right clicks in this override
			return true;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta.hasLore()) {
			List<Component> lore = meta.lore();
			if (lore == null || lore.size() != 1 || !WATERLOG_RELATED_LORE.contains(lore.getFirst())) {
				return true;
			}
		}
		Material itemType = item.getType();
		BlockData defaultBlockData = itemType.createBlockData();
		if (!(defaultBlockData instanceof Waterlogged defaultWaterloggable)) {
			return true;
		}
		boolean defaultIsWaterlogged = defaultWaterloggable.isWaterlogged();

		ItemStack cursor = event.getCursor();
		Material cursorMat = cursor.getType();
		AtomicBoolean isDefault = new AtomicBoolean(false);
		if (Material.WATER_BUCKET.equals(cursorMat)) {
			meta.lore(null);
			item.setItemMeta(meta);
			// I know the BlockDataMeta can do this, but it also forces other block state changes that we don't want, like forcing lower slabs
			NBT.modify(item, nbt -> {
				ReadWriteNBT blockStateTag = nbt.getOrCreateCompound(BLOCK_STATE_TAG_KEY);
				if (defaultIsWaterlogged) {
					blockStateTag.removeKey(WATERLOGGED_KEY);
					if (blockStateTag.getKeys().isEmpty()) {
						nbt.removeKey(BLOCK_STATE_TAG_KEY);
					}
					isDefault.set(nbt.getKeys().isEmpty());
				} else {
					blockStateTag.setString(WATERLOGGED_KEY, "true");
				}
			});
			if (isDefault.get()) {
				item.setItemMeta(null);
			} else {
				meta = item.getItemMeta();
				meta.lore(List.of(WATERLOGGED_LORE));
				item.setItemMeta(meta);
			}
			// Copied from the Minecraft wiki, I know the sound sources/categories don't match
			player.playSound(Sound.sound(Key.key("minecraft:item.bucket.empty"), Sound.Source.BLOCK, 1.0f, 1.0f), player);
			event.setCurrentItem(item);
			return false;
		} else if (Material.BUCKET.equals(cursorMat)) {
			meta.lore(null);
			item.setItemMeta(meta);
			// I know the BlockDataMeta can do this, but it also forces other block state changes that we don't want, like forcing lower slabs
			NBT.modify(item, nbt -> {
				ReadWriteNBT blockStateTag = nbt.getOrCreateCompound(BLOCK_STATE_TAG_KEY);
				if (defaultIsWaterlogged) {
					blockStateTag.setString(WATERLOGGED_KEY, "false");
				} else {
					blockStateTag.removeKey(WATERLOGGED_KEY);
					if (blockStateTag.getKeys().isEmpty()) {
						nbt.removeKey(BLOCK_STATE_TAG_KEY);
					}
					isDefault.set(nbt.getKeys().isEmpty());
				}
			});
			if (isDefault.get()) {
				item.setItemMeta(null);
			} else {
				meta = item.getItemMeta();
				meta.lore(List.of(NOT_WATERLOGGED_LORE));
				item.setItemMeta(meta);
			}
			// Copied from the Minecraft wiki, I know the sound sources/categories don't match
			player.playSound(Sound.sound(Key.key("minecraft:item.bucket.fill"), Sound.Source.PLAYER, 1.0f, 1.0f), player);
			event.setCurrentItem(item);
			return false;
		}

		return true;
	}

	public static boolean mayPlaceWaterloggable(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta.hasLore()) {
			List<Component> lore = meta.lore();
			return lore == null || (lore.size() == 1 && WATERLOG_RELATED_LORE.contains(lore.getFirst()));
		}
		return true;
	}
}
