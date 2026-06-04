package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.BodyguardCS;
import com.playmonumenta.plugins.effects.BodyguardDamageTransfer;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Bodyguard extends Ability {
	private static final HashMap<UUID, HashSet<UUID>> BODYGUARD_PROTECTED_MAP = new HashMap<>();

	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "BodyguardCustomKnockbackResistance";
	private static final int COOLDOWN_L1 = Constants.TICKS_PER_SECOND * 24;
	private static final int COOLDOWN_L2 = Constants.TICKS_PER_SECOND * 20;
	private static final int RANGE = 25;
	private static final int RADIUS = 4;
	private static final int ABSORPTION_HEALTH_1 = 8;
	private static final int ABSORPTION_HEALTH_2 = 10;
	private static final double KNOCKBACK_RESISTANCE = 1.0;
	private static final int BUFF_DURATION = Constants.TICKS_PER_SECOND * 10;
	private static final float KNOCKBACK = 0.45f;
	private static final double TRANSFER_PERCENTAGE = 0.5;
	private static final double TRANSFER_REDUCTION = 0.2;
	private static final int TRANSFER_DURATION = Constants.TICKS_PER_SECOND * 6;
	private static final int TRANSFER_RADIUS = 10;

	public static final String CHARM_COOLDOWN = "Bodyguard Cooldown";
	public static final String CHARM_RANGE = "Bodyguard Range";
	public static final String CHARM_RADIUS = "Bodyguard Stun Radius";
	public static final String CHARM_ABSORPTION = "Bodyguard Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Bodyguard Absorption Duration";
	public static final String CHARM_KBR_DURATION = "Bodyguard Knockback Resistance Duration";
	public static final String CHARM_KNOCKBACK = "Bodyguard Knockback";
	public static final String CHARM_KNOCKBACK_RESISTANCE = "Bodyguard Knockback Resistance";
	public static final String CHARM_DAMAGE_TRANSFER_AMOUNT = "Bodyguard Damage Transfer Amount";
	public static final String CHARM_DAMAGE_TRANSFER_DURATION = "Bodyguard Damage Transfer Duration";
	public static final String CHARM_DAMAGE_TRANSFER_RADIUS = "Bodyguard Damage Transfer Radius";
	public static final String CHARM_DAMAGE_TRANSFER_REDUCTION = "Bodyguard Damage Transfer Reduction";

	public static final AbilityInfo<Bodyguard> INFO =
		new AbilityInfo<>(Bodyguard.class, "Bodyguard", Bodyguard::new)
			.linkedSpell(ClassAbility.BODYGUARD)
			.scoreboardId("Bodyguard")
			.shorthandName("Bg")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Teleport to another player, giving them and yourself absorption, while transferring their damage to you.")
			.cooldown(COOLDOWN_L1, COOLDOWN_L2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("castSelf", "cast on self or others", bg -> bg.cast(true),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick().lookDirections(AbilityTrigger.LookDirection.DOWN)
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.addTrigger(new AbilityTriggerInfo<>("castOthers", "cast on others only", bg -> bg.cast(false),
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).doubleClick()
					.keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.IRON_CHESTPLATE);

	private final double mAbsorptionHealth;
	private final int mAbsorptionDuration;
	private final double mRange;
	private final float mKnockback;
	private final double mKnockbackRadius;
	private final double mKnockbackResistance;
	private final int mKBRDuration;
	private final double mDamageTransferPercentage;
	private final int mDamageTransferDuration;
	private final double mDamageTransferRadius;
	private final double mDamageTransferReduction;

	private final BodyguardCS mCosmetic;

	public Bodyguard(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mAbsorptionHealth = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION, isLevelOne() ? ABSORPTION_HEALTH_1 : ABSORPTION_HEALTH_2);
		mAbsorptionDuration = CharmManager.getDuration(mPlayer, CHARM_ABSORPTION_DURATION, BUFF_DURATION);
		mKBRDuration = CharmManager.getDuration(mPlayer, CHARM_KBR_DURATION, BUFF_DURATION);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mKnockbackRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDamageTransferPercentage = Math.clamp(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_TRANSFER_AMOUNT, TRANSFER_PERCENTAGE), 0, 1);
		mDamageTransferDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_TRANSFER_DURATION, TRANSFER_DURATION);
		mDamageTransferRadius = CharmManager.getRadius(mPlayer, CHARM_DAMAGE_TRANSFER_RADIUS, TRANSFER_RADIUS);
		mDamageTransferReduction = Math.clamp(CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_TRANSFER_REDUCTION, TRANSFER_REDUCTION), 0, 1);
		mKnockbackResistance = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK_RESISTANCE, KNOCKBACK_RESISTANCE);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new BodyguardCS());

		BODYGUARD_PROTECTED_MAP.put(mPlayer.getUniqueId(), new HashSet<>());
	}

	public boolean cast(final boolean allowSelfCast) {
		if (isOnCooldown()) {
			return false;
		}

		final World world = mPlayer.getWorld();
		final Location userLoc = mPlayer.getLocation();

		final Player targetPlayer = EntityUtils.getPlayerAtCursor(mPlayer, mRange, 0.5);
		if (targetPlayer != null) {
			mCosmetic.onBodyguardOther(mPlayer, targetPlayer, world);

			final Vector dir = userLoc.getDirection();
			final Location otherLoc = targetPlayer.getLocation().setDirection(mPlayer.getEyeLocation().getDirection());
			Location targetLoc = otherLoc.clone().subtract(dir.clone().multiply(0.5)).add(0, 0.5, 0);
			final BoundingBox box = mPlayer.getBoundingBox().shift(targetLoc.clone().subtract(mPlayer.getLocation()));
			if (LocationUtils.collidesWithBlocks(box, mPlayer.getWorld())) {
				targetLoc = otherLoc;
			}

			giveAbsorption(targetPlayer);
			giveDamageTransfer(targetPlayer);

			if (userLoc.distance(targetLoc) > 1
				&& !ZoneUtils.hasZoneProperty(userLoc, ZoneProperty.NO_MOBILITY_ABILITIES)
				&& !ZoneUtils.hasZoneProperty(targetLoc, ZoneProperty.NO_MOBILITY_ABILITIES)) {
				PlayerUtils.playerTeleport(mPlayer, targetLoc);
			}
		} else if (!allowSelfCast) {
			return false;
		}

		putOnCooldown();
		giveAbsorption(mPlayer);
		mCosmetic.onBodyguard(mPlayer, world, userLoc);

		for (final LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mKnockbackRadius)) {
			MovementUtils.knockAway(mPlayer, mob, mKnockback, true);
		}
		return true;
	}

	private void giveAbsorption(final Player player) {
		AbsorptionUtils.addAbsorption(player, mAbsorptionHealth, mAbsorptionHealth, mAbsorptionDuration);

		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(player, KNOCKBACK_RESIST_EFFECT_NAME,
				new PercentKnockbackResist(mKBRDuration, mKnockbackResistance, KNOCKBACK_RESIST_EFFECT_NAME)
					.deleteOnAbilityUpdate(true));
		}
	}

	private void giveDamageTransfer(final Player player) {
		// If the guardian has bguard from someone else, remove their own
		EffectManager.getInstance().clearEffects(mPlayer, BodyguardDamageTransfer.EFFECT_ID);

		Effect targetBodyguardEffect = EffectManager.getInstance().getActiveEffect(player, BodyguardDamageTransfer.EFFECT_ID);

		// If the target player has bguard already, update it
		if (targetBodyguardEffect instanceof BodyguardDamageTransfer transfer) {
			removePlayerFromGuardian(transfer.getGuardian(), player);
			addPlayerToGuardian(mPlayer, player);

			transfer.updateTransfer(mPlayer, mDamageTransferPercentage, mDamageTransferDuration, mDamageTransferRadius, mCosmetic);
		} else {
			addPlayerToGuardian(mPlayer, player);

			EffectManager.getInstance().addEffect(player, BodyguardDamageTransfer.EFFECT_ID, new BodyguardDamageTransfer(
				mDamageTransferDuration, mPlayer, player, mDamageTransferPercentage, mDamageTransferRadius, mCosmetic
			).deleteOnAbilityUpdate(true));
		}
	}

	@Override
	public void invalidate() {
		cleanup(mPlayer);
	}

	@Override
	public void playerQuitEvent(PlayerQuitEvent event) {
		cleanup(mPlayer);
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (ClassAbility.BODYGUARD == event.getAbility() && isLevelTwo()) {
			event.updateFinalMultiplier(1 - mDamageTransferReduction);
		}
	}

	public static void cleanup(Player guardian) {
		UUID guardianUUID = guardian.getUniqueId();

		BODYGUARD_PROTECTED_MAP.computeIfAbsent(guardian.getUniqueId(), unused -> new HashSet<>())
			.forEach(uuid -> {
				Player player = Bukkit.getPlayer(uuid);

				Effect bguardEffect = EffectManager.getInstance().getActiveEffect(player, BodyguardDamageTransfer.EFFECT_ID);
				if (bguardEffect instanceof BodyguardDamageTransfer transfer) {
					transfer.clearEffect();
				}
			});
		BODYGUARD_PROTECTED_MAP.remove(guardianUUID);
	}

	public static void removePlayerFromGuardian(Player guardian, Player affected) {
		UUID uuid = guardian.getUniqueId();
		HashSet<UUID> guardianSet = BODYGUARD_PROTECTED_MAP.get(uuid);

		if (guardianSet != null) {
			guardianSet.remove(affected.getUniqueId());
		}
	}

	public static void addPlayerToGuardian(Player guardian, Player affected) {
		UUID uuid = guardian.getUniqueId();
		BODYGUARD_PROTECTED_MAP.computeIfAbsent(uuid, unused -> new HashSet<>()).add(affected.getUniqueId());
	}

	private static Description<Bodyguard> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger(1)
			.tab().addLine("to teleport to another player")
			.addTrigger(0)
			.tab().addLine("to cast on yourself")
			.addDashedLine()
			.addLine("Teleport to a target player to grant absorption")
			.addLine("to both of you and knock nearby mobs away.")
			.addLine()
			.addStat("Effect: +%d1 Absorption for %t")
			.statValues(stat(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH_1), stat(a -> a.mAbsorptionDuration, BUFF_DURATION))
			.addStat("Knockback Radius: %r")
			.statValues(stat(a -> a.mKnockbackRadius, RADIUS))
			.addStat("Max Range: %r")
			.statValues(stat(a -> a.mRange, RANGE))
			.addStat("Cooldown: %t1")
			.statValues(cooldown(COOLDOWN_L1))
			.addLine()
			.addLine("The targeted player will transfer a portion of their")
			.addLine("incoming damage to you if within radius.")
			.addLine()
			.addStat("Effect: %p Damage Transfer for %t")
			.statValues(stat(a -> a.mDamageTransferPercentage, TRANSFER_PERCENTAGE),
				stat(a -> a.mDamageTransferDuration, TRANSFER_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mDamageTransferRadius, TRANSFER_RADIUS))
			.addDashedLine();
	}

	private static Description<Bodyguard> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Bodyguard*'s absorption and").styles(UNDERLINED)
			.addLine("reduce its cooldown. Transferred").styles(UNDERLINED)
			.addLine("damage is reduced by %p.")
			.statValues(stat(a -> a.mDamageTransferReduction, TRANSFER_REDUCTION))
			.addLine()
			.addStatComparison("Effect: +%d1 -> +%d2 Absorption")
			.statValues(stat(ABSORPTION_HEALTH_1), stat(a -> a.mAbsorptionHealth, ABSORPTION_HEALTH_2))
			.addStatComparison("Cooldown: %t1 -> %t2")
			.statValues(cooldown(COOLDOWN_L1), cooldown(COOLDOWN_L2))
			.addLine()
			.addLine("*Bodyguard* provides knockback").styles(UNDERLINED)
			.addLine("resistance to you and the targeted player.")
			.addLine()
			.addStat("Effect: +%p Knockback Resistance for %t")
			.statValues(stat(a -> a.mKnockbackResistance, KNOCKBACK_RESISTANCE),
				stat(a -> a.mKBRDuration, BUFF_DURATION))
			.addDashedLine();
	}
}
