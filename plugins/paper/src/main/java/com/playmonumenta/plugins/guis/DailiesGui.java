package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class DailiesGui extends Gui {

	private static final ItemStack NO_COMPLETION_ITEM = GUIUtils.createBasicItem(
		Material.RED_STAINED_GLASS_PANE,
		"Uncompleted Today",
		NamedTextColor.RED,
		true,
		""
		);

	private static final ItemStack PARTIAL_COMPLETION_ITEM = GUIUtils.createBasicItem(
		Material.YELLOW_STAINED_GLASS_PANE,
		"Partially Completed Today",
		NamedTextColor.YELLOW,
		true,
		""
	);

	private static final ItemStack CLAIMABLE_COMPLETION_ITEM = GUIUtils.createBasicItem(
		Material.ORANGE_STAINED_GLASS_PANE,
		"Available to Claim",
		NamedTextColor.GOLD,
		true,
		"");

	private static final ItemStack FULL_COMPLETION_ITEM = GUIUtils.createBasicItem(
		Material.LIME_STAINED_GLASS_PANE,
		"Completed Today",
		NamedTextColor.GREEN,
		true,
		""
	);

	private int mPage;

	// Dailies which are tracked in this GUI. Order by the desired order to be tracked.
	public enum TrackedDailies {
		// R1:
		DAILY1("King's Bounty", Location.OVERWORLD1.getColor(), 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzQ5YzYzYmM1MDg3MjMzMjhhMTllNTk3ZjQwODYyZDI3YWQ1YzFkNTQ1NjYzYWMyNDQ2NjU4MmY1NjhkOSJ9fX0="), "monumenta:quests/r1/farr", null, 0, "DailyCompleted", null, "DailyQuestsLeft", 2),
		AZACOR(Location.AZACOR, 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDQ0NzcyZGM0ZGVmMjIyMTllZTZkODg5Y2NkYzJmOTIzMmVlMjNkMzU2ZGQ5ZTRhZGNlYTVmNzJjYzBjNjg5In19fQ=="), null, "Quest45", 11, "AzacorHardWin", "azacor_daily_artifact", "DailyLimitAzacor", 8),
		KAUL(Location.KAUL, 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI2MTMwZGM5MGY3ODI4MGFmN2MwNGIyYjc5ZmJkYTNhNTJkNGQzZmZkNGUxMTIyYzc5YjMxMWUwOGQ2NjMwNSJ9fX0="), null, "Quest21", 20, "KaulWins", "kaul_daily_artifact", "DailyLimitKaul", 8),
		VERDANT(Location.VERDANT, 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTNkOGViYjExZjVhMDVjN2FmMjJiMTcyOTc0ZDZiMWUzYzI3OGY4OGJiOTAxOTc3NzY2MTU0M2FkNjJiZDBhYSJ9fX0="), "monumenta:challenges/r1/verdant/find", null, 0, "Verdant", "verdant_artifact_daily", null, 0),
		SANCTUM(Location.SANCTUM, 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjdiOTZkZDk1ZTNlZmViNDk4OTJmNWQ5OTc4NDE3NTg3NDM1YTkzZGQ3MzYxZmUyNzI1YTEyNjE4ZGFiYmNiYyJ9fX0="), "monumenta:challenges/r1/sanctum/find", null, 0, "Sanctum", "sanctum_artifact_daily", null, 0),
		SNOWSPIRIT("Snow Spirit", Location.WINTER.getColor(), 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmIxYzU4YzNiMGJmZTA3Y2Y3OWMwMTc0MWQwYjkzM2ZmNmE3YTdlYzc1ZjY2YjZmNmNiNDk1MjRjMTZlNzhlZSJ9fX0="), null, "Quest58", 7, "SnowSpiritWins", "snowspirit_daily_artifact", null, 0),
		KOAL(Location.KOAL, 1, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjNkZGU1ZDM2YmEwMTA5MzAyYTU1MzZkZjYwNDZhMGNiNmZhZGUyMmU0YWFkODAxNjE3NDA5MTcwYjg5YzFjIn19fQ=="), "monumenta:challenges/r1/coalrupted/find", null, 0, "CoalruptedSierhaven", "coalrupted_artifact_daily", null, 0),
		// R2:
		DAILY2("Isles Bounty", Location.OVERWORLD2.getColor(), 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTMzOThhYjNjYjY5NmIzNDQzMGJlOTQ0YjE0YWZiZDIyN2ZkODdlOTkwMjZiY2ZjOGI3Mzg3YTg2MWJkZSJ9fX0="), null, "Quest101", 12, "Daily2Completed", null, null, 0),
		HORSEMAN(Location.HORSEMAN, 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGM2NTcwZjEyNDI5OTJmNmViYTIzZWU1ODI1OThjMzllM2U3NDUzODMyNzNkZWVmOGIzOTc3NTgzZmUzY2Y1In19fQ=="), null, "TP_Horseman", 1, "HorsemanWins", "horseman_daily_artifact", "DailyLimitHorse", 8),
		MIST(Location.MIST, 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMThhYjVmOWVlOWUyZDNkYzY2NjdmZTM4YjA3ZWNhMzQ0OTc1NDA5ZGFlZjRmY2RlMTk4MTRjNTYzZjQxZTc3NiJ9fX0="), null, "TP_Mist", 1, "MistClears", "mist_artifact_daily", null, 0),
		REMORSE(Location.REMORSE, 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODZjZTliMThjYTIzOGVhZDY1MTViMDE3MzUzNGE3ZjM0ZDI4YmM1MWMwMTg4ZTJkYjA2ODQ1YzE3ZTYyZjEwYSJ9fX0="), "monumenta:challenges/r2/sr/find", null, 0, "SealedRemorse", "sr_artifact_daily", null, 0),
		ELDRASK("Eldrask, the Frost Giant", Location.FROSTGIANT.getColor(), 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGRiYTY0MmVmZmZhMTNlYzM3MzBlYWZjNTkxNGFiNjgxMTVjMWY5OTg4MDNmNzQ0NTJlMmUwY2QyNmFmMGI4In19fQ=="), null, "Quest124", 19, "FGWins", "fg_daily_artifact", "DailyLimitGiant", 8),
		HEKAWT(Location.LICH, 2, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2Y0MGFmODE4MDdiYzE0ZDk4ZWJjNzBkNDc4MTRiODNmNjZmZWM4ZmE5ZjY1MWFjY2RlYWU0ODY5Y2M0ZjY4ZSJ9fX0="), null, "Fred", 1, "LichWins", "lich_daily_artifact", "DailyLimitLich", 8),
		// R3:
		DAILY3("Architect's Ring Bounty", Location.FOREST.getColor(), 3, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjI0ODQ4OTI4NWVjOTM3MzVmMjNhOGYzNDU2OGFmMTIxMGU2YjViZDlmYjRlZjgwNzViY2Q5MjBiYTBkNTlmOCJ9fX0="), null, "R3Access", 1, "Daily3Completed", null, null, 0),
		PORTAL(Location.SCIENCE, 3, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2Q5NmI3ZmU1ZjRmYjJhYmFhMWE0MzEyNzc2NmUyNTI0YzQyZjkyZGYxNWIxYTczZjNlN2Y2NTUxOWRkYzgyIn19fQ=="), "monumenta:challenges/r3/portal/find", null, 0, "Portal", "portal_artifact_daily", null, 0),
		RUIN(Location.BLUESTRIKE, 3, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjFmZmU2MzhhM2RjYWU3ZjZkNjRhMTgxYzU2NDI3MmFhZjIyZGM1ODgyYmQzMDZiMWJlOTUzZGYzZmJlM2E0ZSJ9fX0="), "monumenta:challenges/r3/ruin/find", null, 0, "MasqueradersRuin", "ruin_artifact_daily", null, 0),
		GODSPORE(Location.GODSPORE, 3, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGI3NWU2OWIyMTNjNTI5NzgzMzk0ODUxNTBmMTViZGUwZDFmNjU0ZmU0N2Q1MWE2YTE5ZWY0Yjc0OWVjIn19fQ=="), null, "GodsporeLobby", 1, "GodsporeWins", "godspore_daily_artifact", null, 0),
		FISHINGCOMBAT("Fishing Combat", Location.FISHING.getColor(), 3, new ItemStack(Material.PUFFERFISH), null, "R3Access", 1, "FishCombatsCompleted", null, "DailyLimitRingFishingCombat", 4),
		FISHINGQUEST("Fishing Quest", Location.FISHING.getColor(), 3, new ItemStack(Material.FISHING_ROD), null, "R3Access", 1, "FishQuestsCompleted", null, null, 0),
		ZENITH("Celestial Zenith", Location.ZENITH.getColor(), 3, new ItemStack(Material.SCULK_SENSOR), "monumenta:dungeons/zenith/find", null, 0, "Zenith", "zenith_daily", null, 0),
		SIRIUS("Sirius, the Final Blight", Location.SIRIUS.getColor(), 3, ItemUtils.createPlayerHeadFromBase64("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQzZDI1MGUyNWJiY2EzYTYyYmU1YjNlZjAyY2ZjYWI2ZGNkYzQyNDg4NGM5YTdkNWNjOTVjOWQwIn19fQ=="), null, "SiriusWins", 1, "SiriusWins", "sirius_daily_artifact", "DailyLimitSirius", 8),
		INTRUDER(Location.TWISTED_INTRUDER, 3, ItemUtils.createPlayerHeadFromBase64("e3RleHR1cmVzOntTS0lOOnt1cmw6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJjMmJhNTc1YTQzMGZiOWY2ODBlMzZjNTBkYzgyNDE4YjY1MmZhOTczZDM3NTY2M2MzYTViOTUzNmQ0MzM2ZCJ9fX0="), null, "TwistedXWins", 1, "TwistedXWins", "twistedx_daily_artifact", null, 0),
		AURORA(Location.AURORA, 3, ItemUtils.createPlayerHeadFromBase64("ewogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWU2ODBmOWIxNDBhZTljMWNiZGMzZDFmNzE0ZWQzYWVlMTkyOWVhZjBiNzA4NDUzYTM1YWRmNTk5NDE1ODA2IgogICAgfQogIH0KfQ=="), null, "Aurora", 1, "Aurora", "aurora_daily_artifact", null, 0);


		private final Location mLocation; // Location of the content
		final Component mName; // Component for name. Either taken from Location, or specified in list
		final int mRegion; // Region
		final ItemStack mIcon; // Icon for content item (ex: player head)
		final @Nullable String mAdvancementReqPath; // Advancement path to unlock
		final @Nullable String mScoreboardReqName; // Scoreboard to unlock
		final int mScoreboardReqValue; // Minimum score value to unlock; set to 0 if the req name is null
		final String mCompletionScore; // Content completion score
		final @Nullable String mScoreboardDailyTag; // Content daily tag indicating a daily has been completed
		final @Nullable String mScoreboardMaxDailiesName; // Scoreboard showing max dailies possible
		final int mScoreboardMaxDailiesValue; // Maximum score value that is possible; set to 0 is the max name is null

		// Uses a Location to define the content's Name and Text Color
		TrackedDailies(Location loc, int region, ItemStack icon,
					   @Nullable String advancementReqPath, @Nullable String scoreboardReqName,
					   int scoreboardReqValue, String completionScore,
					   @Nullable String scoreboardDailyTag, @Nullable String scoreboardMaxDailiesName, int scoreboardMaxDailiesValue) {
			mLocation = loc;
			mName = mLocation.getDisplay().decoration(TextDecoration.BOLD, true);
			mRegion = region;
			mIcon = icon;
			mAdvancementReqPath = advancementReqPath;
			mScoreboardReqName = scoreboardReqName;
			mScoreboardReqValue = scoreboardReqValue;
			mCompletionScore = completionScore;
			mScoreboardDailyTag = scoreboardDailyTag;
			mScoreboardMaxDailiesName = scoreboardMaxDailiesName;
			mScoreboardMaxDailiesValue = scoreboardMaxDailiesValue;
		}

		// Ignores Location, custom definition for content's Name and Text Color
		TrackedDailies(String name, TextColor color, int region, ItemStack icon,
					   @Nullable String advancementReqPath, @Nullable String scoreboardReqName,
					   int scoreboardReqValue, String completionScore,
					   @Nullable String scoreboardDailyTag, @Nullable String scoreboardMaxDailiesName, int scoreboardMaxDailiesValue) {
			mLocation = Location.NONE;
			mName = Component.text(name, color).decoration(TextDecoration.BOLD, true);
			mRegion = region;
			mIcon = icon;
			mAdvancementReqPath = advancementReqPath;
			mScoreboardReqName = scoreboardReqName;
			mScoreboardReqValue = scoreboardReqValue;
			mCompletionScore = completionScore;
			mScoreboardDailyTag = scoreboardDailyTag;
			mScoreboardMaxDailiesName = scoreboardMaxDailiesName;
			mScoreboardMaxDailiesValue = scoreboardMaxDailiesValue;
		}
	}
	/* Page Information
	    Page 1: Region 1 Dailies
	    Page 2: Region 2 Dailies
	    Page 3: Region 3 Dailies
	 */

	public DailiesGui(Player player, int page) {
		// TODO: Add something which initializes scores to 0 on the opening of gui (see monumenta:mechanisms/info/dailies)
		// This is technically not needed, as now the GUI visuals actually factor this in
		super(player, 6 * 9, Component.text("Daily Content Information", NamedTextColor.GOLD));
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
		setItem(5, 0, GUIUtils.createBasicItem(Material.SPAWNER, "Switch to Dungeons GUI", NamedTextColor.GRAY, true,
				"View weekly dungeons status."))
				.onLeftClick(() -> {
					mPlayer.playSound(mPlayer, Sound.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.5f);
					new DungeonsGui(mPlayer, mPage).open();
					close();
				});

		// Page Entries for Regional Content:
		// Parse through the TrackedDailies enum and add items that match
		// the page's region.
		if (mPage >= 1 && mPage <= 3) {
			int editingRow = 0;
			int editingColumn = 2;
			for (TrackedDailies dailyEntry : TrackedDailies.values()) {
				if (mPage == dailyEntry.mRegion) { // Check if you are on that entry's regional page
					// Verify that this is actually unlocked before doing Steps 1 & 2
					if ((dailyEntry.mScoreboardReqName != null && ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardReqName).orElse(0) >= dailyEntry.mScoreboardReqValue)
						|| (dailyEntry.mAdvancementReqPath != null && AdvancementUtils.checkAdvancement(mPlayer, dailyEntry.mAdvancementReqPath))) {
						// Step 1: Place the icon of the daily entry
						ItemStack editedIcon = dailyEntry.mIcon;
						ItemMeta iconMeta = editedIcon.getItemMeta();
						iconMeta.displayName(dailyEntry.mName.decoration(TextDecoration.ITALIC, false));
						List<Component> iconLore = new ArrayList<>();
						iconLore.add(Component.text("Completed ", NamedTextColor.BLUE)
							.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mCompletionScore).orElse(0)), NamedTextColor.GOLD)
								.append(Component.text(" time(s).", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
						iconMeta.lore(iconLore);
						editedIcon.setItemMeta(iconMeta);
						setItem(editingRow, editingColumn, editedIcon);
						// Step 2: Place the glass item for the daily entry
						if (dailyEntry.equals(TrackedDailies.SNOWSPIRIT) && !mPlayer.hasPermission("monumenta.event.winter")) { // Hardcoded glass for snow spirit permission locked
							setItem(editingRow + 1, editingColumn, GUIUtils.createBasicItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE, "Daily Unavailable", NamedTextColor.DARK_AQUA, true,
								"This daily reward is unavailable outside of the Winter Event!", NamedTextColor.AQUA));
						} else if (dailyEntry.equals(TrackedDailies.FISHINGQUEST)) { // Hardcoded glass for Daily Ring Fish. Assigned = incomplete, Not assigned = complete
							if (ScoreboardUtils.getScoreboardValue(mPlayer, "DailyRingFish").orElse(0) == 0) {
								setItem(editingRow + 1, editingColumn, FULL_COMPLETION_ITEM);
							} else {
								setItem(editingRow + 1, editingColumn, NO_COMPLETION_ITEM);
							}
						} else if (dailyEntry.equals(TrackedDailies.DAILY1) || dailyEntry.equals(TrackedDailies.DAILY2) || dailyEntry.equals(TrackedDailies.DAILY3)) { // Hardcoded glass for regional dailies for POIs in progress
							ItemStack editedGlass;
							String questScoreboard = switch (dailyEntry) {
								case TrackedDailies.DAILY1 -> "DailyQuest";
								case TrackedDailies.DAILY2 -> "Daily2Quest";
								case TrackedDailies.DAILY3 -> "Daily3Quest";
								default -> "";
							};
							String rewardScoreboard = switch (dailyEntry) {
								case TrackedDailies.DAILY1 -> "DailyReward";
								case TrackedDailies.DAILY2 -> "Daily2Reward";
								case TrackedDailies.DAILY3 -> "Daily3Reward";
								default -> "";
							};
							int playerBountyScore = ScoreboardUtils.getScoreboardValue(mPlayer, questScoreboard).orElse(0);
							int playerRewardScore = ScoreboardUtils.getScoreboardValue(mPlayer, rewardScoreboard).orElse(0);
							if (playerBountyScore >= 1 && playerBountyScore <= 999) {
								// daily in progress
								editedGlass = PARTIAL_COMPLETION_ITEM.clone();
								ItemMeta glassMeta = editedGlass.getItemMeta();
								glassMeta.displayName(Component.text("Bounty Started", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
								editedGlass.setItemMeta(glassMeta);
							} else if (playerRewardScore != 0) {
								// daily ready to claim
								editedGlass = CLAIMABLE_COMPLETION_ITEM.clone();
							} else if (playerBountyScore >= 1000) {
								// daily completed
								editedGlass = FULL_COMPLETION_ITEM.clone();
							} else {
								// daily not completed, bounty score is 0
								editedGlass = NO_COMPLETION_ITEM.clone();
							}
							if (dailyEntry.mScoreboardMaxDailiesName != null && ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardMaxDailiesName).orElse(0) >= 1) { // multiple dailies exist
								ItemMeta glassMeta = editedGlass.getItemMeta();
								List<Component> glassLore = new ArrayList<>();
								glassLore.add(Component.text("Bounties left for today: ", NamedTextColor.BLUE)
										.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardMaxDailiesName).orElse(0)), NamedTextColor.GOLD)
												.append(Component.text(".", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
								glassMeta.lore(glassLore);
								editedGlass.setItemMeta(glassMeta);
							} else if (playerRewardScore != 0 && playerBountyScore == 0) { // Case if player didn't claim a prior day's bounty in R2/R3
								ItemMeta glassMeta = editedGlass.getItemMeta();
								List<Component> glassLore = new ArrayList<>();
								glassLore.add(Component.text("Bounties left for today: ", NamedTextColor.BLUE)
										.append(Component.text("1", NamedTextColor.GOLD)
												.append(Component.text(".", NamedTextColor.BLUE))).decoration(TextDecoration.ITALIC, false));
								glassMeta.lore(glassLore);
								editedGlass.setItemMeta(glassMeta);
							}
							setItem(editingRow + 1, editingColumn, editedGlass);
						} else if ((dailyEntry.mScoreboardDailyTag != null && !ScoreboardUtils.checkTag(mPlayer, dailyEntry.mScoreboardDailyTag))
						|| (dailyEntry.mScoreboardMaxDailiesName != null && ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardMaxDailiesName).orElse(0) == dailyEntry.mScoreboardMaxDailiesValue)) { // Automated Glass Placement
							// Has no daily tag OR is at max daily completes - not completed today
							setItem(editingRow + 1, editingColumn, NO_COMPLETION_ITEM);
						} else {
							if (dailyEntry.mScoreboardMaxDailiesName != null && ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardMaxDailiesName).orElse(0) != 0) {
								// Has some daily options - partially completed today
								ItemStack editedGlass = PARTIAL_COMPLETION_ITEM.clone();
								ItemMeta glassMeta = editedGlass.getItemMeta();
								List<Component> glassLore = new ArrayList<>();
								glassLore.add(Component.text("Daily rewards left: ", NamedTextColor.BLUE)
									.append(Component.text(String.format("%d", ScoreboardUtils.getScoreboardValue(mPlayer, dailyEntry.mScoreboardMaxDailiesName).orElse(0)), NamedTextColor.GOLD)
										.append(Component.text(" (out of ", NamedTextColor.BLUE)
											.append(Component.text(String.format("%d", dailyEntry.mScoreboardMaxDailiesValue), NamedTextColor.GOLD)
												.append(Component.text(").", NamedTextColor.BLUE))))).decoration(TextDecoration.ITALIC, false));
								glassMeta.lore(glassLore);
								editedGlass.setItemMeta(glassMeta);
								setItem(editingRow + 1, editingColumn, editedGlass);
							} else {
								// Has no more daily options - completed today
								setItem(editingRow + 1, editingColumn, FULL_COMPLETION_ITEM);
							}
						}
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
