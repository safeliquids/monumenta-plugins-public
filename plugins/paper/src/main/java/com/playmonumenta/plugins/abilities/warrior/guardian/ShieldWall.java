package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ShieldWallCS;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

public class ShieldWall extends Ability implements AbilityWithChargesOrStacks {
	private static final AbilityTriggerInfo.TriggerRestriction RESTRICTION = new AbilityTriggerInfo.TriggerRestriction("Holding a Shield",
		p -> p.getInventory().getItemInMainHand().getType().equals(Material.SHIELD)
			|| p.getInventory().getItemInOffHand().getType().equals(Material.SHIELD));

	private static final String ON_HIT_EFFECT = "ShieldWallHitCooldownEffect";
	private static final int BASH_DURATION = 15;
	private static final double[] SHIELD_WALL_DAMAGE_L1 = {15, 20};
	private static final double[] SHIELD_WALL_DAMAGE_L2 = {18, 24};
	private static final int SHIELD_WALL_DURABILITY_L1 = 10;
	private static final int SHIELD_WALL_DURABILITY_L2 = 14;
	private static final int SHIELD_WALL_RECHARGE_COUNT = 1;
	private static final int SHIELD_WALL_RECHARGE = Constants.TICKS_PER_SECOND;
	private static final int SHIELD_WALL_COOLDOWN_L1 = 10 * Constants.TICKS_PER_SECOND;
	private static final int SHIELD_WALL_COOLDOWN_L2 = 8 * Constants.TICKS_PER_SECOND;
	public static final int SHIELD_WALL_ANGLE = 180;
	private static final float SHIELD_WALL_KNOCKBACK = 0.3f;
	public static final double SHIELD_WALL_RADIUS_L1 = 2.75;
	public static final double SHIELD_WALL_RADIUS_L2 = 3.25;
	private static final int SHIELD_WALL_HEIGHT = 5;
	private static final double SHIELD_WALL_BASH_VELOCITY = 1;
	private static final int SHIELD_WALL_BASH_STUN_DURATION = 30; // 1.5s

	public static final String CHARM_DAMAGE = "Shield Wall Damage";
	public static final String CHARM_ANGLE = "Shield Wall Angle";
	public static final String CHARM_KNOCKBACK = "Shield Wall Knockback";
	public static final String CHARM_HEIGHT = "Shield Wall Height";
	public static final String CHARM_RADIUS = "Shield Wall Radius";
	public static final String CHARM_DURABILITY = "Shield Wall Durability";
	public static final String CHARM_DURABILITY_RECHARGE = "Shield Wall Durability Recharge";

	public static final String CHARM_COOLDOWN = "Shield Wall Bash Cooldown";
	public static final String CHARM_BASH_VELOCITY = "Shield Wall Bash Velocity";
	public static final String CHARM_BASH_STUN_DURATION = "Shield Wall Bash Stun Duration";

