package com.playmonumenta.plugins.abilities.scout.hunter;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.hunter.QuiverStormCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class QuiverStorm extends Ability implements AbilityWithChargesOrStacks {
	public static final String ARROW_METADATA = "QuiverStormArrow_HasConvertedDamage"; // false if the arrow is a QStorm arrow that has not hit its enemy, true if it has already hit its enemy. Used in Explosive.
	public static final double ENCHANT_RATIO = 0.35;
	private static final String LOCKDOWN_HIT = "LockdownHitThisTick";
	private static final String PREDATOR_HIT = "PredatorStrikeHitThisTick";
	private static final String SPAWN_TICK = "SpawnTick";
	private static final String STRUCK_ENEMY_TICK = "QuiverStormStruckEnemyTick";
	private static final String SHOT_QSTORM_THIS_TICK = "QuiverStormThisTick";

	// List of Enchantments to reduce
	// Piercing is included, but is handled separately
	private static final List<EnchantmentType> EFFECT_ENCHANT_LIST = new ArrayList<>(List.of(
		EnchantmentType.EARTH_ASPECT,
		EnchantmentType.FIRE_ASPECT,
		EnchantmentType.ICE_ASPECT,
		EnchantmentType.WIND_ASPECT,
		EnchantmentType.THUNDER_ASPECT,
		EnchantmentType.BLEEDING,
		EnchantmentType.HARPOON,
		EnchantmentType.PUNCH,
		EnchantmentType.CURSE_OF_SHRAPNEL
	));

	private static final double DAMAGE_PERCENT_L1 = 0.25;
	private static final double DAMAGE_PERCENT_L2 = 0.30;
	private static final int PASSIVE_ARROW = 2;
	private static final int MAX_ARROW_L1 = 3;
	private static final int MAX_ARROW_L2 = 5;
	private static final int DELAY_L1 = 4;
	private static final int DELAY_L2 = 3;
	private static final int PSTRIKE_ARROW = 3;
	private static final int LD_ARROW = 1;

	public static final String CHARM_DAMAGE = "Quiver Storm Damage";
	public static final String CHARM_MAX_STACKS = "Quiver Storm Max Stacks";
	public static final String CHARM_PASSIVE_ARROW = "Quiver Storm Passive Arrows";
	public static final String CHARM_PIERCE = "Quiver Storm Pierce";
	public static final String CHARM_DELAY = "Quiver Storm Arrow Delay";
	public static final String CHARM_PSTRIKE_REFUND = "Quiver Storm Predator Strike Arrow Recharge";
	public static final String CHARM_LOCKDOWN_REFUND = "Quiver Storm Lockdown Arrow Recharge";

	public static final AbilityInfo<QuiverStorm> INFO =
		new AbilityInfo<>(QuiverStorm.class, "Quiver Storm", QuiverStorm::new)
			.linkedSpell(ClassAbility.QUIVER_STORM)
			.scoreboardId("QuiverStorm")
			.shorthandName("QS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Projectiles will fire additional arrows.")
			.displayItem(Material.BLAZE_ROD);

	private final double mDamagePercent;
	private final int mDelay;
	private final int mPierce;
	private final int mMaxCharges;
	private int mCharges;
	private final int mPassive;
	private final int mPstrikeArrowRefund;
	private final int mLockdownRefund;
	private final QuiverStormCS mCosmetic;

	private @Nullable Sharpshooter mSharpshooter;

	public QuiverStorm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_PERCENT_L1 : DAMAGE_PERCENT_L2);
		mMaxCharges = (isLevelOne() ? MAX_ARROW_L1 : MAX_ARROW_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_STACKS);
		mDelay = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DELAY, isLevelOne() ? DELAY_L1 : DELAY_L2);
		mPierce = Math.clamp((int) CharmManager.getLevel(mPlayer, CHARM_PIERCE), 0, 100);
		mPassive = PASSIVE_ARROW + (int) CharmManager.getLevel(mPlayer, CHARM_PASSIVE_ARROW);
		mPstrikeArrowRefund = Math.clamp(PSTRIKE_ARROW + (int) CharmManager.getLevel(mPlayer, CHARM_PSTRIKE_REFUND), 0, mMaxCharges);
		mLockdownRefund = Math.clamp(LD_ARROW + (int) CharmManager.getLevel(mPlayer, CHARM_LOCKDOWN_REFUND), 0, mMaxCharges);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new QuiverStormCS());

		mCharges = Math.min(AbilityManager.getManager().getTrackedCharges(mPlayer, ClassAbility.QUIVER_STORM), mMaxCharges);

		Bukkit.getScheduler().runTask(plugin, () ->
			mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class));
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (!EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			|| projectile.hasMetadata(ARROW_METADATA)
			|| Grappling.playerHoldingHook(mPlayer)
			|| PredatorStrike.hasPredatorStrikeReady(mPlayer)
			|| AbilityUtils.isVolley(mPlayer, projectile)) {
			return true;
		}

		final ItemStack inMainHand = mPlayer.getInventory().getItemInMainHand();
		final ItemStack inOffHand = mPlayer.getInventory().getItemInOffHand();

		if ((ItemStatUtils.hasEnchantment(inMainHand, EnchantmentType.TWO_HANDED)
			&& !(ItemUtils.isNullOrAir(inOffHand) || ItemStatUtils.hasEnchantment(inOffHand, EnchantmentType.WEIGHTLESS)))
			|| ItemUtils.isShootableItem(inOffHand)) {
			return false;
		}

		ItemStatManager.PlayerItemStats stats = Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		DamageListener.appendProjectileStats(stats, projectile);

		final ItemStatManager.PlayerItemStats.ItemStatsMap map = stats.getItemStats();
		if (map != null) {
			ItemStat piercing = Objects.requireNonNull(EnchantmentType.PIERCING.getItemStat());
			double piercingLvl = map.get(piercing);
			piercingLvl += mSharpshooter != null ? mSharpshooter.getAdditionalPierce() : 0;
			map.set(piercing, piercingLvl * ENCHANT_RATIO);

			for (EnchantmentType enchant : EFFECT_ENCHANT_LIST) {
				double lvl = map.get(Objects.requireNonNull(enchant.getItemStat()));

				map.set(Objects.requireNonNull(enchant.getItemStat()), lvl * ENCHANT_RATIO);
			}
		}

		final int arrows = mPassive + consumeAllCharges();
		final Vector projVelocity = projectile.getVelocity();
		double gearProjSpeed = 0;
		if (map != null) {
			gearProjSpeed = (mSharpshooter != null)
				? mSharpshooter.getProjectileSpeedMultWithEnhance(map)
				: map.get(AttributeType.PROJECTILE_SPEED);
		}
		// 0 is the default when no gear has this stat; treat as 1 since velocity was not pre-scaled by gear
		if (gearProjSpeed == 0) {
			gearProjSpeed = 1;
		}
		projVelocity.multiply(1 / gearProjSpeed);
		final EntityType projType = projectile.getType();
		final ItemStack projItem = mPlayer.getEquipment().getItemInMainHand();
		final Vector playerDirection = mPlayer.getEyeLocation().getDirection();
		// Technically this should be wrapped in a check (disable-relative-projectile-velocity is false)
		// But on Monumenta servers this setting is always false
		Vector playerVelocity = mPlayer.getVelocity();
		if (PlayerUtils.isOnGround(mPlayer)) {
			playerVelocity.setY(0);
		}
		projVelocity.subtract(playerVelocity);

		cancelOnDeath(new BukkitRunnable() {
			int mArrows = arrows;

			@Override
			public void run() {
				if (mArrows <= 0) {
					this.cancel();
					return;
				}
				mCosmetic.arrowLaunch(mPlayer);
				shootProjectile(projType, projVelocity, playerDirection, projItem, stats);
				mArrows--;
			}
		}.runTaskTimer(mPlugin, mDelay, mDelay));

		return true;
	}

	private void shootProjectile(final EntityType projectileType, Vector initVelocity, Vector initPlayerDirection, ItemStack weapon, ItemStatManager.PlayerItemStats stats) {
		// Quiver Storm's velocity calculation is off by 0.4%. I cannot identify the source of the bug, but players won't be able to notice it for now.
		final var projectileClass = Preconditions.checkNotNull(projectileType.getEntityClass());
		Location loc = mPlayer.getEyeLocation();
		Vector basisDirection = initVelocity.clone().normalize();
		Vector quiverVelocity = new Vector(0, 0, ItemUtils.getVanillaProjectileSpeed(weapon));
		Vector playerVelocity = mPlayer.getVelocity();
		if (PlayerUtils.isOnGround(mPlayer)) {
			playerVelocity.setY(0);
		}

		// Invert old player's pitch/yaw to make the pattern flat
		double[] initPlayerDir = VectorUtils.vectorToRotation(initPlayerDirection);
		basisDirection = VectorUtils.rotateYAxis(basisDirection, -initPlayerDir[0]); // Yaw
		basisDirection = VectorUtils.rotateXAxis(basisDirection, -initPlayerDir[1]); // Pitch

		double deltaYaw = VectorUtils.vectorToRotation(basisDirection)[0]; // Recovers the yaw pattern
		// Apply yaw offset to get arrow pattern
		quiverVelocity = VectorUtils.rotateYAxis(quiverVelocity, deltaYaw);
		// Apply player pitch/yaw to rotate that pattern to match the arrow's direction
		double[] playerDir = VectorUtils.vectorToRotation(mPlayer.getEyeLocation().getDirection());
		quiverVelocity = VectorUtils.rotateXAxis(quiverVelocity, playerDir[1]); // Pitch
		quiverVelocity = VectorUtils.rotateYAxis(quiverVelocity, playerDir[0]); // Yaw

		loc.setDirection(quiverVelocity);

		Projectile quiverProj = (Projectile) mPlayer.getWorld().spawn(loc, projectileClass);
		quiverProj.setMetadata(ARROW_METADATA, new FixedMetadataValue(mPlugin, false));
		quiverProj.setVelocity(quiverVelocity);
		quiverProj.setShooter(mPlayer);

		// Technically this should be wrapped in a check (disable-relative-projectile-velocity is false)
		// But on Monumenta servers this setting is always false
		quiverProj.setVelocity(quiverProj.getVelocity().add(playerVelocity));

		if (quiverProj instanceof AbstractArrow arrow && !(arrow instanceof Trident)) {
			arrow.setPierceLevel(mPierce);
		}

		if (mSharpshooter != null) {
			mSharpshooter.doNotTrack(quiverProj);
		}

		quiverProj.setMetadata(DamageListener.DO_NOT_REPLACE_METADATA, new FixedMetadataValue(Plugin.getInstance(), 0));
		DamageListener.addProjectileItemStats(quiverProj.getUniqueId(), stats);

		ProjectileLaunchEvent event = new ProjectileLaunchEvent(quiverProj);
		Bukkit.getPluginManager().callEvent(event); // ProjectileSpeed modifies the velocity here


		if (!event.isCancelled()) {
			mCosmetic.arrowEffect(mPlugin, quiverProj);
		}
		if (quiverProj instanceof AbstractArrow arrow) {
			arrow.setCritical(true);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
		} else if (quiverProj instanceof ThrowableProjectile throwable) {
			// Snowball only
			ItemUtils.setSnowballItem(throwable, weapon);
		}

		MetadataUtils.setMetadata(quiverProj, SPAWN_TICK, Bukkit.getCurrentTick());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// QStorm native damage handling
		if (event.getDamager() instanceof Projectile proj
			&& proj.hasMetadata(ARROW_METADATA)
			&& event.getType() == DamageEvent.DamageType.PROJECTILE) {
			event.setCancelled(true);
			proj.setMetadata(ARROW_METADATA, new FixedMetadataValue(mPlugin, true));

			int projectileSpawnTick = proj.getMetadata(SPAWN_TICK).getFirst().asInt();
			if (enemy.getMetadata(STRUCK_ENEMY_TICK).stream().noneMatch(m -> m.asInt() == projectileSpawnTick)) {
				double dmg = AbilityUtils.projectileFinalDamage(proj, enemy, 0, mDamagePercent);
				DamageUtils.damage(mPlayer, proj, enemy,
					new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE_SKILL,
						mInfo.getLinkedSpell(),
						DamageListener.getProjectileItemStats(proj)),
					dmg, true, false, false);
				MetadataUtils.setMetadata(enemy, STRUCK_ENEMY_TICK, projectileSpawnTick);
			}

			if (proj instanceof Trident) {
				proj.remove();
			}

			return false;
		}

		// QStorm arrow addition handling

		ClassAbility ability = event.getAbility();

		boolean hitLockdown = ability == ClassAbility.LOCKDOWN
			&& MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, LOCKDOWN_HIT);

		boolean hitPredatorStrike = ability == ClassAbility.PREDATOR_STRIKE
			&& MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, PREDATOR_HIT);

		if (hitLockdown) {
			addCharge(mLockdownRefund);
		} else if (hitPredatorStrike) {
			addCharge(mPstrikeArrowRefund);
		}

		return false;
	}

	private void addCharge(int count) {
		int prevCharges = mCharges;
		mCharges = Math.min(mMaxCharges, mCharges + count);

		if (mMaxCharges != prevCharges) {
			showChargesMessage();
		}

		updateAbility();
	}

	private int consumeAllCharges() {
		if (mCharges <= 0) {
			return 0;
		}

		int charges = mCharges;
		if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SHOT_QSTORM_THIS_TICK)) {
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mCharges = 0;

				if (mMaxCharges > 1) {
					showChargesMessage();
				}

				updateAbility();
			}, 0);
		}

		return charges;
	}

	@Override
	public void updateAbility() {
		AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.QUIVER_STORM, mCharges);
		ClientModHandler.updateAbility(mPlayer, this);
	}

	private static Description<QuiverStorm> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Firing a projectile will fire extra shots that")
			.addLine("inherit %p of non-damage enchants.")
			.statValues(stat(ENCHANT_RATIO))
			.addLine()
			.addStat("Damage: %p1 (of weapon damage) (p) (per shot)")
			.statValues(stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L1))
			.addStat("Fire Rate: %t1")
			.statValues(stat(a -> a.mDelay, DELAY_L1))
			.addStat("Shots: %d")
			.statValues(stat(a -> a.mPassive, PASSIVE_ARROW))
			.addLine()
			.addLine("Landing *Lockdown* adds %d shots to your next").styles(UNDERLINED)
			.statValues(stat(a -> a.mLockdownRefund, LD_ARROW))
			.addLine("shot, whereas landing *Predator Strike* adds %d shots.").styles(UNDERLINED)
			.statValues(stat(a -> a.mPstrikeArrowRefund, PSTRIKE_ARROW))
			.addLine()
			.addStat("Max Shots: %d1")
			.statValues(stat(a -> a.mMaxCharges, MAX_ARROW_L1))
			.addDashedLine();
	}

	private static Description<QuiverStorm> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Quiver Storm*'s damage,").styles(UNDERLINED)
			.addLine("fire rate, and max shot count.")
			.addLine()
			.addStatComparison("Damage: %p1 -> %p2")
			.statValues(stat(DAMAGE_PERCENT_L1), stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L2))
			.addStatComparison("Fire Rate: %t1 -> %t2")
			.statValues(stat(DELAY_L1), stat(a -> a.mDelay, DELAY_L2))
			.addStatComparison("Max Shots: %d1 -> %d2")
			.statValues(stat(MAX_ARROW_L1), stat(a -> a.mMaxCharges, MAX_ARROW_L2))
			.addDashedLine();
	}

	@Override
	public int getCharges() {
		return mCharges;
	}

	@Override
	public int getMaxCharges() {
		return mMaxCharges;
	}
}
