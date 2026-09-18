package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.DungeonCommandMapping;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DungeonsGui extends Gui {

	private static final ItemStack NO_INSTANCE_ITEM = GUIUtils.createBasicItem(
		Material.RED_STAINED_GLASS_PANE,
		"No Instance Open",
		NamedTextColor.RED,
		true,
		""
	);

	private static final ItemStack OLD_INSTANCE_ITEM = GUIUtils.createBasicItem(
		Material.YELLOW_STAINED_GLASS_PANE,
		"Old Instance",
		NamedTextColor.YELLOW,
		true,
		""
	);

	private static final ItemStack OPEN_INSTANCE_ITEM = GUIUtils.createBasicItem(
		Material.GREEN_STAINED_GLASS_PANE,
		"Open Instance",
		NamedTextColor.DARK_GREEN,
		true,
		""
	);

	private static final ItemStack NEW_INSTANCE_ITEM = GUIUtils.createBasicItem(
		Material.LIME_STAINED_GLASS_PANE,
		"New Instance",
		NamedTextColor.GREEN,
		true,
		""
	);

	private int mPage;

	public enum TrackedDungeons {
		// R1:
		LABS(DungeonCommandMapping.LABS, "Alchemy Labs", "OldLab", "monumenta:dungeons/labs/find", Material.GLASS_BOTTLE, 1),
		WHITE(DungeonCommandMapping.WHITE, "White", "White", "monumenta:dungeons/white/find", Material.WHITE_WOOL, 1),
		ORANGE(DungeonCommandMapping.ORANGE, "Orange", "Orange", "monumenta:dungeons/orange/find", Material.ORANGE_WOOL, 1),
		MAGENTA(DungeonCommandMapping.MAGENTA, "Magenta", "Magenta", "monumenta:dungeons/magenta/find", Material.MAGENTA_WOOL, 1),
		LIGHTBLUE(DungeonCommandMapping.LIGHTBLUE, "Light Blue", "LightBlue", "monumenta:dungeons/light_blue/find", Material.LIGHT_BLUE_WOOL, 1),
		YELLOW(DungeonCommandMapping.YELLOW, "Yellow", "Yellow", "monumenta:dungeons/yellow/find", Material.YELLOW_WOOL, 1),
		WILLOWS(DungeonCommandMapping.WILLOWS, "Willows", "R1Bonus", "monumenta:dungeons/r1bonus/find", Material.JUNGLE_LEAVES, 1),
		REVERIE(DungeonCommandMapping.REVERIE, "Reverie", "Corrupted", "monumenta:dungeons/reverie/find", Material.FIRE_CORAL, 1),
		// R2:
		LIME(DungeonCommandMapping.LIME, "Lime", "Lime", "monumenta:dungeons/lime/find", Material.LIME_WOOL, 2),
		PINK(DungeonCommandMapping.PINK, "Pink", "Pink", "monumenta:dungeons/pink/find", Material.PINK_WOOL, 2),
		GRAY(DungeonCommandMapping.GRAY, "Gray", "Gray", "monumenta:dungeons/gray/find", Material.GRAY_WOOL, 2),
		LIGHTGRAY(DungeonCommandMapping.LIGHTGRAY, "Light Gray", "LightGray", "monumenta:dungeons/light_gray/find", Material.LIGHT_GRAY_WOOL, 2),
		CYAN(DungeonCommandMapping.CYAN, "Cyan", "Cyan", "monumenta:dungeons/cyan/find", Material.CYAN_WOOL, 2),
		PURPLE(DungeonCommandMapping.PURPLE, "Purple", "Purple", "monumenta:dungeons/purple/find", Material.PURPLE_WOOL, 2),
		TEAL(DungeonCommandMapping.TEAL, "Teal", "Teal", "monumenta:dungeons/teal/find", Material.CYAN_CONCRETE_POWDER, 2),
		SHIFTINGCITY(DungeonCommandMapping.SHIFTINGCITY, "Shifting City", "Fred", "monumenta:dungeons/fred/find", Material.PRISMARINE_BRICKS, 2),
		FORUM(DungeonCommandMapping.FORUM, "Forum", "Forum", "monumenta:dungeons/forum/find", Material.BOOKSHELF, 2),
		RUSH(DungeonCommandMapping.RUSH, "Rush", "RushHighestRound", "monumenta:challenges/r1/rushdown/find", Material.FIRE_CORAL_BLOCK, 2),
		// R3:
		SKT(DungeonCommandMapping.SKT, "Silver Knight's Tomb", "SKT", "monumenta:dungeons/skt/find", Material.DEEPSLATE, 3),
		BLUE(DungeonCommandMapping.BLUE, "Blue", "Blue", "monumenta:dungeons/blue/find", Material.BLUE_WOOL, 3),
		BROWN(DungeonCommandMapping.BROWN, "Brown", "Brown", "monumenta:dungeons/brown/find", Material.BROWN_WOOL, 3),
		HEXFALL(DungeonCommandMapping.HEXFALL, "Hexfall", "Ruten", "monumenta:dungeons/hexfall/find", Material.MOSSY_STONE_BRICKS, 3),
		INDIGO(DungeonCommandMapping.INDIGO, "Indigo", "Indigo", "monumenta:dungeons/indigo/find", Material.PURPLE_CONCRETE_POWDER, 3);

		private final DungeonCommandMapping mDungeonMapping;
		final ItemStack mIcon; // Item icon. Combined, contains the dungeon name, color, and base item
		final String mAdvancement; // Discovery advancement
		final String mCompletionScore;
		final int mRegion; // defined section, for organization purposes. int 1-6: r1 main, r1 bonus, r2 main, r2 bonus, r3 main, r3 bonus

		TrackedDungeons(DungeonCommandMapping map, String name, String completionScore, String advancement, Material mat, int region) {
			mDungeonMapping = map;
			mIcon = GUIUtils.createBasicItem(mat, name, map.getLocationColor(), true, "");
			mCompletionScore = completionScore;
			mAdvancement = advancement;
			mRegion = region;
		}
	}

		/* Page Information
	    Page 1: Region 1 Dungeons
	    Page 2: Region 2 Dungeons
	    Page 3: Region 3 Dungeons
	 */

	public DungeonsGui(Player player, int page) {
		super(player, 6 * 9, Component.text("Weekly Dungeons Information", NamedTextColor.GOLD));
		mPage = page;
	}

	@Override
	protected void setup() {
		// P.E.B menu item, which is on all pages
		setItem(0, 0, GUIUtils.createBasicItem(Material.OBSERVER, "Return to P.E.B. Menu", NamedTextColor.GOLD, true,
			"Returns you to the P.E.B. main page."))
			.onLeftClick(() -> {
				mPlayer.playSound(mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
				runConsoleCommand("openpeb @S");
			});

		// new day and new week clock, which is on all pages
		ItemStack timeItem = new ItemStack(Material.CLOCK, 1);
		ItemMeta meta = timeItem.getItemMeta();
		meta.displayName(Component.text("Current Time", NamedTextColor.GOLD, TextDecoration.BOLD)
			.decoration(TextDecoration.ITALIC, false));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text(String.format("A new day begins in %dh %dm", DateUtils.untilNewDay(ChronoUnit.HOURS), DateUtils.untilNewDay(ChronoUnit.MINUTES) % 60), NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("A new week begins in %dd %dh", DateUtils.untilNewWeek(ChronoUnit.DAYS), DateUtils.untilNewWeek(ChronoUnit.HOURS) % 24), NamedTextColor.YELLOW)
			.decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		timeItem.setItemMeta(meta);
		setItem(1, 0, timeItem);

		// set glass items for GUI organization
		for (int i = 0; i < 6; i++) {
			setItem(i, 1, GUIUtils.createBasicItem(Material.GRAY_STAINED_GLASS_PANE, "", NamedTextColor.GRAY, false, ""));
		}

		// Page selection items, which are on all pages
		// Region 1: No Requirements
		if (mPage == 1) {
			setItem(2, 0, GUIUtils.createBasicItem(Material.EMERALD_BLOCK, "King's Valley",
				NamedTextColor.GREEN,
				true,
				"You have this page open already!",
				NamedTextColor.RED
			));
		} else {
			setItem(2, 0, GUIUtils.createBasicItem(Material.GREEN_TERRACOTTA, "King's Valley", NamedTextColor.GREEN, true,
				"View Region 1 daily completions."))
				.onLeftClick(() -> {
					mPage = 1;
					mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.5f);
					update();
				});
		}
		// Region 2:
		if (mPage == 2) {
			setItem(3, 0, GUIUtils.createBasicItem(Material.EMERALD_BLOCK, "Celsian Isles",
				NamedTextColor.AQUA,
				true,
				"You have this page open already!",
				NamedTextColor.RED
			));
		} else {
			if (PlayerUtils.hasUnlockedIsles(mPlayer)) {
				setItem(3, 0, GUIUtils.createBasicItem(Material.SAND, "Celsian Isles", NamedTextColor.AQUA, true,
					"View Region 2 daily completions."))
					.onLeftClick(() -> {
						mPage = 2;
						mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.5f);
						update();
					});
			} else {
				setItem(3, 0, GUIUtils.createBasicItem(Material.BEDROCK, "Locked", NamedTextColor.RED, true,
					"You do not have this region unlocked yet!", NamedTextColor.RED));
			}
		}

		// Region 3:
		if (mPage == 3) {
			setItem(4, 0, GUIUtils.createBasicItem(Material.EMERALD_BLOCK, "Architect's Ring",
				NamedTextColor.RED,
				true,
				"You have this page open already!",
				NamedTextColor.RED
			));
		} else {
			if (PlayerUtils.hasUnlockedRing(mPlayer)) {
				setItem(4, 0, GUIUtils.createBasicItem(Material.RED_MUSHROOM_BLOCK, "Architect's Ring", NamedTextColor.RED, true,
					"View Region 3 daily completions."))
					.onLeftClick(() -> {
						mPage = 3;
						mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.5f);
						update();
					});
			} else {
				setItem(4, 0, GUIUtils.createBasicItem(Material.BEDROCK, "Locked", NamedTextColor.RED, true,
					"You do not have this region unlocked yet!", NamedTextColor.RED));
			}
		}

		// Dungeons GUI (DungeonsGui.java):
		setItem(5, 0, GUIUtils.createBasicItem(Material.MAP, "Switch to Dailies GUI", NamedTextColor.GOLD, true,
			"View daily content status."))
			.onLeftClick(() -> {
				mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.5f);
				new DailiesGui(mPlayer, mPage).open();
				close();
			});

		// Page Entries for Regional Content:
		// Parse through the TrackedDungeons enum and add items that match
		// the page's region.
		if (mPage >= 1 && mPage <= 3) {
			int editingRow = 0;
			int editingColumn = 2;
			for (TrackedDungeons dungeonEntry : TrackedDungeons.values()) {
				if (mPage == dungeonEntry.mRegion) { // Check if you are on that entry's regional page
					// First, verify that this is actually unlocked OR that a player has an instance or complete
					// If one of the three are met, do Steps 1+2
					if (AdvancementUtils.checkAdvancement(mPlayer, dungeonEntry.mAdvancement)
					|| dungeonEntry.mDungeonMapping.getAccessScore(mPlayer) >= 1
					|| ScoreboardUtils.getScoreboardValue(mPlayer, dungeonEntry.mCompletionScore).orElse(0) >= 1) {
						// Step 1: Place the icon of the dungeon entry
						ItemStack editedIcon = dungeonEntry.mIcon;
						ItemMeta iconMeta = editedIcon.getItemMeta();
						List<Component> iconLore = new ArrayList<>();
						if (dungeonEntry.equals(TrackedDungeons.RUSH)) {
							iconLore.add(Component.text("Record: ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "RushHighestRoundSolo").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" wave(s) solo,", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
							iconLore.add(Component.text("and ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "RushHighestRound").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" wave(s) in multiplayer.", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
						} else if (dungeonEntry.equals(TrackedDungeons.SKT)) {
							iconLore.add(Component.text("Completed ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "SKT").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" time(s) on Normal", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
							iconLore.add(Component.text("and ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "SKTH").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" time(s) on Savage.", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
							boolean hasNormalComplete = ScoreboardUtils.checkTag(mPlayer, "skt_weekly_normal");
							boolean hasSavageComplete = ScoreboardUtils.checkTag(mPlayer, "skt_weekly_hard");
							if (hasNormalComplete) {
								iconLore.add(Component.text("Completed this week on Normal", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
							} else {
								iconLore.add(Component.text("Uncompleted this week on Normal", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
							}
							if (hasSavageComplete) {
								iconLore.add(Component.text("Completed this week on Savage", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
							} else {
								iconLore.add(Component.text("Uncompleted this week on Savage", NamedTextColor.RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
							}
						} else if (dungeonEntry.equals(TrackedDungeons.HEXFALL)) {
							iconLore.add(Component.text("Completed Ru'Ten ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "Ruten").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" time(s)", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
							iconLore.add(Component.text("and Hycenea ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, "Hexfall").orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" time(s).", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
						} else {
							iconLore.add(Component.text("Completed ", NamedTextColor.BLUE)
								.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, dungeonEntry.mCompletionScore).orElse(0)), NamedTextColor.GOLD)
									.append(Component.text(" time(s).", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
						}
						iconMeta.lore(iconLore);
						editedIcon.setItemMeta(iconMeta);
						setItem(editingRow, editingColumn, editedIcon);
						// Step 2: Place the glass item for the dungeon entry
						int accessScore = dungeonEntry.mDungeonMapping.getAccessScore(mPlayer);
						ItemStack editedGlass;
						ItemMeta glassMeta;
						List<Component> glassLore = new ArrayList<>();
						boolean canInvite = dungeonEntry.mDungeonMapping.canInvite(mPlayer);
						if (!(accessScore >= 1)) { // RED: No instance. No description needed.
							editedGlass = NO_INSTANCE_ITEM.clone();
						} else { // There is an instance, so there will be a description.
							// TODO: This is currently borrowing existing descriptions; should it use its own?
							glassLore = dungeonEntry.mDungeonMapping.getDungeonAccessTimeInfo(mPlayer);
							// Then, set the color of glass
							if (!dungeonEntry.mDungeonMapping.canAlwaysInvite() && canInvite) { // GREEN: Invitable, it is New
								editedGlass = NEW_INSTANCE_ITEM.clone();
							} else if (dungeonEntry.mDungeonMapping.canAlwaysInvite()) { // DARK GREEN: Always Invitable
								editedGlass = OPEN_INSTANCE_ITEM.clone();
							} else { // YELLOW: Not invitable, instance is not new
								editedGlass = OLD_INSTANCE_ITEM.clone();
							}
						}
						glassMeta = editedGlass.getItemMeta();
						glassMeta.lore(glassLore);
						editedGlass.setItemMeta(glassMeta);
						setItem(editingRow + 1, editingColumn, editedGlass);

					}
					// Step 3: Edit row/column values properly to prepare placing the next item
					if (editingColumn == 8) {
						editingRow += 2;
						editingColumn = 2;
					} else {
						editingColumn++;
					}
					// TODO: error if there's too many items?
				}

			}
		}

	}

	// Utilized from other GUIs, runs a command for the player
	private void runConsoleCommand(String command) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(command.replace("@S", mPlayer.getName()));
	}
}
