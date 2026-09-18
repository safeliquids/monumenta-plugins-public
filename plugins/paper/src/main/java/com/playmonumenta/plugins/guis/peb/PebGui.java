package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.lib.PagedFloweyGui;
import com.playmonumenta.plugins.utils.GUIUtils;
import org.bukkit.entity.Player;

public class PebGui extends PagedFloweyGui {
	public static final PagedFloweyGui.PageType MAIN_PAGE = new PagedFloweyGui.PageType("Main Page", 6 * 9);
	public static final PagedFloweyGui.PageType PLAYER_INFO_PAGE = new PagedFloweyGui.PageType("Player Information", 6 * 9);
	public static final PagedFloweyGui.PageType GAMEPLAY_OPTIONS_PAGE = new PagedFloweyGui.PageType("Gameplay Options", 6 * 9);
	public static final PagedFloweyGui.PageType TECHNICAL_OPTIONS_PAGE = new PagedFloweyGui.PageType("Technical Options", 6 * 9);
	public static final PagedFloweyGui.PageType TRADE_GUI_PAGE = new PagedFloweyGui.PageType("Trade Gui Settings", 6 * 9);
	public static final PagedFloweyGui.PageType INTERACTABLE_OPTIONS_PAGE = new PagedFloweyGui.PageType("Interactable Options", 6 * 9);
	public static final PagedFloweyGui.PageType SERVER_INFO_PAGE = new PagedFloweyGui.PageType("Server Information", 6 * 9);
	public static final PagedFloweyGui.PageType BOOK_SKINS_PAGE = new PagedFloweyGui.PageType("Book Skins", 6 * 9);
	public static final PagedFloweyGui.PageType PICKUP_AND_DISABLE_DROP_PAGE = new PagedFloweyGui.PageType("Pickup/Disable Drops", 6 * 9);
	public static final PagedFloweyGui.PageType GLOWING_PAGE = new PagedFloweyGui.PageType("Glowing Settings", 6 * 9);
	public static final PagedFloweyGui.PageType PARTIAL_PARTICLES_PAGE = new PagedFloweyGui.PageType("Particle Settings", 6 * 9);
	public static final PagedFloweyGui.PageType SOUND_CONTROLS_PAGE = new PagedFloweyGui.PageType("Sound Options", 6 * 9);
	public static final PagedFloweyGui.PageType SOUND_CATEGORIES_PAGE = new PagedFloweyGui.PageType("Sound Categories", 6 * 9);
	public static final PagedFloweyGui.PageType SOUND_OVERWORLD_PLOTS_PAGE = new PagedFloweyGui.PageType("Sound: Overworld/Plots", 6 * 9);

	public PebGui(Player player, PageType page) {
		super(player, GUIUtils.FILLER, page);
		registerPage(MAIN_PAGE, () -> new MainPage(this));
		registerPage(PLAYER_INFO_PAGE, () -> new PlayerInfoPage(this));
		registerPage(GAMEPLAY_OPTIONS_PAGE, () -> new GameplayOptionsPage(this));
		registerPage(TECHNICAL_OPTIONS_PAGE, () -> new TechnicalOptionsPage(this));
		registerPage(TRADE_GUI_PAGE, () -> new TradeGuiPage(this));
		registerPage(INTERACTABLE_OPTIONS_PAGE, () -> new InteractableOptionsPage(this));
		registerPage(SERVER_INFO_PAGE, () -> new ServerInfoPage(this));
		registerPage(BOOK_SKINS_PAGE, () -> new BookSkinsPage(this));
		registerPage(PICKUP_AND_DISABLE_DROP_PAGE, () -> new PickupAndDisableDropPage(this));
		registerPage(GLOWING_PAGE, () -> new GlowingOptionsPage(this));
		registerPage(PARTIAL_PARTICLES_PAGE, () -> new PartialParticlesPage(this));
		registerPage(SOUND_CONTROLS_PAGE, () -> new SoundOptionsPage(this));
		registerPage(SOUND_CATEGORIES_PAGE, () -> new SoundCategoriesPage(this));
		registerPage(SOUND_OVERWORLD_PLOTS_PAGE, () -> new SoundOverworldPlotsPage(this));
	}

	public PebGui(Player player) {
		this(player, MAIN_PAGE);
	}
}
