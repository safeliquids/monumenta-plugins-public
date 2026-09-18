package com.playmonumenta.plugins.abilities.scout.ranger;

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
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.RendingRazorCS;
import com.playmonumenta.plugins.effects.AbilityCooldownRechargeRate;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.perRegion;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;


public class RendingRazor extends Ability {
	private static final String CDR_EFFECT_NAME = "RendingRazorCDRBuff";
	private static final int MAX_CDR_COUNT = 3;

	private static final double[] DAMAGE = {14, 18};
	private static final int RAZOR_TRAVEL_TIME = Constants.TICKS_PER_SECOND * 6;
	private static final double MAXIMUM_BLOCK_DISTANCE = 14.0;
	private static final float KNOCKBACK = 0.15f;
	private static final double REND_SPEED = 1.25; // blocks per tick
	private static final double RADIUS_L1 = 2;
	private static final double RADIUS_L2 = 2.5;
	private static final double CDR_EFFECT = 0.75;
	private static final int BLEED = 2;
	private static final int CDR_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int PIERCE = 999;
	private static final int COOLDOWN_L1 = 16 * Constants.TICKS_PER_SECOND;
	private static final int COOLDOWN_L2 = 12 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Rending Razor Damage";
	public static final String CHARM_COOLDOWN = "Rending Razor Cooldown";
	public static final String CHARM_SPEED = "Rending Razor Travel Speed";
	public static final String CHARM_COOLDOWN_REDUCTION = "Rending Razor Cooldown Reduction Amplifier";
	public static final String CHARM_COOLDOWN_REDUCTION_DURATION = "Rending Razor Cooldown Reduction Duration";
	public static final String CHARM_RAZOR_RANGE = "Rending Razor Range";
	public static final String CHARM_RAZOR_SIZE = "Rending Razor Size";
	public static final String CHARM_RAZOR_PIERCE = "Rending Razor Pierce";
	public static final String CHARM_KNOCKBACK = "Rending Razor Knockback";
	public static final String CHARM_BLEED = "Rending Razor Bleed";

