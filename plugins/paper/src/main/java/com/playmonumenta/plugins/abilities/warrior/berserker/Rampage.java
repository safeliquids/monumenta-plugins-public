package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.RampageCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public final class Rampage extends Ability implements AbilityWithChargesOrStacks {
	// 8s

	private static final int COOLDOWN = 10 * Constants.TICKS_PER_SECOND;
	private static final int MAX_DURATION = 8 * Constants.TICKS_PER_SECOND;
	private static final double DAMAGE = 10;
	private static final double RADIUS = 4;
	private static final int BLOODLUST_COST = 4;
	private static final int BLOODLUST_EXTEND_COST = 2;
	private static final int MAX_EXTENSIONS_L1 = 2;
	private static final int MAX_EXTENSIONS_L2 = 4;
	private static final double HEAL_PERCENT = 0.01;
	private static final double SPEED_PER_EXTENSION = 0.05;
	private static final double DAMAGE_PER_EXTENSION = 0.05;
	private static final double KNOCKBACK = 0.45;
	private static final int DURATION_PER_EXTENSION = Constants.TICKS_PER_SECOND * 4;
	private static final int INITIAL_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final String REGENERATION_EFFECT_NAME = "RampageCustomRegenerationEffect";
	private static final String SPEED_EFFECT_NAME = "RampageCustomSpeedEffect";
	private static final String DAMAGE_EFFECT_NAME = "RampageCustomDamageEffect";
	private static final String AESTHETICS_EFFECT_NAME = "RampageAestheticEffect";

	public static final String CHARM_DAMAGE = "Rampage Damage";
	public static final String CHARM_MAX_RECAST = "Rampage Max Recast";
	public static final String CHARM_COOLDOWN = "Rampage Cooldown";

	public static final String CHARM_SPEED_EFFECT = "Rampage Speed Amplifier Per Recast";
	public static final String CHARM_DAMAGE_EFFECT = "Rampage Damage Amplifier Per Recast";
	public static final String CHARM_HEALING = "Rampage Healing";

	public static final String CHARM_BLOODLUST_COST = "Rampage Bloodlust Cost";
	public static final String CHARM_BLOODLUST_RECAST_COST = "Rampage Recast Bloodlust Cost";
	public static final String CHARM_RADIUS = "Rampage Range";
	public static final String CHARM_KNOCKBACK = "Rampage Knockback";
	public static final String CHARM_INITIAL_DURATION = "Rampage Initial Duration";
	public static final String CHARM_DURATION_PER_RECAST = "Rampage Duration Per Recast";
	public static final String CHARM_MAX_DURATION = "Rampage Max Duration";

	public static final AbilityInfo<Rampage> INFO =
		new AbilityInfo<>(Rampage.class, "Rampage", Rampage::new)
			.linkedSpell(ClassAbility.RAMPAGE)
			.scoreboardId("Rampage")
			.shorthandName("Rmp")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Consume Bloodlust stacks to knock enemies back, and gain health regeneration and melee damage on Bloodlust gain.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Rampage::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).lookDirections(AbilityTrigger.LookDirection.DOWN)
				.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)))
			.displayItem(Material.BLAZE_POWDER);

	private final double mDamage;
	private final double mRadius;
	private final double mHealing;
	private final double mSpeedEffect;
	private final double mDamageEffect;
	private final double mKnockback;
	private final int mBloodlustCost;
	private final int mBloodlustExtensionCost;
	private final int mMaxDuration;
	private final int mMaxExtensions;
	private final int mDurationExtension;
	private final int mInitialDuration;

	private int mRecasts = 0;

	private @Nullable Bloodlust mBloodlust;
	private int mLastCastTicks = 0;
	private boolean mActive = false;

	private final RampageCS mCosmetic;

	public Rampage(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, DAMAGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDurationExtension = CharmManager.getDuration(mPlayer, CHARM_DURATION_PER_RECAST, DURATION_PER_EXTENSION);
		mMaxExtensions = (isLevelOne() ? MAX_EXTENSIONS_L1 : MAX_EXTENSIONS_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_RECAST);
		mInitialDuration = CharmManager.getDuration(mPlayer, CHARM_INITIAL_DURATION, INITIAL_DURATION);

		mHealing = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, HEAL_PERCENT);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mSpeedEffect = SPEED_PER_EXTENSION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_EFFECT);
		mDamageEffect = DAMAGE_PER_EXTENSION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_EFFECT);

		mBloodlustCost = BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_BLOODLUST_COST);
		mBloodlustExtensionCost = BLOODLUST_EXTEND_COST + (int) CharmManager.getLevel(mPlayer, CHARM_BLOODLUST_RECAST_COST);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_MAX_DURATION, MAX_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new RampageCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mBloodlust = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Bloodlust.class));
	}

	public boolean cast() {
		if (mBloodlust == null || isOnCooldown() || isRecast()) {
			return false;
		}

		if (mRecasts >= mMaxExtensions) {
			mCosmetic.cannotRecast(mPlayer);
			return false;
		}

		EffectManager effectManager = mPlugin.mEffectManager;

		int stackCost = mActive ? mBloodlustExtensionCost : mBloodlustCost;
		if (mBloodlust.getStacks() < stackCost) {
			return false;
		}

		mBloodlust.useStacks(stackCost);

		if (!mActive) {
			mActive = true;

			effectManager.addEffect(mPlayer, AESTHETICS_EFFECT_NAME, new Aesthetics(mInitialDuration,
				(entity, fourHertz, twoHertz, oneHertz) -> rampageTick(fourHertz, twoHertz, oneHertz),
				(entity) -> rampageEnd()).deleteOnAbilityUpdate(true));

			effectManager.addEffect(mPlayer, REGENERATION_EFFECT_NAME,
				new CustomRegeneration(mInitialDuration, mHealing * EntityUtils.getMaxHealth(mPlayer), 5, null, false, mPlugin));
		} else {
			mRecasts++;
			mCosmetic.onStackGain(mPlayer.getWorld(), mPlayer, mPlayer.getLocation());

			Effect rampage = effectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
			Effect regen = effectManager.getActiveEffect(mPlayer, REGENERATION_EFFECT_NAME);

			if (rampage != null) {
				rampage.setDuration(Math.min(rampage.getDuration() + mDurationExtension, mMaxDuration));
			}

			if (regen != null) {
				regen.setDuration(Math.min(regen.getDuration() + mDurationExtension, mMaxDuration));
			}
		}

		if (isLevelTwo() && mRecasts > 0) {
			Effect rampage = effectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);

			// Rampage shouldn't be null...
			if (rampage != null) {
				effectManager.clearEffects(mPlayer, SPEED_EFFECT_NAME);
				effectManager.addEffect(mPlayer, SPEED_EFFECT_NAME,
					new PercentSpeed(
						Math.min(rampage.getDuration(), mMaxDuration),
						mSpeedEffect * mRecasts, SPEED_EFFECT_NAME)
						.deleteOnAbilityUpdate(true));

				effectManager.clearEffects(mPlayer, DAMAGE_EFFECT_NAME);
				effectManager.addEffect(mPlayer, DAMAGE_EFFECT_NAME,
					new PercentDamageDealt(
						Math.min(rampage.getDuration(), mMaxDuration),
						mDamageEffect * mRecasts)
						.damageTypes(EnumSet.of(DamageType.MELEE, DamageType.MELEE_SKILL))
						.deleteOnAbilityUpdate(true));
			}
		}


		World world = mPlayer.getWorld();

		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mRadius);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
			MovementUtils.knockAway(mPlayer, mob, (float) mKnockback, true);
			mCosmetic.onHitMob(mPlayer, mob);
		}

		ClientModHandler.updateAbility(mPlayer, this);
		Location loc = mPlayer.getLocation();
		mCosmetic.onCast(mPlayer, loc, world, mRadius);
		return true;
	}

	private void rampageTick(boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		mCosmetic.tick(mPlayer, fourHertz, twoHertz, oneHertz);
		if (oneHertz) {
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	private void rampageEnd() {
		mCosmetic.loseEffect(mPlayer);
		mActive = false;
		mRecasts = 0;
		putOnCooldown();
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private boolean isRecast() {
		int ticks = Bukkit.getServer().getCurrentTick();
		if (ticks - mLastCastTicks <= 5) {
			return true;
		}
		mLastCastTicks = ticks;
		return false;
	}

	// Using this for UMM
	@Override
	public @NotNull Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();
		int charges = getCharges();

		Component output = Component.text("[", NamedTextColor.YELLOW)
			.append(Component.text(name != null ? name : "Error", color))
			.append(Component.text("]", NamedTextColor.YELLOW))
			.append(Component.text(": ", NamedTextColor.WHITE));

		if (charges == 0 && mBloodlust != null) {
			output = output.append(mBloodlust.getStacks() >= mBloodlustCost ?
				Component.text("✓", NamedTextColor.GREEN, TextDecoration.BOLD) :
				Component.text("x", NamedTextColor.RED, TextDecoration.BOLD)
			);
		} else {
			output = output.append(Component.text(charges + "s ",
				charges >= getMaxCharges() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
		}

		return output;
	}

	@Override
	public int getMaxCharges() {
		return mMaxDuration / 20;
	}

	@Override
	public int getCharges() {
		Effect rampage = mPlugin.mEffectManager.getActiveEffect(mPlayer, AESTHETICS_EFFECT_NAME);
		return rampage != null ? rampage.getDuration() / 20 : 0;
	}

	@Override
	public ChargeType getChargeType() {
		return ChargeType.STACKS;
	}

	@Override
	public @Nullable String getMode() {
		return mActive ? "max" : null;
	}

	private static Description<Rampage> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend %d stacks of *Bloodlust* to deal damage to all").styles(Bloodlust.BLOODLUST_COLOR)
				.statValues(stat(a -> a.mBloodlustCost, BLOODLUST_COST))
			.addLine("nearby mobs and knock them back and enter a")
			.addLine("rampage for the next %t.")
				.statValues(stat(a -> a.mInitialDuration, INITIAL_DURATION))
			.addLine()
			.addStat("Damage: %d (m)")
				.statValues(stat(a -> a.mDamage, DAMAGE))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Healing: %p HP every %t")
				.statValues(stat(a -> a.mHealing, HEAL_PERCENT), stat(5))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addLine()
			.addLine("Recasting while *Rampage* is active spends %d *Bloodlust*").styles(UNDERLINED, Bloodlust.BLOODLUST_COLOR)
				.statValues(stat(a -> a.mBloodlustExtensionCost, BLOODLUST_EXTEND_COST))
			.addLine("stacks to extend the duration and deal damage again.")
			.addLine()
			.addStat("Duration Increase: +%t per recast")
				.statValues(stat(a -> a.mDurationExtension, DURATION_PER_EXTENSION))
			.addStat("Max Recast: %d1")
				.statValues(stat(a -> a.mMaxExtensions, MAX_EXTENSIONS_L1))
			.addStat("Max Duration: %t")
				.statValues(stat(a -> a.mMaxDuration, MAX_DURATION))
			.addDashedLine();
	}

	private static Description<Rampage> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Rampage*'s max recast.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Max Recast: %d1 ->  %d2")
				.statValues(stat(MAX_EXTENSIONS_L1), stat(a -> a.mMaxExtensions, MAX_EXTENSIONS_L2))
			.addLine()
			.addLine("Gain speed and increased melee damage")
			.addLine("for each *Rampage* recast.").styles(UNDERLINED)
			.addLine()
			.addStat("Effect: +%p Speed per recast")
				.statValues(stat(a -> a.mSpeedEffect, SPEED_PER_EXTENSION))
			.addStat("Effect: +%p Melee Damage per recast")
				.statValues(stat(a -> a.mDamageEffect, DAMAGE_PER_EXTENSION))
			.addDashedLine();
	}
}
