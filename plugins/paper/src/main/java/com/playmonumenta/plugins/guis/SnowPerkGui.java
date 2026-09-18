package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.snowperks.Butterfingers;
import com.playmonumenta.plugins.abilities.snowperks.CarbonCapture;
import com.playmonumenta.plugins.abilities.snowperks.CoalInsurance;
import com.playmonumenta.plugins.abilities.snowperks.CoalLauncher;
import com.playmonumenta.plugins.abilities.snowperks.CreeperMistletoe;
import com.playmonumenta.plugins.abilities.snowperks.DownTheChimney;
import com.playmonumenta.plugins.abilities.snowperks.FestiveSweater;
import com.playmonumenta.plugins.abilities.snowperks.IcicleBurst;
import com.playmonumenta.plugins.abilities.snowperks.LuminiteDrill;
import com.playmonumenta.plugins.abilities.snowperks.Nutcracker;
import com.playmonumenta.plugins.abilities.snowperks.SelfReflection;
import com.playmonumenta.plugins.abilities.snowperks.ShatterProofOrnament;
import com.playmonumenta.plugins.abilities.snowperks.ShinyWrappingPaper;
import com.playmonumenta.plugins.abilities.snowperks.SierhavenSnowglobe;
import com.playmonumenta.plugins.abilities.snowperks.SniffysBlessing;
import com.playmonumenta.plugins.abilities.snowperks.SnowLeopardClaw;
import com.playmonumenta.plugins.abilities.snowperks.SnowyOwlFeather;
import com.playmonumenta.plugins.abilities.snowperks.SphereOfVargos;
import com.playmonumenta.plugins.abilities.snowperks.StringLightHook;
import com.playmonumenta.plugins.abilities.snowperks.ToughCookie;
import com.playmonumenta.plugins.abilities.snowperks.WindUpCar;
import com.playmonumenta.plugins.guis.lib.Gui;
import com.playmonumenta.plugins.guis.lib.GuiItem;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.utils.DescriptionUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.scoreboard;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class SnowPerkGui extends Gui {
	public static final Style SNOW_POINT_COLOR = Style.style(TextColor.color(0x74D2D2));
	public static final Style SNOW_ARROW_COLOR = Style.style(TextColor.color(0x236D85));
	public static final Style COAL_COLOR = Style.style(TextColor.color(0x736B63));
	public static final Style COALRUPTED_COLOR = Style.style(Location.KOAL.getColor());
	public static final Style ACHIEVEMENT_COLOR = Style.style(TextColor.color(0x0D5723)).decorate(TextDecoration.UNDERLINED);
	public static final Style ACHIEVEMENT_ARROW_COLOR = Style.style(TextColor.color(0x33D14));
	public static final Style SNIFFY_COLOR = Style.style(TextColor.color(0xAA1100)).decorate(TextDecoration.UNDERLINED);
	public static final String REMAINING_POINTS = "SnowPoints";
	public static final String TOTAL_POINTS = "TotalSnowPoints";
	public static final String COAL_UNTIL_POINTS = "CoalUntilPoints";
	public static final String COAL_COLLECTED = "LifetimeCoalCollected";
	public static final String COMPLETIONS = "CoalruptedSierhaven";
	public static final String MAX_COAL_COLLECTED = "CoalruptedSierhavenMax";
	public static final List<AbilityInfo<?>> PERKS = List.of(
		// Page 1 Perks
		SierhavenSnowglobe.INFO,
		ShinyWrappingPaper.INFO,
		Nutcracker.INFO,
		ToughCookie.INFO,
		CoalInsurance.INFO,
		CoalLauncher.INFO,
		FestiveSweater.INFO,
		CarbonCapture.INFO,
		SnowLeopardClaw.INFO,
		SnowyOwlFeather.INFO,
		CreeperMistletoe.INFO,
		ShatterProofOrnament.INFO,
		IcicleBurst.INFO,
		StringLightHook.INFO,

		// Page 2 Perks
		DownTheChimney.INFO,
		SphereOfVargos.INFO,
		Butterfingers.INFO,
		WindUpCar.INFO,
		LuminiteDrill.INFO,
		SelfReflection.INFO,
		SniffysBlessing.INFO
	);
	public static final Set<AbilityInfo<?>> ACHIEVEMENT_PERKS = Set.of(
		CreeperMistletoe.INFO,
		StringLightHook.INFO,
		LuminiteDrill.INFO,
		SelfReflection.INFO,
		SniffysBlessing.INFO
	);
	private static final int PERKS_PER_PAGE = 14;
	private static final int[][] PERK_POSITIONS = {{10, 11, 12, 13, 14, 15, 16, 28, 29, 30, 31, 32, 33, 34}, {19, 20, 21, 22, 23, 24, 25}};
	private static final List<TextColor> LIGHT_COLORS = List.of(TextColor.color(0xE6556D), TextColor.color(0xE6AF50), TextColor.color(0x51AB3F), TextColor.color(0x7E7EE6), TextColor.color(0xE65C95));

	public static class SnowPerkInfo<T extends Ability> extends AbilityInfo<T> {
		private int mSnowPointCost = 0;
		private Predicate<Player> mUnlockReq = player -> true;

		public SnowPerkInfo(Class<T> abilityClass, @Nullable String displayName, BiFunction<Plugin, Player, T> constructor) {
			super(abilityClass, displayName, constructor);
			canUse(player -> ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.COALRUPTED_SNOW_PERKS) && getLevelScore(player) > 0);
		}

		public SnowPerkInfo<T> snowPointCost(int cost) {
			mSnowPointCost = cost;
			return this;
		}

		public int snowPointCost() {
			return mSnowPointCost;
		}

		public SnowPerkInfo<T> unlockReq(Predicate<Player> req) {
			mUnlockReq = req;
			return this;
		}

		public Predicate<Player> unlockReq() {
			return mUnlockReq;
		}
	}

	private final Plugin mPlugin;
	private final BukkitRunnable mDescriptionRunnable; // animated holiday light borders!
	private final boolean mIsMaxPoints;
	private boolean mLightParity = true;
	private int mRainbowFrame = 0;
	private int mCurrentPage = 1;

	public SnowPerkGui(Player player) {
		super(player, GUIUtils.FILLER, Component.text("Snow Perk Selection"), 6 * 9);

		mPlugin = Plugin.getInstance();

		// GUI updating can be laggy, so only have the higher interval if the player is maxed and needs rainbow text
		mIsMaxPoints = ScoreboardUtils.getScoreboardValue(mPlayer, COAL_UNTIL_POINTS).orElse(0) == -1;
		if (mIsMaxPoints) {
			mDescriptionRunnable = new BukkitRunnable() {
				int mTimer = 0;
				@Override
				public void run() {
					// runs every 2 ticks, so we need to change lights every 15 runs
					mTimer = (mTimer + 1) % 15;
					if (mTimer == 0) {
						mLightParity = !mLightParity;
					}

					mRainbowFrame = (mRainbowFrame + 1) % 16;

					markDirty();
					update();
				}
			};
		} else {
			mDescriptionRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mLightParity = !mLightParity;
					markDirty();
					update();
				}
			};
		}

		mDescriptionRunnable.runTaskTimer(mPlugin, 0, mIsMaxPoints ? 2 : 30);
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		mDescriptionRunnable.cancel();
	}

	@Override
	protected void render() {
		Component mainMenuDescription = new FormattedDescriptionBuilder<>().arrowColor(SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("You have %d out of %d *Snow Points* remaining.").styles(SNOW_POINT_COLOR)
				.statValues(scoreboard(REMAINING_POINTS), scoreboard(TOTAL_POINTS))
			.addLine()
			.addLine("*Snow Points* are used to unlock perks that").styles(SNOW_POINT_COLOR)
			.addLine("activate while inside *Coalrupted Sierhaven*!").styles(COALRUPTED_COLOR)
			.addLine()
			.addIfElse((a, p) -> mIsMaxPoints,
				d -> {
					List<Style> styles = new ArrayList<>();
					for (int i = 0; i < 16; i++) {
						styles.add(Style.style(TextColor.color(HSVLike.hsvLike(i / 16f, 0.6f, 0.9f))));
					}
					Collections.rotate(styles, mRainbowFrame);
					styles.add(SNOW_POINT_COLOR);
					return d.addLine("*C**o**n**g**r**a**t**u**l**a**t**i**o**n**s**!* You've maxed out your *Snow Points*!").styles(styles);
				},
				d -> d.addLine("Collect %d more *Coal* to get +%d *Snow Points*!").styles(COAL_COLOR, SNOW_POINT_COLOR)
					.statValues(scoreboard(COAL_UNTIL_POINTS), stat(2)))
			.addLine()
			.addStat("Total Coal Collected: %d")
				.statValues(scoreboard(COAL_COLLECTED))
			.addDashedLine()
			.get(mPlayer);

		mainMenuDescription = makeLinesJolly(mainMenuDescription, true, mLightParity);

		int remainingPoints = ScoreboardUtils.getScoreboardValue(mPlayer, REMAINING_POINTS).orElse(0);
		Component mainMenuName = DescriptionUtils.centeredComponent(mainMenuDescription, "Snow Points", SNOW_POINT_COLOR, true);
		GuiItem.builder().maxLoreLength(99)
			.name(mainMenuName)
			.lore(mainMenuDescription)
			.count(remainingPoints > 0 ? remainingPoints : 1)
			.material(remainingPoints > 0 ? Material.SNOW_BLOCK : Material.COAL_BLOCK)
			.set(this, 0, 4);

		Component resetDescription = new FormattedDescriptionBuilder<>().arrowColor(SNOW_ARROW_COLOR)
			.addDashedLine()
			.addLine("Reset your perks and all assigned")
			.addLine("points, allowing you to pick new")
			.addLine("*Snow Perks*.").styles(SNOW_POINT_COLOR)
			.addDashedLine()
			.addAction("Click to reset your perks.", DescriptionUtils.ACTION_SELECT)
			.get();
		resetDescription = makeLinesJolly(resetDescription, true, mLightParity);
		Component resetName = DescriptionUtils.centeredComponent(resetDescription, "Reset Snow Perks", SNOW_POINT_COLOR, true);
		GuiItem.builder(Material.POWDER_SNOW_BUCKET).maxLoreLength(99)
			.name(resetName)
			.lore(resetDescription)
			.onMouseClick(this::resetSnowPerks)
			.set(this, 5, 4);

		renderPageSwitchers(mCurrentPage);
		renderPerkItems(mCurrentPage);
	}

	private void renderPageSwitchers(int page) {
		boolean page2Unlocked = ScoreboardUtils.getScoreboardValue(mPlayer, MAX_COAL_COLLECTED).orElse(0) >= 600;

		if (page == 1) {
			Component forwardDescription = new FormattedDescriptionBuilder<>().arrowColor(SNOW_ARROW_COLOR)
				.addDashedLine()
				// Can't center components within FDB yet so manually add spaces :sob:
				.addLine("*Unlock:* *Reach a personal best of*").styles(DescriptionUtils.REQUIREMENT_LABEL, DescriptionUtils.REQUIREMENT_TEXT)
				.tab().addLine("*600 Coal collected in a run.*").styles(DescriptionUtils.REQUIREMENT_TEXT)
				.addLine()
				.addStat("Personal Best: %d *Coal*").styles(COAL_COLOR).statValues(scoreboard(MAX_COAL_COLLECTED))
				.addLine()
				.addLine("Go to the next page of *Snow Perks*.").styles(SNOW_POINT_COLOR)
				.addDashedLine()
				.addIfElse((a, p) -> page2Unlocked,
					desc -> desc.addAction("Click to go to page 2!", DescriptionUtils.ACTION_SELECT),
					desc -> desc.addAction("Page not unlocked yet!", DescriptionUtils.ACTION_DENIED))
				.get(mPlayer);
			forwardDescription = makeLinesJolly(forwardDescription, true, mLightParity);
			GuiItem.builder(Material.ARROW).maxLoreLength(99)
				.name(DescriptionUtils.centeredComponent(forwardDescription, "Next Page", SNOW_POINT_COLOR, true))
				.lore(forwardDescription)
				.onMouseClick(() -> {
					if (page2Unlocked) {
						mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
						mCurrentPage = 2;
						markDirty();
					} else {
						mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
						mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
					}
				})
				.set(this, 0, 8);
		} else {
			Component forwardDescription = new FormattedDescriptionBuilder<>()
				.addDashedLine()
				.addLine("Go to the previous page of *Snow Perks*.").styles(SNOW_POINT_COLOR)
				.addDashedLine()
				.addAction("Click to go to page 1!", DescriptionUtils.ACTION_SELECT)
				.get();
			forwardDescription = makeLinesJolly(forwardDescription, true, mLightParity);
			GuiItem.builder(Material.ARROW).maxLoreLength(99)
				.name(DescriptionUtils.centeredComponent(forwardDescription, "Previous Page", SNOW_POINT_COLOR, true))
				.lore(forwardDescription)
				.onMouseClick(() -> {
					mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
					mCurrentPage = 1;
					markDirty();
				})
				.set(this, 0, 0);
		}
	}

	private void renderPerkItems(int page) {
		// Page 1 has perks 0-13, page 2 has perks 14-27, etc.
		int startIndex = (page - 1) * PERKS_PER_PAGE;
		int endIndex = Math.min(startIndex + PERKS_PER_PAGE, PERKS.size());
		List<AbilityInfo<?>> perksToDisplay = PERKS.subList(startIndex, endIndex);

		for (int i = 0; i < perksToDisplay.size(); i++) {
			SnowPerkInfo<?> perk = (SnowPerkInfo<?>) perksToDisplay.get(i);

			if (perk.getDisplayName() == null || perk.getDisplayItem() == null || perk.getScoreboard() == null) {
				continue;
			}

			String scoreboard = perk.getScoreboard();
			int pointCost = perk.snowPointCost();
			int currentPoints = ScoreboardUtils.getScoreboardValue(mPlayer, REMAINING_POINTS).orElse(0);
			boolean alreadySelected = ScoreboardUtils.getScoreboardValue(mPlayer, scoreboard).orElse(0) > 0;
			boolean unlockedPerk = perk.unlockReq() != null && perk.unlockReq().test(mPlayer);
			boolean enoughPoints = pointCost <= currentPoints;
			boolean isAchievementPerk = ACHIEVEMENT_PERKS.contains(perk);
			boolean isSniffy = perk == SniffysBlessing.INFO;

			Style nameStyle;
			if (isSniffy) {
				nameStyle = SNIFFY_COLOR;
			} else if (isAchievementPerk) {
				nameStyle = ACHIEVEMENT_COLOR;
			} else {
				nameStyle = SNOW_POINT_COLOR;
			}

			Component name = Component.text(perk.getDisplayName(), nameStyle).decorate(TextDecoration.BOLD)
				.append(Component.text(StringUtils.smallCaps(alreadySelected ? " [Active]" : " [Inactive]"), alreadySelected ? DescriptionUtils.GOLD : DescriptionUtils.DARK_GREY)
					.decoration(TextDecoration.BOLD, false).decoration(TextDecoration.UNDERLINED, false));

			Component instruction;
			Material paneColor;
			if (alreadySelected) {
				instruction = DescriptionUtils.actionLine("Perk already selected.", DescriptionUtils.ACTION_COMPLETED).appendNewline()
					.append(DescriptionUtils.actionLine("Click to deselect!", DescriptionUtils.ACTION_SELECT));
				paneColor = isAchievementPerk ? Material.GREEN_STAINED_GLASS_PANE : Material.CYAN_STAINED_GLASS_PANE;
			} else if (!unlockedPerk) {
				instruction = DescriptionUtils.actionLine("Perk not unlocked!", DescriptionUtils.ACTION_DENIED);
				paneColor = Material.RED_STAINED_GLASS_PANE;
			} else if (!enoughPoints) {
				instruction = DescriptionUtils.actionLine("Not enough points!", DescriptionUtils.ACTION_DENIED);
				paneColor = Material.BLACK_STAINED_GLASS_PANE;
			} else {
				instruction = DescriptionUtils.actionLine("Click to select this perk!", DescriptionUtils.ACTION_SELECT);
				paneColor = Material.WHITE_STAINED_GLASS_PANE;
			}

			Component description = perk.getDescription(1, mPlayer, false);
			description = description.appendNewline().append(instruction);
			description = makeLinesJolly(description, alreadySelected, mLightParity);

			Runnable togglePerk = () -> {
				if (alreadySelected) {
					// Already selected; run deselect perk logic
					ScoreboardUtils.setScoreboardValue(mPlayer, scoreboard, 0);
					ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, currentPoints + pointCost);

					if (isSniffy) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SNIFFER_HURT, SoundCategory.PLAYERS, 0.9f, 1f);
					}
					mPlayer.playSound(mPlayer, Sound.BLOCK_TRIAL_SPAWNER_PLACE, SoundCategory.PLAYERS, 0.8f, 0.9f);
				} else if (!enoughPoints || !unlockedPerk) {
					// Action blocked due to something; error sound and return
					mPlayer.playSound(mPlayer, Sound.BLOCK_BAMBOO_WOOD_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1f, 1f);
					mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 0.8f, 0.75f);
					return;
				} else {
					// Allow selection to happen
					ScoreboardUtils.setScoreboardValue(mPlayer, scoreboard, 1);
					ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, currentPoints - pointCost);

					if (isSniffy) {
						mPlayer.playSound(mPlayer, Sound.ENTITY_SNIFFER_HAPPY, SoundCategory.PLAYERS, 0.9f, 1.25f);
					}
					if (isAchievementPerk) {
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f), 0);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 5/4f), 2);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 3/2f), 4);
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 5/3f), 6);
					} else {
						mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f);
					}
					mPlayer.playSound(mPlayer, Sound.ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.PLAYERS, 0.8f, 1.35f);
				}
				markDirty();
			};

			int position = PERK_POSITIONS[page - 1][i];
			GuiItem.Builder perkItem = GuiItem.builder().maxLoreLength(99)
				.name(name)
				.lore(description)
				.onMouseClick(togglePerk);
			perkItem.material(perk.getDisplayItem()).set(this, position);
			perkItem.material(paneColor).set(this, position + 9);
		}
	}

	/**
	 * Replaces regular FormattedDescriptionBuilder dashed lines with dotted lines with animated lights.
	 * @param component the description to replace dashed lines with animated light lines
	 * @param lightEnabled whether or not the lights should be turned on or not.
	 * @param lightParity which frame of the light animation to use (2-frame animation)
	 * @return a component with animated light lines
	 */
	private Component makeLinesJolly(Component component, boolean lightEnabled, boolean lightParity) {
		for (int i = 0; i < 2; i++) {
			boolean lineParity = i % 2 == 0;
			component = component.replaceText(builder -> {
				builder.match("–+");
				builder.once();
				builder.replacement((matchResult, b) -> {
					Component line = Component.text("·", DescriptionUtils.BLACK.decorate(TextDecoration.BOLD));

					int width = matchResult.group().length() * 8;
					int dashPairsNeeded = (int) Math.ceil((width - 4) / 7d);
					for (int j = 0; j < dashPairsNeeded; j++) {
						boolean lightOn = lightEnabled && (j + (lightParity ^ lineParity ? 1 : 0)) % 2 == 0;
						TextColor color = LIGHT_COLORS.get(j % 5);

						line = line.append(Component.text("•", lightOn ? Style.style(color) : DescriptionUtils.BLACK))
							.append(Component.text("·", DescriptionUtils.BLACK));
					}
					return line;
				});
			});
		}

		return component;
	}

	private void resetSnowPerks() {
		ScoreboardUtils.setScoreboardValue(mPlayer, REMAINING_POINTS, ScoreboardUtils.getScoreboardValue(mPlayer, TOTAL_POINTS).orElse(6));
		for (AbilityInfo<?> perk : PERKS) {
			SnowPerkInfo<?> snowPerk = (SnowPerkInfo<?>) perk;
			if (snowPerk.getScoreboard() != null) {
				ScoreboardUtils.setScoreboardValue(mPlayer, snowPerk.getScoreboard(), 0);
			}
		}

		mPlayer.playSound(mPlayer, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1f, 1f);
		mPlayer.playSound(mPlayer, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.PLAYERS, 0.8f, 1f);
		mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 16/15f);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 1f), 3);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 4/5f), 6);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.playSound(mPlayer, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 2/3f), 9);

		markDirty();
	}
}