	public static final AbilityInfo<RendingRazor> INFO =
		new AbilityInfo<>(RendingRazor.class, "Rending Razor", RendingRazor::new)
			.linkedSpell(ClassAbility.RENDING_RAZOR)
			.scoreboardId("RendingRazor")
			.shorthandName("RR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Throw a razor that rends through mobs. Grants cooldown reduction per hit.")
			.cooldown(COOLDOWN_L1, COOLDOWN_L2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RendingRazor::cast,
				new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK), AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.SHEARS);

	private final RendingRazorCS mCosmetic;
	private final double mRendSpeed;
	private final double mDamage;
	private final int mCDRDuration;
	private final double mCDRBuff;
	private final double mRazorRange;
	private final double mRadius;
	private final int mPierce;
	private final int mMaxDuration;
	private final float mKnockback;
	private final int mBleed;

	public RendingRazor(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mRendSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, REND_SPEED);
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, AbilityUtils.getRegionScaled(mPlayer, DAMAGE));
		mCDRDuration = CharmManager.getDuration(mPlayer, CHARM_COOLDOWN_REDUCTION_DURATION, CDR_DURATION);
		mCDRBuff = CDR_EFFECT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_COOLDOWN_REDUCTION);
		mRazorRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RAZOR_RANGE, MAXIMUM_BLOCK_DISTANCE);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RAZOR_SIZE, isLevelOne() ? RADIUS_L1 : RADIUS_L2);
		mPierce = PIERCE + (int) CharmManager.getLevel(player, CHARM_RAZOR_PIERCE);
		mKnockback = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
		mBleed = BLEED + (int) CharmManager.getLevel(mPlayer, CHARM_BLEED);
		mMaxDuration = mCDRDuration * MAX_CDR_COUNT; // Ominous x mob limit

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new RendingRazorCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		putOnCooldown();
		mCosmetic.addItemDisplay(mPlayer, mRadius);
		mCosmetic.razorCast(mPlayer);
		ClientModHandler.updateAbility(mPlayer, this);

		Vector eyeDir = mPlayer.getLocation().getDirection();

		double razorDuration = RAZOR_TRAVEL_TIME / mRendSpeed;

		BukkitTask razorTask = new BukkitRunnable() {
			final int mStartingTick = Bukkit.getCurrentTick();
			double mStartAngle = Math.atan(eyeDir.getZ() / eyeDir.getX());
			final HashSet<LivingEntity> mStruckMobs = new HashSet<>();
			final HashSet<LivingEntity> mExcludedMobs = new HashSet<>();

			final Location mOrigin = mPlayer.getEyeLocation();
			Location mRazorLoc = mPlayer.getEyeLocation();
			final World mWorld = mRazorLoc.getWorld();

			Vector mDir = mPlayer.getLocation().getDirection().normalize().multiply(mRendSpeed);

			int mIncrementDegrees = 0;

			boolean mReturning = false;
			int mTicks = 0;
			int mPierceCount = mPierce;

			@Override
			public void run() {
				if (!mPlayer.getWorld().equals(mRazorLoc.getWorld()) ||
					mTicks >= razorDuration) {
					mCosmetic.removeDisplay(mStartingTick);
					this.cancel();
					return;
				}

				if (mIncrementDegrees == 0) {
					if (mRazorLoc.getDirection().getX() < 0) {
						mStartAngle += Math.PI;
					}
					mStartAngle += Math.PI * 90 / 180;
				}

				boolean hasCollided = !mRazorLoc.getBlock().isPassable();
				boolean maxMobLimit = mPierceCount < 0;
				boolean maxDistance = mRazorLoc.distanceSquared(mOrigin) > mRazorRange * mRazorRange;

				if (!mReturning && (hasCollided || maxMobLimit || maxDistance)) {
					if (!maxDistance) {
						mCosmetic.razorHit(mPlayer, mRazorLoc);
					}

					mStruckMobs.clear();
					mStruckMobs.addAll(mExcludedMobs);

					mReturning = true;
				}

				if (mReturning) {
					mDir = LocationUtils.getDirectionTo(mPlayer.getEyeLocation(), mRazorLoc).multiply(mRendSpeed);
				}
				mRazorLoc = mRazorLoc.add(mDir);
				mRazorLoc.setDirection(mDir);

				mCosmetic.tick(mPlayer, mWorld, mRazorLoc, mRadius, mIncrementDegrees);
				mCosmetic.spinDisplay(mRazorLoc, mStartingTick);

				final Hitbox razorHitbox = new Hitbox.SphereHitbox(mRazorLoc, mRadius);
				final List<LivingEntity> hitEnemies = razorHitbox.getHitMobs();
				hitEnemies.removeIf(e -> mStruckMobs.contains(e) || e.isDead());

				for (LivingEntity target : hitEnemies) {
					mCosmetic.razorPierce(mPlayer, LocationUtils.getHalfHeightLocation(target));

					mStruckMobs.add(target);
					mPierceCount--;

					giveCDRBuff();

					if (isLevelTwo()) {
						EntityUtils.applyBleed(mPlugin, mPlayer, target, mBleed);
					}

					DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.PROJECTILE_SKILL, mDamage,
						mInfo.getLinkedSpell(), true, true);
					MovementUtils.knockAwayDirection(mDir, target, mKnockback);

					if (mPierceCount < 0 && !mReturning) {
						mExcludedMobs.addAll(hitEnemies);
						break;
					}
				}

				if (mReturning && mPlayer.getEyeLocation().distanceSquared(mRazorLoc) <= mRendSpeed * mRendSpeed) {
					mCosmetic.razorReturned(mPlayer.getLocation(), mStartingTick);
					mCosmetic.removeDisplay(mStartingTick);
					this.cancel();
					return;
				}

				mIncrementDegrees += 60;
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
		cancelOnDeath(razorTask);

		return true;
	}

	private void giveCDRBuff() {
		Effect cdrBuff = mPlugin.mEffectManager.getActiveEffect(mPlayer, CDR_EFFECT_NAME);

		if (cdrBuff == null) {
			mPlugin.mEffectManager.addEffect(mPlayer, CDR_EFFECT_NAME,
				new AbilityCooldownRechargeRate(mCDRDuration, mCDRBuff, ClassAbility.RENDING_RAZOR)
					.deleteOnAbilityUpdate(true));
		} else {
			cdrBuff.setDuration(Math.min(cdrBuff.getDuration() + mCDRDuration, mMaxDuration));
		}
	}

	@Override
	public void playerDeathEvent(PlayerDeathEvent event) {
		if (!event.isCancelled()) {
			mCosmetic.onDeath();
		}
	}

	private static Description<RendingRazor> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a spinning razor that rends through mobs before returning.")
			.addLine("Each hit grants faster cooldown recharge rate.")
			.addLine()
			.addStat("Damage: %d0R (p)")
			.statValues(perRegion(a -> a.mDamage, DAMAGE[0], DAMAGE[1]))
			.addStat("Effect: +%p Cooldown Recharge Rate for %t (per hit)")
			.statValues(stat(a -> a.mCDRBuff, CDR_EFFECT), stat(a -> a.mCDRDuration, CDR_DURATION))
			.addStat("Max Duration: %t")
			.statValues(stat(a -> a.mMaxDuration, CDR_DURATION * MAX_CDR_COUNT))
			.addStat("Radius: %r1")
			.statValues(stat(a -> a.mRadius, RADIUS_L1))
			.addStat("Cooldown: %t1")
			.statValues(cooldown(COOLDOWN_L1))
			.addDashedLine();
	}

	private static Description<RendingRazor> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Rending Razor*'s radius and reduce its cooldown.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Radius: %r1 -> %r2")
			.statValues(stat(RADIUS_L1), stat(a -> a.mRadius, RADIUS_L2))
			.addStat("Cooldown: %t1 -> %t2")
			.statValues(cooldown(COOLDOWN_L1), cooldown(COOLDOWN_L2))
			.addLine()
			.addLine("*Rending Razor* inflicts %d stacks of *Bleed*.").styles(UNDERLINED, Bleed.BLEED_COLOR)
			.statValues(stat(a -> a.mBleed, BLEED))
			.addDashedLine();
	}
}
