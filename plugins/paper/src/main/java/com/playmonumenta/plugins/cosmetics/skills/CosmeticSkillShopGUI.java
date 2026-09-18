package com.playmonumenta.plugins.cosmetics.skills;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.cosmetics.gui.CosmeticsGUI;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class CosmeticSkillShopGUI extends Gui {

	private static final List<Component> DEPTHS_INTRO;
	private static final List<Component> DELVE_INTRO;
	private static final List<Component> PRESTIGE_INTRO;
	private static final List<Component> SANGUINE_INTRO;
	private static final List<Component> HEXFALL_INTRO;
	private static final List<Component> INTRUDER_INTRO;

	//GUI constants
	private static final Material LOCKED = Material.BARRIER;
	private static final int LINE = 5;
	private static final int INTRO_LOC = 4;
	private static final int ENTRY_START = 9;
	private static final int[] ENTRY_COLUMNS = {0, 1, 2, 3, 5, 6, 7, 8};
	private static final int ENTRY_PER_LINE = ENTRY_COLUMNS.length;
	private static final int BACK_LOC = (LINE - 1) * 9 + 4;

	private static final int DEPTHS_ENTRY_LOC = 20;
	private static final int DELVE_ENTRY_LOC = 21;
	private static final int PRESTIGE_ENTRY_LOC = 22;
	private static final int GALLERY_ENTRY_LOC = 23;
	private static final int HEXFALL_ENTRY_LOC = 24;
	private static final int INTRUDER_ENTRY_LOC = 25;

	private @Nullable CSSet mCurrentPage = null;
	private boolean mCosmeticSkillChanged = false;

	public enum CSSet {
		DEPTHS(Component.text("Darkest Depths"), Component.text("Da").append(Component.text("rkest Dep").decorate(TextDecoration.OBFUSCATED)).append(Component.text("ths")),
			DEPTHS_INTRO, List.of(Component.text("Complete ").append(Component.text("Darke").decorate(TextDecoration.OBFUSCATED)).append(Component.text("st D").append(Component.text("epth").decorate(TextDecoration.OBFUSCATED).append(Component.text("s")))), Component.text("to unlock this theme!")),
			Constants.Colors.DARKEST_DEPTHS, Material.BLACKSTONE, Constants.Objectives.DARKEST_DEPTHS
		),

		DELVE(Component.text("Dungeon Delves"), Component.text("Dungeon D").decorate(TextDecoration.OBFUSCATED).append(Component.text("elves")),
			DELVE_INTRO, List.of(Component.text("Complete Monument of King's Valley"), Component.text("to unlock this theme!")),
			Constants.Colors.DUNGEON_DELVES, Material.NETHERITE_BLOCK, Constants.Objectives.R1_MONUMENT
		),

		PRESTIGE(Component.text("Prestige Hall"), Component.text("Chall").decorate(TextDecoration.OBFUSCATED).append(Component.text("enge Delves")),
			PRESTIGE_INTRO, List.of(Component.text("Complete Monument of King's Valley"), Component.text("to unlock this theme!")),
			Constants.Colors.CHALLENGE_DELVES, Material.GOLD_BLOCK, Constants.Objectives.R1_MONUMENT
		),

		SANGUINE(Component.text("Sanguine Halls"), Component.text("MzkCaerulaArbor").decorate(TextDecoration.OBFUSCATED),
			SANGUINE_INTRO, List.of(Component.text("Reveal the secret b").append(Component.text("eneth the ocean").decorate(TextDecoration.OBFUSCATED)).append(Component.text("s")), Component.text("to unlock this theme!")),
			Constants.Colors.SANGUINE_HALLS, Material.WAXED_OXIDIZED_COPPER, Constants.Objectives.GALLERY_OF_FEAR_ACCESS
		),

		HEXFALL(Component.text("Hexfall"), Component.text("H").append(Component.text("exfall").decorate(TextDecoration.OBFUSCATED)),
			HEXFALL_INTRO, List.of(Component.text("Defeat the d").append(Component.text("ryad in t").decorate(TextDecoration.OBFUSCATED)).append(Component.text("he Sa")).append(Component.text("nctum of Sunken Fa").decorate(TextDecoration.OBFUSCATED)).append(Component.text("ith")), Component.text("to unlock this theme!")),
			Constants.Colors.HEXFALL, Material.MOSSY_STONE_BRICKS, Constants.Objectives.HEXFALL
		),

		INTRUDER(Component.text("Twisted ").append(Component.text("lxxxxxxx").decorate(TextDecoration.OBFUSCATED)), Component.text("Twisted lxxxxxxx").decorate(TextDecoration.OBFUSCATED),
			INTRUDER_INTRO, List.of(MessagingUtils.fromMiniMessage("Defeat the Tw<obfuscated>isted lxxxxx</obfuscated>r in you<obfuscated>r own mind</obfuscated>"), Component.text("to unlock this theme!")),
			Constants.Colors.TWISTED_INTRUDER, Material.BLACK_GLAZED_TERRACOTTA, Constants.Objectives.TWISTED_INTRUDER
		),
		;

		final Component mName; // Color gets applied to this later, only other styling is needed
		final Component mLockedName;
		final List<Component> mIntro;
		final List<Component> mLockedDescription;
		final TextColor mColor;
		final Material mMat;
		final String mScoreboard;

		CSSet(Component name, Component lockedName, List<Component> intro, List<Component> lockedDescription, TextColor color, Material mat, String scoreboard) {
			mName = name;
			mLockedName = lockedName;
			mColor = color;
			mMat = mat;
			mIntro = intro;
			mLockedDescription = lockedDescription.stream().map(c -> c.color(color)).toList();
			mScoreboard = scoreboard;
		}

		List<BuyableCS> getCosmeticSkills() {
			List<BuyableCS> list = new ArrayList<>();
			for (Supplier<CosmeticSkill> constructor : CosmeticSkills.getCosmeticSkills()) {
				if (constructor.get() instanceof BuyableCS buyable && buyable.getSet() == this) {
					list.add(buyable);
				}
			}
			return list;
		}
	}

	public CosmeticSkillShopGUI(Player player) {
		super(player, 9 * LINE, Component.text("Cosmetic Skill Shop", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
	}

	@Override
	public void setup() {
		if (mCurrentPage == null) {
			// Intro item
			ItemStack introItem = createPageIcon(Material.RED_GLAZED_TERRACOTTA, Component.text("Theme Selection", NamedTextColor.RED), List.of(Component.text("Select a theme to buy", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false), Component.text("cosmetic skills!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)));
			setItem(INTRO_LOC, introItem);

			setPageIcon(DEPTHS_ENTRY_LOC, CSSet.DEPTHS);
			setPageIcon(DELVE_ENTRY_LOC, CSSet.DELVE);
			setPageIcon(PRESTIGE_ENTRY_LOC, CSSet.PRESTIGE);
			setPageIcon(GALLERY_ENTRY_LOC, CSSet.SANGUINE);
			setPageIcon(HEXFALL_ENTRY_LOC, CSSet.HEXFALL);
			setPageIcon(INTRUDER_ENTRY_LOC, CSSet.INTRUDER);

			// Back item
			setBackItem("Back to Cosmetic Manager");
		} else {
			ItemStack introItem = createIntroItem(mCurrentPage);
			setItem(INTRO_LOC, introItem);

			List<BuyableCS> skills = mCurrentPage.getCosmeticSkills();
			int size = skills.size();
			int perColumn = Math.max(size / ENTRY_PER_LINE, 1);
			for (int i = 0; i < size; i++) {
				int slot = ENTRY_START + ENTRY_COLUMNS[(i / perColumn) % ENTRY_PER_LINE] + (i % perColumn) * 9;
				BuyableCS skin = skills.get(i);
				List<String> price = skin.getCostDescription();
				String name = skin.getName();
				if (name == null) {
					// Should be impossible
					continue;
				}
				ItemStack item = createSkillIcon(name, mCurrentPage.mColor, price);
				boolean locked = item.getType() == LOCKED;
				setItem(slot, new GuiItem(item).onClick(e -> {
					if (!locked && !CosmeticsManager.getInstance().playerHasCosmetic(mPlayer, CosmeticType.COSMETIC_SKILL, name)) {
						// Try to buy
						if (mPlayer.getGameMode() == GameMode.CREATIVE) {
							buyCosmetic(name);
							mPlayer.sendMessage(Component.text("Because you are in creative mode, this is free!", NamedTextColor.GREEN));
							return;
						}

						if (skin.attemptPurchase(mPlayer)) {
							buyCosmetic(name);
						}
					} else {
						// Already bought
						mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 1);
						mPlayer.sendMessage(Component.text("You already have this cosmetic skill. Go to Cosmetic Manager to equip it!", NamedTextColor.RED));
					}
				}));
			}

			// Back item
			setBackItem("Back to Overview");
		}
	}

	private void buyCosmetic(String skin) {
		if (CosmeticsManager.getInstance().addCosmetic(mPlayer, CosmeticType.COSMETIC_SKILL, skin, true)) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1, 1.5f);
			mPlayer.sendMessage(Component.text("You successfully bought " + skin + "! It has been automatically equipped.", NamedTextColor.GREEN));
			update();
			mCosmeticSkillChanged = true;
		} else {
			// Shouldn't be here! But leave it as a handler to avoid typo in code.
			mPlayer.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 1, 0.5f);
			mPlayer.sendMessage(Component.text("EX[" + skin + "]2: An exception occurred when buying cosmetic skill. Contact a moder or dev with this message to report if you believe this is a bug.", NamedTextColor.DARK_RED));
			close();
		}
	}

	private ItemStack createIntroItem(CSSet set) {
		List<Component> intro = new ArrayList<>(set.mIntro);
		if (set == CSSet.PRESTIGE) {
			intro.add(Component.text("Challenge Points: " + ScoreboardUtils.getScoreboardValue(mPlayer, PrestigeCS.CHALLENGE_POINTS_SCOREBOARD).orElse(0)).style(INTRO_STYLE));
		}
		return createPageIcon(set.mMat, set.mName.color(set.mColor), intro);
	}

	private void setBackItem(String itemName) {
		ItemStack item = new ItemStack(Material.REDSTONE_BLOCK, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text(itemName, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		item.setItemMeta(meta);
		setItem(BACK_LOC, new GuiItem(item).onClick(e -> {
			if (mCurrentPage != null) {
				mCurrentPage = null;
				update();
			} else {
				close();
				new CosmeticsGUI(mPlugin, mPlayer).openInventory(mPlayer, mPlugin);
			}
		}));
	}

	private void setPageIcon(int location, CSSet set) {
		GuiItem item;
		if (ScoreboardUtils.getScoreboardValue(mPlayer, set.mScoreboard).orElse(0) > 0 || mPlayer.getGameMode() == GameMode.CREATIVE) {
			item = new GuiItem(createPageIcon(set.mMat, set.mName.color(set.mColor), set.mIntro)).onClick(e -> {
				mCurrentPage = set;
				mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.5f, 1f);
				update();
			});
		} else {
			item = new GuiItem(createPageIcon(LOCKED, set.mLockedName.color(set.mColor), set.mLockedDescription));
		}
		setItem(location, item);
	}

	private ItemStack createPageIcon(Material icon, Component name, List<Component> desc) {
		return createBasicItem(icon, name, desc, new ArrayList<>());
	}

	private ItemStack createSkillIcon(String skin, TextColor color, List<String> price) {
		CosmeticSkill skill = CosmeticSkills.getCosmeticSkill(skin);
		if (skill == null) {
			return new ItemStack(Material.BARRIER);
		}
		List<String> desc = new ArrayList<>();
		desc.add("Cosmetic " + skill.getAbilityName());
		if (skill instanceof LockableCS lockable && !lockable.isUnlocked(mPlayer)) {
			// Locked skin, show lock description to give info
			return createBasicItem(LOCKED, skin, color, desc, NamedTextColor.RED, lockable.getLockDesc());
		}

		Cosmetic cosmetic = CosmeticSkills.getCosmeticByName(skin);
		String[] extraLore = new String[0];
		// Unlocked skin, show extra lore
		if (cosmetic != null && cosmetic.getDescription() != null) {
			extraLore = cosmetic.getDescription();
		}
		Cosmetic skillCosmetic = skill.getCosmetic();
		if (skillCosmetic != null && CosmeticsManager.getInstance().playerHasCosmetic(mPlayer, CosmeticType.COSMETIC_SKILL, skillCosmetic.getName())) {
			// attach
			desc.add("Owned");
		} else {
			// attach price
			desc.addAll(price);
		}
		return createBasicItem(skill.getDisplayItem(), skin, color, desc, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, List<String> desc, String... extraLore) {
		return createBasicItem(mat, name, nameColor, desc, NamedTextColor.DARK_GRAY, extraLore);
	}

	private ItemStack createBasicItem(Material mat, String name, TextColor nameColor, List<String> desc, TextColor extraColor, String... extraLore) {
		return createBasicItem(mat, Component.text(name, nameColor), desc, extraColor, extraLore);
	}

	private ItemStack createBasicItem(Material mat, Component name, List<String> desc, TextColor extraColor, String... extraLore) {
		return createBasicItem(mat, name,
			desc.stream().map(s -> Component.text(s, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)).toList(),
			Arrays.stream(extraLore).map(s -> Component.text(s, extraColor).decoration(TextDecoration.ITALIC, false)).toList());
	}

	private ItemStack createBasicItem(Material mat, Component name, List<? extends Component> desc, List<? extends Component> extraLore) {
		ItemStack item = new ItemStack(mat, 1);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, true));
		List<Component> lore = new ArrayList<>(desc);
		lore.addAll(extraLore);
		meta.lore(lore);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	protected void onClose(InventoryCloseEvent event) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (mCosmeticSkillChanged) {
				mPlugin.mAbilityManager.updatePlayerAbilities(mPlayer, false);
			}
		}, 2);
	}

	private static final Style INTRO_STYLE = Style.style(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);

	/**
	 * "Styles" a list of components for use in the intros.
	 *
	 * @param components The list of components.
	 * @return The styled components.
	 */
	private static List<Component> applyIntroStyle(String... components) {
		List<Component> comps = new ArrayList<>();
		for (String s : components) {
			comps.add(Component.text(s, INTRO_STYLE));
		}
		return ImmutableList.copyOf(comps);
	}

	static {
		DEPTHS_INTRO = applyIntroStyle(
			"Attuned with powers from",
			"Darkest Depths trees."
		);
		DELVE_INTRO = applyIntroStyle(
			"Essences of the twisted contingency,",
			"rewards for heroic adventurers."
		);
		PRESTIGE_INTRO = applyIntroStyle(
			"Every single step here is",
			"a witness of your prestige and glory."
		);
		SANGUINE_INTRO = applyIntroStyle(
			"The nightmare was never meant for life.",
			"Banish the dream.",
			"End this nightmare!"
		);
		HEXFALL_INTRO = applyIntroStyle(
			"Hycenea's grasp on the Wolfswood has vanished.",
			"The residue of her magic now calls for you to control."
		);
		INTRUDER_INTRO = ImmutableList.of(
			MessagingUtils.fromMiniMessage("<yellow>Warped by the <obfuscated>lxxxxxxx</obfuscated>'s influence.")
		);
	}


}
