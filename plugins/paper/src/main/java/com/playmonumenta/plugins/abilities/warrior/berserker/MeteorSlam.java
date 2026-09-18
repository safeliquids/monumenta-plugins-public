package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.berserker.MeteorSlamCS;
import com.playmonumenta.plugins.effects.ZeroArgumentEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class MeteorSlam extends Ability {
	public static final String METEOR_SLAM_JUMP_BOOST_EFFECT = "MeteorSlamJumpBoostEffect";

	private static final String SLAM_ONCE_THIS_TICK_METAKEY = "MeteorSlamTickSlammed";
	private static final int SNEAK_TIME_REQ = 3;
	private static final int CAST_DELAY = 5;

	// Swing
	private static final int JUMP_AMPLIFIER_L1 = 3;
	private static final int JUMP_AMPLIFIER_L2 = 4;
	private static final int DURATION_TICKS = Constants.TICKS_PER_SECOND * 2;
	private static final double VAULT_VELOCITY = 1.0;
	private static final double VAULT_VELOCITY_PENALTY = 0.5;

	// Slam
	private static final double AUTOMATIC_THRESHOLD = 3;
	private static final double MAX_HEIGHT = 7;
	private static final double SLAM_DAMAGE_PER_BLOCK_L1 = 3;
	private static final double SLAM_DAMAGE_PER_BLOCK_L2 = 4;
	private static final double SLAM_RADIUS = 3;

	// Ground Pound
	private static final double GROUND_POUND_DAMAGE_BONUS = 0.5;
	private static final double GROUND_POUND_RADIUS_BONUS = 0.5;
	private static final double GROUND_POUND_VELOCITY = 1.8;
	private static final int GROUND_POUND_FIRE_DURATION = 5 * Constants.TICKS_PER_SECOND;
	private static final int GROUND_POUND_BLOODLUST_COST = 1;
	private static final double GROUND_POUND_KNOCKBACK = 0.5;
	private static final double GROUND_POUND_SLOWNESS_MULTIPLIER = 0.15;
	private static final double GROUND_POUND_VULNERABILITY_MULTIPLIER = 0.15;
	private static final int GROUND_POUND_SLOWNESS_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final int GROUND_POUND_VULNERABILITY_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final int COOLDOWN_L1 = 8 * Constants.TICKS_PER_SECOND;
	private static final int COOLDOWN_L2 = 6 * Constants.TICKS_PER_SECOND;

	// Others

	public static final String CHARM_JUMP_BOOST = "Meteor Slam Jump Boost";
	public static final String CHARM_DURATION = "Meteor Slam Duration";
	public static final String CHARM_VELOCITY = "Meteor Slam Vault Velocity";
	public static final String CHARM_THRESHOLD = "Meteor Slam Fall Requirement";
	public static final String CHARM_HEIGHT = "Meteor Slam Max Height";
	public static final String CHARM_SLAM_DAMAGE = "Meteor Slam Damage";
	public static final String CHARM_METEOR_SLAM_RADIUS = "Meteor Slam Radius";
	public static final String CHARM_GROUND_POUND_VELOCITY = "Meteor Slam Ground Pound Velocity";
	public static final String CHARM_GROUND_POUND_DAMAGE = "Meteor Slam Ground Pound Damage Per Block Fallen";
	public static final String CHARM_GROUND_POUND_RADIUS = "Meteor Slam Ground Pound Additional Radius";
	public static final String CHARM_GROUND_POUND_FIRE_DURATION = "Meteor Slam Ground Pound Fire Duration";
	public static final String CHARM_GROUND_POUND_BLOODLUST_COST = "Meteor Slam Ground Pound Bloodlust Cost";
	public static final String CHARM_GROUND_POUND_KNOCKBACK = "Meteor Slam Ground Pound Knockback";
	public static final String CHARM_GROUND_POUND_SLOWNESS_MULTIPLIER = "Meteor Slam Ground Pound Slowness Multiplier";
	public static final String CHARM_GROUND_POUND_SLOWNESS_DURATION = "Meteor Slam Ground Pound Slowness Duration";
	public static final String CHARM_GROUND_POUND_VULNERABILITY_MULTIPLIER = "Meteor Slam Ground Pound Slowness Multiplier";
	public static final String CHARM_GROUND_POUND_VULNERABILITY_DURATION = "Meteor Slam Ground Pound Slowness Duration";
	public static final String CHARM_BLOODLUST_COST = "Meteor Slam Bloodlust Cost";
	public static final String CHARM_COOLDOWN = "Meteor Slam Cooldown";

	// Charm vars

	private final int mJumpBoost;
	private final int mDuration;
	private final double mVaultVelocity;
	private final double mThreshold;
	private final double mMaxHeight;
	private final double mSlamDamage;
	private final double mSlamRadius;
	private final double mGroundPoundDamage;
	private final double mGroundPoundVelocity;
	private final double mGroundPoundRadius;
	private final int mGroundPoundFireDuration;
	private final int mGroundPoundBloodlustCost;
	private final double mGroundPoundKnockback;
	private final double mGroundPoundSlownessMultiplier;
	private final int mGroundPoundSlownessDuration;
	private final double mGroundPoundVulnMultiplier;
	private final int mGroundPoundVulnDuration;

	// Non-charm vars
	private final MeteorSlamCS mCosmetic;
	private final BukkitRunnable mSlamAttackRunner;
	private @Nullable Bloodlust mBloodlust;
	private boolean mHasTouchedGround = false;
	private boolean mGroundPound = false;
	private boolean mCastedGroundpound = false;
	private double mFallFromY = -7050;
	private int mVaultCastTime = 0;
	private int mPoundCastTime = 0;
	private int mSneakTime = 0;

	public static final AbilityInfo<MeteorSlam> INFO =
		new AbilityInfo<>(MeteorSlam.class, "Meteor Slam", MeteorSlam::new)
			.linkedSpell(ClassAbility.METEOR_SLAM)
			.scoreboardId("MeteorSlam")
			.shorthandName("MS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Swing your weapon to damage mobs and vault yourself upward. Passively generate a slam attack when fallen from great heights.")
			.cooldown(COOLDOWN_L1, COOLDOWN_L2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", MeteorSlam::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(true)))
			.addTrigger(new AbilityTriggerInfo<>("castgroundpound", "cast ground pound", mSlam -> mSlam.doGroundPound(true), new AbilityTrigger(AbilityTrigger.Key.SWAP).enabled(false)))
			.displayItem(Material.FIRE_CHARGE);

	public MeteorSlam(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mJumpBoost = (isLevelOne() ? JUMP_AMPLIFIER_L1 : JUMP_AMPLIFIER_L2) + (int) CharmManager.getLevel(mPlayer, CHARM_JUMP_BOOST);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);

		mVaultVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_VELOCITY, VAULT_VELOCITY);

		mThreshold = AUTOMATIC_THRESHOLD + CharmManager.getLevel(mPlayer, CHARM_THRESHOLD);
		mMaxHeight = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, MAX_HEIGHT);
		mSlamDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SLAM_DAMAGE, isLevelOne() ? SLAM_DAMAGE_PER_BLOCK_L1 : SLAM_DAMAGE_PER_BLOCK_L2);
		mSlamRadius = CharmManager.getRadius(mPlayer, CHARM_METEOR_SLAM_RADIUS, SLAM_RADIUS);

		mGroundPoundDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_DAMAGE, GROUND_POUND_DAMAGE_BONUS);
		mGroundPoundVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_VELOCITY, GROUND_POUND_VELOCITY);
		mGroundPoundRadius = CharmManager.getRadius(mPlayer, CHARM_GROUND_POUND_RADIUS, GROUND_POUND_RADIUS_BONUS);
		mGroundPoundKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_GROUND_POUND_KNOCKBACK, GROUND_POUND_KNOCKBACK);
		mGroundPoundBloodlustCost = GROUND_POUND_BLOODLUST_COST + (int) CharmManager.getLevel(mPlayer, CHARM_GROUND_POUND_BLOODLUST_COST);
		mGroundPoundFireDuration = CharmManager.getDuration(mPlayer, CHARM_GROUND_POUND_FIRE_DURATION, GROUND_POUND_FIRE_DURATION);
		mGroundPoundSlownessMultiplier = GROUND_POUND_SLOWNESS_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_GROUND_POUND_SLOWNESS_MULTIPLIER);
		mGroundPoundVulnMultiplier = GROUND_POUND_VULNERABILITY_MULTIPLIER + CharmManager.getLevelPercentDecimal(player, CHARM_GROUND_POUND_VULNERABILITY_MULTIPLIER);
		mGroundPoundSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_GROUND_POUND_SLOWNESS_DURATION, GROUND_POUND_SLOWNESS_DURATION);
		mGroundPoundVulnDuration = CharmManager.getDuration(mPlayer, CHARM_GROUND_POUND_VULNERABILITY_DURATION, GROUND_POUND_VULNERABILITY_DURATION);


		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new MeteorSlamCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mBloodlust = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Bloodlust.class));

		mSlamAttackRunner = new BukkitRunnable() {
			@Override
			public void run() {
				if (player == null) {
					this.cancel();
					return;
				}
				if (AbilityManager.getManager().getPlayerAbility(player, MeteorSlam.class) == null
					|| player.isDead()
					|| !player.isOnline()) {
					// If reached this point but not silenced, then proceed with cancelling
					// If silenced, only return to not run anything, but don't cancel runnable
					if (!AbilityManager.getManager().getPlayerAbilities(player).isSilenced()) {
						this.cancel();
					}
					return;
				}

				if (!PlayerUtils.isOnGround(player)) {
					updateFallFrom(); // Vanilla fall distance would be 0 if on ground
					groundPoundVelocityCheck();
					doGroundPound(false);
					if (mGroundPound) {
						mCosmetic.onGroundPoundTick(mPlugin, mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
					}
				} else {
					// Currently on ground

					// If first tick landing, should still have old mFallFromY to calculate using
					// Therefore can damage if eligible
					if (calculateFallDistance() > mThreshold && MetadataUtils.checkOnceThisTick(plugin, player, SLAM_ONCE_THIS_TICK_METAKEY)) {
						// Only for checking in LivingEntityDamagedByPlayerEvent below,
						// so doesn't slam twice, since this doesn't yet set fall distance to 0
						doSlamAttack(player.getLocation().add(0, 0.15, 0));
					}
					onLanding();
				}
			}
		};
		cancelOnDeath(mSlamAttackRunner.runTaskTimer(plugin, 0, 1));
	}

	public boolean cast() {
		if (isOnCooldown()
			|| ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			return false;
		}

		putOnCooldown();
		doSlash();
		mPlayer.getScheduler().runDelayed(mPlugin, (task) -> {
			if (!PlayerUtils.isOnGround(mPlayer)) {
				mHasTouchedGround = false;
			}
		}, null, 5);
		mVaultCastTime = Bukkit.getServer().getCurrentTick();
		mCastedGroundpound = false;

		mPlugin.mPotionManager.addPotion(mPlayer, PotionManager.PotionID.ABILITY_SELF,
			new PotionEffect(PotionEffectType.JUMP, mDuration, mJumpBoost, true, false));

		mPlugin.mEffectManager.addEffect(mPlayer, METEOR_SLAM_JUMP_BOOST_EFFECT,
			new ZeroArgumentEffect(mDuration, METEOR_SLAM_JUMP_BOOST_EFFECT) {
				@Override
				public String toString() {
					return String.format("%s duration:%d", METEOR_SLAM_JUMP_BOOST_EFFECT, getDuration());
				}
			});

		return true;
	}

	private void doSlash() {
		World world = mPlayer.getWorld();

		mCosmetic.onUpwardSlash(world, mPlayer.getLocation(), mPlayer, 3, 60);

		Location castLocation = mPlayer.getLocation().clone();
		castLocation.setDirection(mPlayer.getLocation().getDirection().setY(0));
		castLocation.setY(castLocation.y() - 3);

		Vector dir = mPlayer.getLocation().getDirection().setY(0).normalize().setY(6).normalize();
		Vector velocity = dir.multiply(mVaultVelocity * (mHasTouchedGround ? 1 : VAULT_VELOCITY_PENALTY));
		mPlayer.setVelocity(velocity);

		// TODO: Possible alternative?
		// Jump can cancel y velocity, thus check if this has happened
		mPlayer.getScheduler().runDelayed(mPlugin, (task) -> {
			Vector currVelocity = mPlayer.getVelocity();

			if (velocity.getY() > currVelocity.getY() && !BlockUtils.isBouncy(mPlayer.getLocation().add(0, -1, 0).getBlock().getType())) {
				currVelocity.setY(velocity.getY());
				mPlayer.setVelocity(currVelocity);
			}
		}, null, 2);
	}

	public void groundPoundVelocityCheck() {
		if (isLevelTwo()
			&& mGroundPound
			&& Bukkit.getServer().getCurrentTick() - CAST_DELAY > mPoundCastTime // 5 tick window so ground pound is guarantee
			&& mPlayer.getVelocity().getY() > -mGroundPoundVelocity + 0.05) {
			mSneakTime = 0;
			mGroundPound = false;
		}
	}

	public boolean doGroundPound(boolean customCast) {
		if (mBloodlust == null
			|| mCastedGroundpound
			|| ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)
			|| Bukkit.getCurrentTick() - mVaultCastTime >= mDuration
			|| !canGroundPound()) {
			return false;
		}

		boolean customTriggerEnabled = hasCustomTrigger(mPlayer);
		if (!customTriggerEnabled
			&& Bukkit.getServer().getCurrentTick() - CAST_DELAY > mVaultCastTime
			&& mPlayer.isSneaking()
			&& mSneakTime < SNEAK_TIME_REQ
		) {
			mSneakTime++;
		}

		if (!mGroundPound
			&& ((customTriggerEnabled && customCast)
			|| mSneakTime >= SNEAK_TIME_REQ)
		) {
			// Seperate if statement to prevent failed cast & consuming stack
			if (mBloodlust.useStacks(mGroundPoundBloodlustCost)) {
				mPlayer.setVelocity(new Vector(0, -mGroundPoundVelocity, 0));
				mCosmetic.onGroundPoundCast(mPlugin, mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
				mPoundCastTime = Bukkit.getServer().getCurrentTick();
				mGroundPound = true;
				mCastedGroundpound = true;
				return true;
			}
		}

		return false;
	}

	@Override
	public void invalidate() {
		if (mSlamAttackRunner != null) {
			mSlamAttackRunner.cancel();
		}
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// If there is a mob in range, cancel the fall damage
		if (event.getType() == DamageEvent.DamageType.FALL && !new Hitbox.SphereHitbox(mPlayer.getLocation(), mSlamRadius + (mGroundPound ? mGroundPoundRadius : 0)).getHitMobs().isEmpty()) {
			event.setCancelled(true);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.MELEE
			&& PlayerUtils.isFallingAttack(mPlayer)
			&& calculateFallDistance() > mThreshold) {

			Location loc = enemy.getLocation().add(0, 0.15, 0);

			if (MetadataUtils.checkOnceThisTick(mPlugin, mPlayer, SLAM_ONCE_THIS_TICK_METAKEY)) {
				doSlamAttack(loc);
				mCosmetic.onSlamCritical(mPlugin, mPlayer.getWorld(), loc, mPlayer);
				onLanding();
				return true;
			}
		}
		return false;
	}

	// Jumping at the same time cancels slam attack
	private void doSlamAttack(Location location) {
		World world = mPlayer.getWorld();
		double fallDistance = calculateFallDistance();
		double linearFall = Math.min(mMaxHeight, fallDistance);
		double extraFall = 10 * (1 - Math.pow(0.975, Math.max(0, fallDistance - linearFall)));
		double actualFall = linearFall + extraFall;

		double slamDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SLAM_DAMAGE, actualFall * (isLevelOne() ? SLAM_DAMAGE_PER_BLOCK_L1 : SLAM_DAMAGE_PER_BLOCK_L2));
		double slamRadius = mSlamRadius;

		if (mGroundPound) {
			slamRadius *= 1 + mGroundPoundRadius;
			slamDamage *= 1 + mGroundPoundDamage;
			mCosmetic.onGroundPoundSlam(mPlugin, world, location, mPlayer, slamRadius);
		}

		for (LivingEntity enemy : new Hitbox.SphereHitbox(location, slamRadius).getHitMobs()) {
			DamageUtils.damage(mPlayer, enemy, DamageEvent.DamageType.MELEE_SKILL, slamDamage, mInfo.getLinkedSpell(), true);
			if (mGroundPound) {
				if (isLevelTwo()) {
					EntityUtils.applySlow(mPlugin, mGroundPoundSlownessDuration, mGroundPoundSlownessMultiplier, enemy);
					EntityUtils.applyVulnerability(mPlugin, mGroundPoundVulnDuration, mGroundPoundVulnMultiplier, enemy);
				}
				MovementUtils.knockAway(mPlayer, enemy, (float) mGroundPoundKnockback, true);
				EntityUtils.applyFire(mPlugin, mGroundPoundFireDuration, enemy, mPlayer);
			}
		}

		mCosmetic.onSlam(world, location, mPlayer, slamRadius, fallDistance);
	}

	// Since getFallDistance is unreliable (ie does not reset while in bed), we check the distance ourselves.
	// 0 to reset fall distance when in water, vines, etc...
	private void updateFallFrom() {
		if (mPlayer.getFallDistance() <= 0) {
			mFallFromY = -10000;
		} else {
			mFallFromY = Math.max(mFallFromY, mPlayer.getLocation().getY());
		}
	}

	private double calculateFallDistance() {
		double currentY = mPlayer.getLocation().getY();
		double fallDistance = mFallFromY - currentY;
		return Math.max(fallDistance, 0);
	}

	private boolean canGroundPound() {
		Location loc = mPlayer.getLocation();
		World world = loc.getWorld();
		double halfReq = mThreshold / 2 + 0.125;

		BoundingBox groundPoundHitbox = new BoundingBox().shift(loc.add(0, -halfReq, 0)).expand(0.3, halfReq, 0.3);

		return !NmsUtils.getVersionAdapter().hasCollisionWithBlocks(world, groundPoundHitbox, false);
	}

	private void onLanding() {
		mHasTouchedGround = true;
		mGroundPound = false;
		mFallFromY = -7050;
		mSneakTime = 0;
	}

	@Override
	public @Nullable Component getHotbarMessage() {
		final TextColor color = INFO.getActionBarColor();
		final String name = INFO.getHotbarName();

		if (mBloodlust != null && mBloodlust.getStacks() < 1 && !isOnCooldown()) {
			return Component.text("[", NamedTextColor.YELLOW)
				.append(Component.text(name != null ? name : "Error", color))
				.append(Component.text("]", NamedTextColor.YELLOW))
				.append(Component.text(": ", NamedTextColor.WHITE))
				.append(Component.text("x", NamedTextColor.RED, TextDecoration.BOLD));
		}

		return null;
	}

	private static Description<MeteorSlam> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Vault upwards and gain Jump Boost.")
			.addLine("Passively, falling more than %d blocks creates")
			.statValues(stat(a -> a.mThreshold, AUTOMATIC_THRESHOLD))
			.addLine("a slam upon landing, dealing more damage for")
			.addLine("each block fallen. Fall damage is cancelled")
			.addLine("if any mob was hit by the slam.")
			.addLine()
			.addStat("Effect: Jump Boost %d1 for %t")
				.statValues(stat(a -> a.mJumpBoost + 1, JUMP_AMPLIFIER_L1 + 1), stat(a -> a.mDuration, DURATION_TICKS))
			.addStat("Slam Damage: %d1 (m) per block (%d block softcap)")
				.statValues(stat(a -> a.mSlamDamage, SLAM_DAMAGE_PER_BLOCK_L1),
					stat(a -> a.mMaxHeight, MAX_HEIGHT))
			.addStat("Slam Radius: %r")
				.statValues(stat(a -> a.mSlamRadius, SLAM_RADIUS))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_L1))
			.addLine()
			.addLine("Sneaking while midair spends %d stack of *Bloodlust*").styles(Bloodlust.BLOODLUST_COLOR)
				.statValues(stat(a -> a.mGroundPoundBloodlustCost, GROUND_POUND_BLOODLUST_COST))
			.addLine("to lunge downwards and enhance your next slam,")
			.addLine("increasing its damage and radius, and causing it")
			.addLine("to ignite mobs, and knock them back.")
			.addLine("(Can cast once per vault)")
			.addLine()
			.addStat("Damage Boost: +%p")
				.statValues(stat(a -> a.mGroundPoundDamage, GROUND_POUND_DAMAGE_BONUS))
			.addStat("Radius Boost: +%p ")
				.statValues(stat(a -> a.mGroundPoundRadius, GROUND_POUND_RADIUS_BONUS))
			.addStat("Effect: Fire for %t")
				.statValues(stat(a -> a.mGroundPoundFireDuration, GROUND_POUND_FIRE_DURATION))
			.addDashedLine();
	}

	private static Description<MeteorSlam> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Meteor Slam*'s damage, jump").styles(UNDERLINED)
			.addLine("boost level and reduce its cooldown.")
			.addLine()
			.addStatComparison("Effect: %d1 -> %d2 Jump Boost")
				.statValues(stat(JUMP_AMPLIFIER_L1 + 1),
					stat(a -> a.mJumpBoost + 1, JUMP_AMPLIFIER_L2 + 1))
			.addStatComparison("Slam Damage: %d1 -> %d2 (m) per block")
				.statValues(stat(SLAM_DAMAGE_PER_BLOCK_L1), stat(a -> a.mSlamDamage, SLAM_DAMAGE_PER_BLOCK_L2))
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(COOLDOWN_L1), cooldown(COOLDOWN_L2))
			.addLine()
			.addLine("Ground Pound now inflicts vulnerability and slowness.")
			.addLine()
			.addStat("Effect: %p Vulnerability for %t")
				.statValues(stat(a -> a.mGroundPoundSlownessMultiplier, GROUND_POUND_SLOWNESS_MULTIPLIER), stat(a -> a.mGroundPoundSlownessDuration, GROUND_POUND_SLOWNESS_DURATION))
			.addStat("Effect: %p Slowness for %t")
				.statValues(stat(a -> a.mGroundPoundSlownessMultiplier, GROUND_POUND_SLOWNESS_MULTIPLIER), stat(a -> a.mGroundPoundSlownessDuration, GROUND_POUND_SLOWNESS_DURATION))
			.addDashedLine();
	}

	private static boolean hasCustomTrigger(Player player) {
		AbilityTrigger groundPoundTrigger = AbilityManager.getManager().getCustomTrigger(player, INFO, "castgroundpound");
		return groundPoundTrigger != null && groundPoundTrigger.isEnabled();
	}
}
