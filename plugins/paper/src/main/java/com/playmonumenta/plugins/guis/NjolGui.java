package com.playmonumenta.plugins.guis;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A helper class to make simple Minecraft item GUIs.
 * Extend this class and define all items in the overridden {@link #setup()} method, then just {@link #open()} it to show it to a player.
 */
public abstract class NjolGui {

	private static final Map<UUID, NjolGui> LAST_OPENED_INVENTORY = new HashMap<>();

	protected final Plugin mPlugin;
	public final Player mPlayer;
	protected int mSize;
	private Component mTitle;
	private final boolean mCloseOnTeleport;
	private boolean mTitleDirty = false;
	private GuiCustomInventory mCustomInventory;

	private final List<NjolGuiItem> mItems;

	public ItemStack mFiller = GUIUtils.FILLER;

	public NjolGui(Player player, int size, String title) {
		this(player, size, Component.text(title));
	}

	public NjolGui(Player player, int size, Component title) {
		this(player, size, title, false);
	}

	public NjolGui(Player player, int size, Component title, boolean closeOnTeleport) {
		mPlugin = Plugin.getInstance();
		mPlayer = player;
		mSize = size;
		mTitle = title;
		mCloseOnTeleport = closeOnTeleport;
		mCustomInventory = new GuiCustomInventory(size, title);
		mItems = new ArrayList<>(size);
	}

	/**
	 * Updates the GUI - will clear all items and call {@link #setup()} again to fill it anew.
	 * Will also update the title and GUI size if they have been changed.
	 */
	public void update() {
		mCustomInventory.getInventory().clear();
		mItems.clear();

		setup();

		List<HumanEntity> oldViewers = Collections.emptyList();
		if (mSize != mCustomInventory.getInventory().getSize()
			|| mTitleDirty
			|| mCustomInventory.mDiscarded) {
			oldViewers = new ArrayList<>(mCustomInventory.getInventory().getViewers());
			mCustomInventory.discard();
			mCustomInventory = new GuiCustomInventory(mSize, mTitle);
			mTitleDirty = false;
		}
		Inventory inventory = mCustomInventory.getInventory();
		for (int i = 0; i < mSize; i++) {
			NjolGuiItem guiItem = i < mItems.size() ? mItems.get(i) : null;
			if (guiItem != null) {
				inventory.setItem(i, guiItem.mItem);
			} else {
				inventory.setItem(i, mFiller);
			}
		}
		for (HumanEntity human : oldViewers) {
			if (human instanceof Player player) {
				mCustomInventory.openInventory(player, mPlugin);
			}
		}
	}

	/**
	 * Shows this GUI to the player. Can safely be called multiple times.
	 */
	public void open() {
		update();
		LAST_OPENED_INVENTORY.put(mPlayer.getUniqueId(), this);
		mCustomInventory.openInventory(mPlayer, mPlugin);
	}

	/**
	 * Define GUI items in this method using the various {@link #setItem(int, int, ItemStack) setItem} methods.
	 */
	protected abstract void setup();

	/**
	 * Closes this GUI. Note that this will call {@link #onClose(InventoryCloseEvent) onClose}.
	 */
	public void close() {
		mCustomInventory.close();
	}

	/**
	 * Changes the size of this GUI. Can be called at any time from within {@link #setup()}. If called from anywhere else, needs an {@link #update()} call to become effective.
	 */
	public void setSize(int newSize) {
		if (newSize < 9 || newSize > 6 * 9 || newSize % 9 != 0) {
			throw new IllegalArgumentException("Invalid GUI size " + newSize);
		}
		mSize = newSize;
		if (mItems.size() > mSize) {
			MMLog.warning("Resizing a GUI to be smaller than its contents! (num items=" + mItems.size() + ", new GUI size=" + mSize + ", GUI class=" + getClass() + ")");
		}
	}

	/**
	 * Changes the title of this GUI. Can be called at any time from within {@link #setup()}. If called from anywhere else, needs an {@link #update()} call to become effective.
	 */
	public void setTitle(Component newTitle) {
		if (mTitle.equals(newTitle)) {
			return;
		}
		mTitle = newTitle;
		mTitleDirty = true;
	}

	public NjolGuiItem setItem(int row, int column, NjolGuiItem item) {
		return setItem(row * 9 + column, item);
	}

	public NjolGuiItem setItem(int index, NjolGuiItem item) {
		if (index < 0 || index >= 6 * 9) {
			throw new IllegalArgumentException("Invalid item index " + index);
		}
		if (index >= mSize) {
			MMLog.warning("Invalid item index " + index + " for inventory of size " + mSize + " (GUI class=" + getClass() + ")");
			return item;
		}
		while (mItems.size() <= index) {
			mItems.add(null);
		}
		mItems.set(index, item);
		return item;
	}

	public NjolGuiItem setItem(int row, int column, ItemStack item) {
		return setItem(row, column, new NjolGuiItem(item));
	}

	public NjolGuiItem setItem(int index, ItemStack item) {
		return setItem(index, new NjolGuiItem(item));
	}

	public @Nullable NjolGuiItem getItem(int index) {
		if (mItems.size() <= index) {
			return null;
		}
		return mItems.get(index);
	}

	public Inventory getInventory() {
		return mCustomInventory.getInventory();
	}

	public boolean getCloseOnTeleport() {
		return mCloseOnTeleport;
	}

	/**
	 * Called when the player clicks in the GUI area. Only use this if {@link NjolGuiItem#onClick(Consumer)} is not sufficient for your use case.
	 * If this returns false, no item's onClick handler will be called.
	 */
	protected boolean onGuiClick(InventoryClickEvent event) {
		return true;
	}

	/**
	 * Called when the player clicks in the player inventory area. Useful to perform actions on player items.
	 */
	protected void onPlayerInventoryClick(InventoryClickEvent event) {

	}

	protected void onOutsideInventoryClick(InventoryClickEvent event) {

	}

	protected void onInventoryDrag(InventoryDragEvent event) {

	}

	/**
	 * Called when this GUI is closed, whether by the player, {@link #close()}, or another inventory or GUI being shown to the player,
	 * or other reasons (check the {@link InventoryCloseEvent#getReason() event reason} for details).
	 */
	protected void onClose(InventoryCloseEvent event) {

	}

	private class GuiCustomInventory extends CustomInventory {
		private boolean mDiscarded = false;

		private enum RateLimitState {
			NONE(false, false),
			SEND_MESSAGE(true, true),
			FULL(true, false);

			private final boolean mBlockInteraction;
			private final boolean mSendMessage;

			RateLimitState(boolean mBlockInteraction, boolean mSendMessage) {
				this.mBlockInteraction = mBlockInteraction;
				this.mSendMessage = mSendMessage;
			}

			boolean sendMessage() {
				return mSendMessage;
			}

			boolean blockInteraction() {
				return mBlockInteraction;
			}

			RateLimitState next() {
				return switch (this) {
					case NONE -> SEND_MESSAGE;
					case SEND_MESSAGE, FULL -> FULL;
				};
			}
		}

		private int mRateLimitResetTicks = 1;
		private RateLimitState mRateLimit = RateLimitState.NONE;
		private int mRateLimitTick;

		public GuiCustomInventory(int size, Component title) {
			super(mPlayer, size, title);
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			event.setCancelled(true);

			if (checkRateLimit()) {
				return;
			}

			GUIUtils.refreshOffhand(event);
			if (mDiscarded) {
				MMLog.warning("GuiCustomInventory received click event after being discarded (GUI class=" + NjolGui.this.getClass() + ")");
				return;
			}
			if (event.getClickedInventory() == mInventory) {
				if (!onGuiClick(event)) {
					return;
				}
				if (event.getSlot() < mItems.size()) {
					NjolGuiItem item = mItems.get(event.getSlot());
					if (item != null) {
						item.clicked(event);
					}
				}
			} else if (event.getClickedInventory() != null) {
				onPlayerInventoryClick(event);
			} else {
				onOutsideInventoryClick(event);
			}
		}

		@Override
		protected void inventoryDrag(InventoryDragEvent event) {
			event.setCancelled(true);
			if (checkRateLimit()) {
				return;
			}
			onInventoryDrag(event);
		}

		@Override
		protected void inventoryClose(InventoryCloseEvent event) {
			if (!mDiscarded) {
				onClose(event);
				LAST_OPENED_INVENTORY.remove(mPlayer.getUniqueId());
				mDiscarded = true;
			}
		}

		public void discard() {
			mDiscarded = true;
		}

		private boolean checkRateLimit() {
			int currentTick = Bukkit.getCurrentTick();

			if (currentTick - mRateLimitTick > mRateLimitResetTicks) {
				mRateLimit = RateLimitState.NONE;
				mRateLimitTick = currentTick;
			}

			if (mRateLimit.sendMessage()) {
				MessagingUtils.sendError(mPlayer, "Please do not spam the GUI!");
			}

			boolean blocksInteraction = mRateLimit.blockInteraction();
			mRateLimit = mRateLimit.next();
			return blocksInteraction;
		}

		public void setRateLimit(int ticks) {
			Preconditions.checkArgument(ticks > 0, "Rate limit must be at least one tick!");
			mRateLimitResetTicks = ticks;
		}
	}

	public static @Nullable NjolGui getOpenGui(Player player) {
		NjolGui lastOpenedGui = LAST_OPENED_INVENTORY.get(player.getUniqueId());
		if (lastOpenedGui != null && lastOpenedGui.mCustomInventory.getInventory().equals(player.getOpenInventory().getTopInventory())) {
			return lastOpenedGui;
		}
		return null;
	}

	public static void playerQuit(Player player) {
		LAST_OPENED_INVENTORY.remove(player.getUniqueId());
	}

}