	public static final AbilityInfo<ShieldWall> INFO =
		new AbilityInfo<>(ShieldWall.class, "Shield Wall", ShieldWall::new)
			.linkedSpell(ClassAbility.SHIELD_WALL)
			.scoreboardId("ShieldWall")
			.shorthandName("SW")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Block to generate wall that can block projectiles and mobs from entering.")
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ShieldWall::cast,
				new AbilityTrigger(AbilityTrigger.Key.SWAP), RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("castbash", "cast bash", ShieldWall::shieldWallBash,
				new AbilityTrigger(AbilityTrigger.Key.DROP), RESTRICTION))
			.cooldown(SHIELD_WALL_COOLDOWN_L1, CHARM_COOLDOWN)
			.displayItem(Material.STONE_BRICK_WALL);

	private final double mHeight;
	private final float mKnockback;
	private final double mDamage;
	private final double mAngle;
	private final double mRadius;
	private final int mRecharge;
	private final ShieldWallCS mCosmetic;
	private final double mBashVelocity;
	private final int mStunDuration;
	private final int mMaxDurability;

	private boolean mBashing = false;
	private @Nullable BukkitTask mRunnable;
	private @Nullable CounterStrike mCounterStrike;

	private int mShieldWallIframe = Bukkit.getCurrentTick();
	private int mT = 0;
	private int mCastTime = Bukkit.getCurrentTick();
	private int mDurability;
	private int mTimer = 0;

	private final Set<LivingEntity> mMobsAlreadyHit = new HashSet<>();
	private final Set<LivingEntity> mMobsAlreadyBashed = new HashSet<>();

	public ShieldWall(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mHeight = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEIGHT, SHIELD_WALL_HEIGHT);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, SHIELD_WALL_KNOCKBACK);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE,
			isLevelOne() ? AbilityUtils.getRegionScaled(mPlayer, SHIELD_WALL_DAMAGE_L1) :
			AbilityUtils.getRegionScaled(mPlayer, SHIELD_WALL_DAMAGE_L2));
		mAngle = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ANGLE, SHIELD_WALL_ANGLE);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS, isLevelOne() ? SHIELD_WALL_RADIUS_L1 : SHIELD_WALL_RADIUS_L2);
		mRecharge = (int) (SHIELD_WALL_RECHARGE_COUNT + CharmManager.getLevel(mPlayer, CHARM_DURABILITY_RECHARGE));
		mMaxDurability = (int) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DURABILITY,
			isLevelOne() ? SHIELD_WALL_DURABILITY_L1 : SHIELD_WALL_DURABILITY_L2);
		mBashVelocity = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_BASH_VELOCITY, SHIELD_WALL_BASH_VELOCITY);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_BASH_STUN_DURATION, SHIELD_WALL_BASH_STUN_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ShieldWallCS());
		mDurability = mMaxDurability;


		Bukkit.getScheduler().runTask(mPlugin, () ->
			mCounterStrike = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, CounterStrike.class));
	}

	public boolean cast() {
		int currTick = Bukkit.getCurrentTick();
		if (currTick - mCastTime < 5) {
			return false;
		}

		mCastTime = currTick;

		if (mRunnable != null) {
			disableShieldWall();

			return true;
		} else if (mDurability <= 0) {
			return false;
		}

		enableShieldWall();

		cancelOnDeath(
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (!mPlayer.isOnline()
						|| AbilityUtils.isSilenced(mPlayer)
						|| !RESTRICTION.test(mPlayer)
						|| (mDurability <= 0 && !mBashing)) {
						disableShieldWall();
						return;
					}

					shieldWallTick();
				}
			}.runTaskTimer(mPlugin, 0, 1));
		return true;
	}

	@Override
	public void invalidate() {
		if (mRunnable != null) {
			mRunnable.cancel();
		}
	}

	public void shieldWallTick() {
		Location loc = mPlayer.getLocation();
		Hitbox hitbox = Hitbox.approximateHollowCylinderSegment(loc.clone().add(0, -1, 0), mHeight + 1, mBashing ? 0 : 0.7 * mRadius - 0.5, 1.15 * mRadius, Math.toRadians(mAngle) / 2);
		List<Pair<Float, Double>> mArcHeights = mCosmetic.wallParticles(mPlayer, loc, mRadius, mAngle, mHeight, mT++);

		List<Projectile> projectiles = hitbox.getHitEntitiesByClass(Projectile.class);
		for (Projectile proj : projectiles) {
			if (proj.getShooter() instanceof LivingEntity shooter && !(shooter instanceof Player)) {
				proj.remove();
				mCosmetic.shieldOnBlock(mPlayer, loc, proj.getLocation(), mRadius);
			}

			if (breakShield()) {
				return;
			}
		}

		List<LivingEntity> entities = hitbox.getHitMobs();
		for (LivingEntity le : entities) {

			boolean shouldBreak = false;
			boolean enteredWall = !mMobsAlreadyHit.contains(le);
			if (enteredWall) {
				if (mBashing) {
					if (!mMobsAlreadyBashed.contains(le)) {
						shieldWallBash(le);
					}
				} else {
					shouldBreak = breakShield();
				}
			}

			if (!mPlugin.mEffectManager.hasEffect(le, ON_HIT_EFFECT + mPlayer.getName())) {
				if (mKnockback > 0 && !EntityUtils.isCCImmuneMob(le)) {
					float y = 0.4f;
					if (!le.isOnGround()) {
						y -= 0.2f;
					}
					if (!enteredWall) {
						y -= 0.15f;
					}
					mCosmetic.shieldOnHit(loc, mArcHeights, le, mRadius, enteredWall ? 1 : 0.25f);
					MovementUtils.knockAway(loc, le, mKnockback, y, true);
				} else {
					mCosmetic.shieldOnHit(loc, mArcHeights, le, mRadius, enteredWall ? 1 : 0.2f);
				}
				mPlugin.mEffectManager.addEffect(le, ON_HIT_EFFECT + mPlayer.getName(), new OnHitTimerEffect(5));
			}

			mMobsAlreadyHit.add(le);

			if (shouldBreak) {
				return;
			}
		}

		mMobsAlreadyHit.removeIf(mob -> !entities.contains(mob));
	}

	private void shieldWallBash(LivingEntity le) {
		DamageUtils.damage(mPlayer, le, DamageEvent.DamageType.MELEE_SKILL, mDamage, ClassAbility.SHIELD_WALL, false);

		if (EntityUtils.isBoss(le) || EntityUtils.isElite(le)) {
			EntityUtils.applySlow(mPlugin, mStunDuration, .99, le);
		} else {
			EntityUtils.applyStun(mPlugin, mStunDuration, le);
		}
		mMobsAlreadyBashed.add(le);

		if (mCounterStrike != null) {
			mCounterStrike.onTaunt(le);
		}

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (le.isDead() || !le.isValid() || mT > mStunDuration * 2) {
					this.cancel();
				} else if (!EntityUtils.isStunned(le)) {
					EntityUtils.applyTaunt(le, mPlayer, false);
					this.cancel();
				}

				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		mCosmetic.shieldWallBash(mPlayer, le.getWorld(), le.getLocation().add(0, 1, 0));
	}

	public boolean shieldWallBash() {
		if (isOnCooldown() || mRunnable == null) {
			return false;
		}

		mBashing = true;
		mMobsAlreadyHit.clear();

		mCosmetic.shieldStartEffect(mPlayer.getWorld(), mPlayer, mPlayer.getLocation(), mRadius, mAngle, mHeight);

		if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			Vector dir = mPlayer.getLocation().getDirection();
			dir.setY(0);
			dir.normalize().multiply(mBashVelocity);
			mPlayer.setVelocity(dir);
		}

		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			mBashing = false;
			mMobsAlreadyBashed.clear();
		}, BASH_DURATION);

		putOnCooldown();
		return true;
	}

	public void enableShieldWall() {
		mCosmetic.shieldToggleOn(mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
	}

	public void disableShieldWall() {
		if (mRunnable != null) {
			mRunnable.cancel();
		}
		mRunnable = null;
		mCosmetic.shieldToggleOff(mPlayer.getWorld(), mPlayer.getLocation(), mPlayer);
	}

	private boolean breakShield() {
		int tick = Bukkit.getCurrentTick();
		if (tick - mShieldWallIframe < 10) {
			return false;
		}

		mShieldWallIframe = tick;

		consumeCharge();

		return mDurability <= 0;
	}

	private void consumeCharge() {
		mDurability = Math.max(0, mDurability - 1);
		updateAbility();
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mRunnable != null) {
			mTimer = 0;
			return;
		}
		mTimer += 5;

		if (mTimer % SHIELD_WALL_RECHARGE == 0) {
			addDurability(mRecharge);
		}
	}

	public void addDurability(int count) {
		mDurability = Math.min(mMaxDurability, mDurability + count);
		updateAbility();
	}

	private static Description<ShieldWall> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Toggle a shield in front of you that blocks mobs and")
			.addLine("enemy projectiles at the cost of *1* durability.").styles(WHITE)
			.addLine("Recharge durability when *Shield Wall* is down.").styles(UNDERLINED)
			.addLine()
			.addStat("Radius: %r1")
				.statValues(stat(a -> a.mRadius, SHIELD_WALL_RADIUS_L1))
			.addStat("Height: %r")
				.statValues(stat(a -> a.mHeight, SHIELD_WALL_HEIGHT))
			.addStat("Max Durability: %d1")
				.statValues(stat(a -> a.mMaxDurability, SHIELD_WALL_DURABILITY_L1))
			.addStat("Recharge: +%d every %t")
				.statValues(stat(a -> a.mRecharge, SHIELD_WALL_RECHARGE_COUNT),
				stat(SHIELD_WALL_RECHARGE))
			.addLine()
			.addTrigger(1)
			.addLine("While *Shield Wall* is up, perform a forward").styles(UNDERLINED)
			.addLine("bash that taunts, damages, and stuns mobs.")
			.addLine("(Elites/Bosses are rooted instead)")
			.addLine()
			.addStat("Damage: %d1R (m)")
				.statValues(perRegion(a -> a.mDamage, SHIELD_WALL_DAMAGE_L1[0], SHIELD_WALL_DAMAGE_L1[1]))
			.addStat("Effect: Stun for %t")
				.statValues(stat(a -> a.mStunDuration, SHIELD_WALL_BASH_STUN_DURATION))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(SHIELD_WALL_COOLDOWN_L1))
			.addDashedLine();
	}

	private static Description<ShieldWall> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Shield Wall*'s radius, durability,").styles(UNDERLINED)
			.addLine("damage, and reduce its cooldown.")
			.addLine()
			.addStatComparison("Radius: %r1 -> %r2")
				.statValues(stat(SHIELD_WALL_RADIUS_L1), stat(a -> a.mRadius, SHIELD_WALL_RADIUS_L2))
			.addStatComparison("Max Durability: %d1 -> %d2")
				.statValues(stat(SHIELD_WALL_DURABILITY_L1), stat(a -> a.mMaxDurability, SHIELD_WALL_DURABILITY_L2))
			.addStatComparison("Damage: %d1 -> %d2R (m)")
				.statValues(perRegion(SHIELD_WALL_DAMAGE_L1[0], SHIELD_WALL_DAMAGE_L1[1]),
					perRegion(a -> a.mDamage, SHIELD_WALL_DAMAGE_L2[0], SHIELD_WALL_DAMAGE_L2[1]))
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(SHIELD_WALL_COOLDOWN_L1), cooldown(SHIELD_WALL_COOLDOWN_L2))
			.addDashedLine();
	}

	@Override
	public int getCharges() {
		return mDurability;
	}

	@Override
	public int getMaxCharges() {
		return mMaxDurability;
	}
}
