package com.playmonumenta.plugins.events;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent.DamageModifier.Stage;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageEvent extends Event implements Cancellable {

	public enum DamageType {
		MELEE(1, true, "Melee", "🗡"),
		MELEE_SKILL(1, true, "Melee Skill", "🗡"),
		MELEE_ENCH(1, true, "Melee Enchantment", "🗡"),
		PROJECTILE(1, true, "Projectile", "🏹"),
		PROJECTILE_SKILL(1, true, "Projectile Skill", "🏹"),
		PROJECTILE_ENCH(1, true, "Projectile Enchantment", "🏹"),
		MAGIC(1, true, "Magic", "⭐"),
		OTHER(1, true, "Other", ""),
		THORNS(1, true, "Thorns", "\uD83C\uDF35"),
		BLAST(1, true, "Blast", "\uD83D\uDCA5"),
		FIRE(0.5, true, "Fire", "\uD83D\uDD25"),
		FALL(0.5, false, "Fall", "☠"),
		AILMENT(0, true, "Ailment", "☠"),
		TRUE(0, false, "True", ""),
		UNSCALABLE(0, false, "Unscalable", ""),
		UNSCALABLE_SKILL(0, false, "Unscalable Skill", ""),
		UNSCALABLE_ENCH(0, false, "Unscalable Enchantment", ""),
		;

		public static DamageType getType(DamageCause cause) {
			// List every cause for completeness
			return switch (cause) {
				case WORLD_BORDER, CONTACT, MELTING, DROWNING, STARVATION, LIGHTNING, FALLING_BLOCK, CUSTOM, DRYOUT,
				     FREEZE, CRAMMING, SONIC_BOOM, SUFFOCATION -> UNSCALABLE;
				case ENTITY_ATTACK -> MELEE;
				case ENTITY_SWEEP_ATTACK -> MELEE_ENCH;
				case PROJECTILE -> PROJECTILE;
				case DRAGON_BREATH, MAGIC -> MAGIC;
				case THORNS -> THORNS;
				case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> BLAST;
				case FIRE, FIRE_TICK, HOT_FLOOR, LAVA -> FIRE;
				case FALL, FLY_INTO_WALL -> FALL;
				case POISON, WITHER -> AILMENT;
				case VOID, KILL, SUICIDE -> TRUE;
				// we should log an error on default, this makes porting easier since any new damage types added will
				// automatically lead to a stacktrace
				default -> {
					MMLog.warning("Unknown/new damage type: " + cause);
					yield OTHER;
				}
			};
		}

		public static boolean is(DamageCause cause, DamageType type) {
			return getType(cause) == type;
		}

		private final double mDefenseModifier;
		private final boolean mIsScalable;
		private final String mDisplay;
		private final String mSymbol;

		DamageType(double defenseModifier, boolean isScalable, String display, String symbol) {
			mDefenseModifier = defenseModifier;
			mIsScalable = isScalable;
			mDisplay = display;
			mSymbol = symbol;
		}


		public boolean isDefendable() {
			return mDefenseModifier > 0;
		}

		public double getDefenseModifier() {
			return mDefenseModifier;
		}

		public boolean isScalable() {
			return mIsScalable;
		}

		public String getDisplay() {
			return mDisplay;
		}

		public String getSymbol() {
			return mSymbol;
		}

		public static EnumSet<DamageType> getEnumSet() {
			return EnumSet.allOf(DamageType.class);
		}

		public static EnumSet<DamageType> getNonTrueTypes() {
			EnumSet<DamageType> enumSet = getEnumSet();
			enumSet.remove(TRUE);
			return enumSet;
		}

		public static EnumSet<DamageType> getScalableDamageType() {
			EnumSet<DamageType> enumSet = getEnumSet();
			enumSet.removeIf(damageType -> !damageType.isScalable());
			return enumSet;
		}

		public static EnumSet<DamageType> getUnscalableDamageType() {
			EnumSet<DamageType> enumSet = getEnumSet();
			enumSet.removeIf(DamageType::isScalable);
			return enumSet;
		}

		public static EnumSet<DamageType> getAllMeleeTypes() {
			return EnumSet.of(MELEE, MELEE_ENCH, MELEE_SKILL);
		}

		public static EnumSet<DamageType> getAllProjectileTypes() {
			return EnumSet.of(PROJECTILE, PROJECTILE_SKILL, PROJECTILE_ENCH);
		}

		public static EnumSet<DamageType> getAllMagicTypes() {
			// Might create MAGIC_ENCH to handle Trivium properly in the future, not this PR though
			return EnumSet.of(MAGIC);
		}

		public static EnumSet<DamageType> getAllMeleeAndProjectileTypes() {
			EnumSet<DamageType> enumSet = getAllMeleeTypes();
			enumSet.addAll(getAllProjectileTypes());
			return enumSet;
		}

		public static EnumSet<DamageType> getAllProjectileAndMagicTypes() {
			EnumSet<DamageType> enumSet = getAllProjectileTypes();
			enumSet.addAll(getAllMagicTypes());
			return enumSet;
		}

		public static EnumSet<DamageType> getAllMeleeProjectileAndMagicTypes() {
			EnumSet<DamageType> enumSet = getAllMeleeAndProjectileTypes();
			enumSet.addAll(getAllMagicTypes());
			return enumSet;
		}
	}

	public record DamageModifier(double modifier, boolean multiplicative, Stage stage,
								 EnumSet<DamageType> damageTypes) {
		public enum Stage {
			// NOTE: ordinal (definition order) decides priority!
			BASE(false),
			GEAR(false),
			EFFECT_POSITIVE(false),
			EFFECT_NEGATIVE(false),
			CRITICAL(false),
			FINAL(true),

			;
			private final boolean mMultiplicativeModifier;

			Stage(boolean multiplicativeModifier) {
				mMultiplicativeModifier = multiplicativeModifier;
			}

			public boolean isMultiplicativeModifier() {
				return mMultiplicativeModifier;
			}
		}

		public static DamageModifier add(double add, Stage stage, EnumSet<DamageType> damageTypes) {
			return new DamageModifier(add, false, stage, damageTypes);
		}

		public static DamageModifier add(double add, Stage stage) {
			return new DamageModifier(add, false, stage, EnumSet.allOf(DamageType.class));
		}

		public static DamageModifier percent(double add, Stage stage, EnumSet<DamageType> damageTypes) {
			return new DamageModifier(add, true, stage, damageTypes);
		}

		public static DamageModifier percent(double add, Stage stage) {
			return new DamageModifier(add, true, stage, EnumSet.allOf(DamageType.class));
		}

		@Override
		public @NotNull String toString() {
			return "modifier=%.2f, multiplicative=%s, stage=%s, simpleTypes=%s,\ndetailedTypes=%s".formatted(
				modifier, multiplicative, stage, damageTypes.stream().map(DamageType::getSymbol).distinct().collect(Collectors.joining()), damageTypes
			);
		}
	}

	public static class Metadata {

		private DamageType mType;
		private @Nullable ClassAbility mAbility;
		private final @Nullable ItemStatManager.PlayerItemStats mPlayerItemStats;
		private final @Nullable String mBossSpellName;

		public Metadata(DamageType type, @Nullable ClassAbility ability) {
			this(type, ability, null);
		}

		public Metadata(DamageType type, @Nullable ClassAbility ability, @Nullable ItemStatManager.PlayerItemStats playerItemStats) {
			this(type, ability, playerItemStats, null);
		}

		public Metadata(DamageType type, @Nullable ClassAbility ability, @Nullable ItemStatManager.PlayerItemStats playerItemStats, @Nullable String bossSpellName) {
			if (type == null) {
				mType = DamageType.OTHER;
				MMLog.warning("Attempted to construct DamageEvent with null DamageType");
			} else {
				mType = type;
			}

			mAbility = ability;
			mPlayerItemStats = playerItemStats;
			mBossSpellName = bossSpellName;
		}

		public DamageType getType() {
			return mType;
		}

		public void setType(DamageType type) {
			mType = type;
		}

		public @Nullable String getBossSpellName() {
			return mBossSpellName;
		}
	}

	private final LivingEntity mDamagee;
	private final @Nullable Entity mDamager;
	private final @Nullable LivingEntity mSource;
	private final EntityDamageEvent mEvent;
	private final Metadata mMetadata;

	private boolean mLifelineCancel;

	private final double mOriginalDamage;
	private final Multimap<Stage, DamageModifier> mDamageModifiers;
	private @Nullable Double mDamageCap = null;
	private boolean mIsCrit = false;

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee) {
		this(event, damagee, DamageType.getType(event.getCause()));
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type) {
		this(event, damagee, type, null);
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, DamageType type, @Nullable ClassAbility ability) {
		this(event, damagee, new Metadata(type, ability));
	}

	public DamageEvent(EntityDamageEvent event, LivingEntity damagee, Metadata metadata) {
		mDamagee = damagee;
		mDamager = event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent ? entityDamageByEntityEvent.getDamager() : null;
		mMetadata = metadata;
		mOriginalDamage = event.getDamage();
		mDamageModifiers = Multimaps.newMultimap(new EnumMap<>(Stage.class), ArrayList::new);
		mEvent = event;
		mLifelineCancel = false;
		setBaseDamage(event.getDamage());

		if (mDamager instanceof Projectile proj) {
			ProjectileSource source = proj.getShooter();
			if (source instanceof LivingEntity le) {
				mSource = le;
			} else {
				mSource = null;
			}
		} else if (mDamager instanceof EvokerFangs fangs) {
			mSource = fangs.getOwner();
			mMetadata.setType(DamageType.MAGIC);
		} else if (mDamager instanceof LivingEntity le) {
			mSource = le;
		} else {
			mSource = null;
		}
	}

	public void addDamageModifier(DamageModifier damageModifier) {
		if (!damageModifier.damageTypes.contains(mMetadata.getType())) {
			return;
		}
		mDamageModifiers.put(damageModifier.stage(), damageModifier);
		mEvent.setDamage(recalculateDamage(null));
	}

	public ImmutableMultimap<Stage, DamageModifier> damageModifiersCopy() {
		return ImmutableMultimap.copyOf(mDamageModifiers);
	}

	public double getDamage() {
		return getDamage(null);
	}


	/**
	 * Gets the damage Monumenta tries to do to the entity, excludes iframes
	 * @param excludedType      Excludes damage modifiers if it also buffs this damage type
	 * @return The final damage that will be dealt
	 */
	public double getDamage(@Nullable DamageType excludedType) {
		return recalculateDamage(excludedType);
	}

	public double getFinalDamage(boolean includeAbsorption) {
		return getFinalDamage(includeAbsorption, null);
	}

	/**
	 * Gets the final damage that will be dealt by this damage event, if the damage were to happen right now.
	 * To make sure to get the actual final damage, set the priority the ability or attribute using this sufficiently high.
	 * This also means that this method should not be used by low-priority handlers.
	 *
	 * @param includeAbsorption Whether to deduct existing absorption from the result (same behaviour as {@link EntityDamageEvent#getFinalDamage()}),
	 *                          useful to check if an attack would be lethal
	 * @param excludedType      Excludes damage modifiers if it also buffs this damage type
	 * @return The final damage that will be dealt
	 */
	// Bukkit deprecates EntityDamageEvent.DamageModifier
	@SuppressWarnings("deprecation")
	public double getFinalDamage(boolean includeAbsorption, @Nullable DamageType excludedType) {
		recalculateDamage(excludedType);

		if (includeAbsorption) {
			return Math.max(0, mEvent.getFinalDamage());
		} else {
			return Math.max(0, mEvent.getFinalDamage() - mEvent.getDamage(EntityDamageEvent.DamageModifier.ABSORPTION));
		}
	}

	public double getOriginalDamage() {
		return mOriginalDamage;
	}

	/**
	 * Calculates damage for various purposes.
	 *
	 * @param excludedType used for on hit damages to prevent double-dipping effects. (does not add modifier if it also buffs this damage type)
	 */
	private double calculateRawDamage(@Nullable DamageType excludedType) {
		double damage = 0;
		Stage[] values = Stage.values(); // this is in order of declaration!!

		for (Stage stage : values) {
			double add = 0;
			double multiplier = 1;
			double multiplicativeMultiplier = 1;
			for (DamageModifier damageModifier : mDamageModifiers.get(stage)) {
				EnumSet<DamageType> damageTypes = damageModifier.damageTypes;
				if (excludedType == null || !damageTypes.contains(excludedType)) {
					if (damageModifier.multiplicative) {
						if (damageModifier.modifier <= 1 || stage.isMultiplicativeModifier()) {
							multiplicativeMultiplier *= damageModifier.modifier;
						} else {
							multiplier += Math.max(damageModifier.modifier - 1, 0);
						}
					} else {
						add += damageModifier.modifier;
					}
				}
			}
			damage += add;
			damage *= multiplier * multiplicativeMultiplier;
		}

		return damage;
	}

	/**
	 * Calculates damage for lightning totem temporarily.
	 * @param lastStage    calculates all stages up till and including this stage
	 * @param excludedType used for on hit damages to prevent double-dipping effects. (does not add modifier if it also buffs this damage type)
	 */
	public double calculateDamageUpTo(Stage lastStage, @Nullable DamageType excludedType) {
		double damage = 0;
		Stage[] values = Stage.values(); // this is in order of declaration!!

		for (Stage stage : values) {
			double add = 0;
			double multiplier = 1;
			double multiplicativeMultiplier = 1;
			for (DamageModifier damageModifier : mDamageModifiers.get(stage)) {
				EnumSet<DamageType> damageTypes = damageModifier.damageTypes;
				if (excludedType == null || !damageTypes.contains(excludedType)) {
					if (damageModifier.multiplicative) {
						if (damageModifier.modifier <= 1 || stage.isMultiplicativeModifier()) {
							multiplicativeMultiplier *= damageModifier.modifier;
						} else {
							multiplier += Math.max(damageModifier.modifier - 1, 0);
						}
					} else {
						add += damageModifier.modifier;
					}
				}
			}
			damage += add;
			damage *= multiplier * multiplicativeMultiplier;
			if (stage == lastStage) {
				break;
			}
		}

		return damage;
	}

	private Pair<Double, Double> getStageDamage(Stage stage) {
		double add = 0;
		double multiplier = 1;
		for (DamageModifier damageModifier : mDamageModifiers.get(stage)) {
			if (damageModifier.multiplicative) {
				multiplier += Math.max(damageModifier.modifier - 1, 0);
			} else {
				add += damageModifier.modifier;
			}
		}
		return Pair.of(add, multiplier);
	}

	static final int DAMAGE_CAP = 1000000;
	static final int DAMAGE_WARN = 10000;
	private boolean mHasBeenWarned = false;
	private final UUID mEventIdentifier = UUID.randomUUID();

	private double recalculateDamage(@Nullable DamageType excludedType) {
		// Never set damage above 1000000 (arbitrary high amount) so that it doesn't go over the limit of what can actually be dealt
		double damage = Math.min(calculateRawDamage(excludedType), DAMAGE_CAP);

		if (mDamageCap != null) {
			damage = Math.min(damage, mDamageCap);
		}
		// Log big warning because damage is too high
		if (damage >= DAMAGE_WARN) {
			damageCapWarn(damage);
		}
		if (getCause() == DamageCause.POISON && mDamagee instanceof Player && mDamagee.getHealth() - damage <= 0) {
			mEvent.setDamage(Math.max(mDamagee.getHealth() - 1, 0));
		} else {
			mEvent.setDamage(damage);
		}
		return damage;
	}

	private void damageCapWarn(double damage) {
		if (!(mSource instanceof Player player)) {
			return;
		}
		if (!mHasBeenWarned) {
			mHasBeenWarned = true;
			final var inventory = player.getInventory();
			// grab current charms
			final var charms = CharmManager.getInstance().getCharms(player, CharmManager.getInstance().mEnabledCharmType);
			final var charmNames = new ArrayList<>();
			if (charms != null && !charms.isEmpty()) {
				for (final var charm : charms) {
					charmNames.add(ItemUtils.getPlainName(charm));
				}
			}
			final var charmString = charmNames.isEmpty() ? "" : String.join(",", charmNames.toArray(new String[0]));
			String equipment;
			try {
				equipment = String.join(",", "mainhand=" + ItemUtils.getPlainName(inventory.getItemInMainHand()), "offhand=" + ItemUtils.getPlainName(inventory.getItemInOffHand()), "helmet=" + ItemUtils.getPlainName(inventory.getHelmet()), "chestplate=" + ItemUtils.getPlainName(inventory.getChestplate()), "leggings=" + ItemUtils.getPlainName(inventory.getLeggings()), "boots=" + ItemUtils.getPlainName(inventory.getBoots()), "charms=[" + charmString + "]");
			} catch (Exception ex) {
				equipment = "error";
			}
			final String string = String.join(" ", "Player dealt damage higher than " + DAMAGE_WARN, "[" + String.join(",", "player=" + player.getName(), "damage=" + damage, "originalDamage=" + mOriginalDamage, "damageEventId=" + mEventIdentifier, "equipment=[" + equipment + "]") + "]");
			// now craft the stacktrace
			MMLog.severe(() -> string + parseStackTracesFromMonumentaPlugin());
			AuditListener.logPlayer(string);
			return;
		}
		MMLog.severe(() -> "Player: " + player.getName() + " excceded damage cap! [damageEventId=" + mEventIdentifier + ",damage=" + damage + "]");
	}

	private static final Set<String> IGNORED_CLAZZ = Set.of(
		"DamageEvent",
		"DamageListener",
		"DamageUtils"
	);

	private static String parseStackTracesFromMonumentaPlugin() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		var s = "\n";
		for (StackTraceElement stackTraceElement : stackTrace) {
			String className = stackTraceElement.getClassName();
			if (!className.contains("com.playmonumenta")) {
				continue;
			}

			var founcIgnored = false;
			for (var ignoredClass : IGNORED_CLAZZ) {
				if (className.contains(ignoredClass)) {
					founcIgnored = true;
					break;
				}
			}
			if (founcIgnored) {
				continue;
			}

			s = s + stackTraceElement + "\n";
		}
		return s;
	}

	public void setBaseDamage(double damage) {
		if (damage < 0) {
			MMLog.debug("Negative damage dealt: " + damage, new Exception());
		}
		if (!Double.isFinite(damage)) {
			MMLog.warning("Non-finite damage dealt: " + damage, new Exception());
			damage = 0;
		}

		// In case something goes horribly wrong, log the stack trace when set to finest
		mDamageModifiers.removeAll(Stage.BASE);
		addDamageModifier(DamageModifier.add(damage, Stage.BASE, EnumSet.of(mMetadata.getType())));
		MMLog.trace(() -> Arrays.toString(Thread.currentThread().getStackTrace()));
	}

	public void addBaseDamage(double damage) {
		addDamageModifier(DamageModifier.add(damage, Stage.BASE, EnumSet.of(mMetadata.getType())));
	}

	public void addFinalDamage(double damage, EnumSet<DamageType> damageTypes) {
		addDamageModifier(DamageModifier.add(damage, Stage.FINAL, damageTypes));
	}

	public void updateDamageWithMultiplier(double damageMultiplier, EnumSet<DamageType> damageTypes) {
		if (damageMultiplier < 0) {
			MMLog.debug("Negative damage multiplier: " + damageMultiplier, new Exception());
		}
		if (damageMultiplier > 1) {
			// Accumulate damage multiplier (Additively)
			addDamageModifier(DamageModifier.percent(damageMultiplier, Stage.EFFECT_POSITIVE, damageTypes));
		} else {
			// Accumulate weakness / reduction multiplier (Multiplicatively)
			addDamageModifier(DamageModifier.percent(damageMultiplier, Stage.EFFECT_NEGATIVE, damageTypes));
		}
	}

	public void updateGearDamageWithMultiplier(double damageGearMultiplier, EnumSet<DamageType> damageTypes) {
		if (damageGearMultiplier < 0) {
			MMLog.debug("Negative damage multiplier: " + damageGearMultiplier, new Exception());
		}
		// Accumulate damage multiplier
		addDamageModifier(DamageModifier.percent(damageGearMultiplier, Stage.GEAR, damageTypes));
	}

	public void updateFinalMultiplier(double damageMultiplier) {
		addDamageModifier(DamageModifier.percent(damageMultiplier, Stage.FINAL, EnumSet.of(mMetadata.getType())));
	}


	public double getBaseDamage() {
		return getStageDamage(Stage.BASE).first();
	}

	public double getGearDamageMultiplier() {
		return getStageDamage(Stage.GEAR).second();
	}

	public double getEffectDamage() {
		return getStageDamage(Stage.EFFECT_POSITIVE).second();
	}

	public double getWeaknessMultiplier() {
		return getStageDamage(Stage.EFFECT_NEGATIVE).second();
	}

	public double getResistanceMultiplier() {
		return getStageDamage(Stage.FINAL).second();
	}

	public void setIsCrit(boolean crit) {
		if (mIsCrit && !crit) {
			mDamageModifiers.removeAll(Stage.CRITICAL);
		} else if (!mIsCrit && crit) {
			mDamageModifiers.put(Stage.CRITICAL, DamageModifier.percent(1.5, Stage.CRITICAL, EnumSet.of(DamageType.MELEE)));
		}
		mIsCrit = crit;
	}

	public boolean getIsCrit() {
		return mIsCrit;
	}

	// Will override an existing cap!
	public void setDamageCap(@Nullable Double cap) {
		mDamageCap = cap;
		recalculateDamage(null);
	}

	public @Nullable Double getDamageCap() {
		return mDamageCap;
	}

	public DamageType getType() {
		return mMetadata.mType;
	}

	public void setType(DamageType newType) {
		mMetadata.mType = newType;
	}

	public @Nullable ClassAbility getAbility() {
		return mMetadata.mAbility;
	}

	public void setAbility(ClassAbility ability) {
		mMetadata.mAbility = ability;
	}

	public @Nullable String getBossSpellName() {
		return mMetadata.mBossSpellName;
	}

	public LivingEntity getDamagee() {
		return mDamagee;
	}

	public @Nullable Entity getDamager() {
		return mDamager;
	}

	public @Nullable LivingEntity getSource() {
		return mSource;
	}

	@Override
	public boolean isCancelled() {
		return mEvent.isCancelled();
	}

	@Override
	public void setCancelled(boolean cancelled) {
		mEvent.setCancelled(cancelled);
	}

	public void setLifelineCancel(boolean lifelineCancel) {
		mLifelineCancel = lifelineCancel;
	}

	public boolean isLifelineCancel() {
		return mLifelineCancel;
	}

	public @Nullable ItemStatManager.PlayerItemStats getPlayerItemStats() {
		return mMetadata.mPlayerItemStats;
	}

	public EntityDamageEvent getEvent() {
		return mEvent;
	}

	/**
	 * Use getType() in most scenarios - this is just if differentiation is needed within the type.
	 * This will return {@link DamageCause#CUSTOM} for any custom damage dealt (e.g. by abilities).
	 *
	 * @return The original damage cause of the {@link EntityDamageEvent}
	 */
	public DamageCause getCause() {
		return mEvent.getCause();
	}

	/**
	 * Returns whether the event deals 0 damage (e.g. blocked by a shield, iframes, resistance, etc.)
	 */
	public boolean isBlocked() {
		return isBlockedByShield() || getFinalDamage(false) <= 0;
	}

	/**
	 * Returns whether the damage is blocked by a shield
	 */
	// Bukkit deprecates EntityDamageEvent.DamageModifier
	@SuppressWarnings("deprecation")
	public boolean isBlockedByShield() {
		return mEvent.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0;
	}

	/**
	 * Returns true if the damage cause is {@link DamageCause#VOID} or {@link DamageCause#KILL}, false if not
	 */
	public boolean isUnblockable() {
		return getCause() == DamageCause.VOID || getCause() == DamageCause.KILL;
	}

	// Mandatory Event Methods
	private static final HandlerList HANDLERS = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
