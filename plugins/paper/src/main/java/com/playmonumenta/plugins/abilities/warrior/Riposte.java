package com.playmonumenta.plugins.abilities.warrior;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.RiposteCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Riposte extends Ability implements AbilityWithDuration {
	private static final int RIPOSTE_1_COOLDOWN = Constants.TICKS_PER_SECOND * 15;
	private static final int RIPOSTE_2_COOLDOWN = Constants.TICKS_PER_SECOND * 12;
	private static final int RIPOSTE_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int RIPOSTE_AXE_DURATION = Constants.TICKS_PER_SECOND * 2;
	private static final int RIPOSTE_AXE_RADIUS = 2;
	private static final float RIPOSTE_KNOCKBACK_SPEED = 0.15f;
	private static final double RIPOSTE_SWORD_BONUS_DAMAGE = 1;
	private static final double ENHANCEMENT_DAMAGE = 15;
	private static final double ENHANCEMENT_RADIUS = 4;
	private static final int ENHANCEMENT_ROOT_DURATION = (int) (Constants.TICKS_PER_SECOND * 1.5);

	public static final String CHARM_COOLDOWN = "Riposte Cooldown";
	public static final String CHARM_DURATION = "Riposte Duration";
	public static final String CHARM_STUN_DURATION = "Riposte Axe Stun Duration";
	public static final String CHARM_STUN_RADIUS = "Riposte Axe Stun Radius";
	public static final String CHARM_KNOCKBACK = "Riposte Knockback";
	public static final String CHARM_BONUS_DAMAGE = "Riposte Sword Bonus Damage";
	public static final String CHARM_DAMAGE = "Riposte Enhancement Damage";
	public static final String CHARM_RADIUS = "Riposte Enhancement Range";
	public static final String CHARM_ROOT_DURATION = "Riposte Enhancement Root Duration";

	public static final AbilityInfo<Riposte> INFO =
		new AbilityInfo<>(Riposte.class, "Riposte", Riposte::new)
			.linkedSpell(ClassAbility.RIPOSTE)
			.scoreboardId("Obliteration")
			.shorthandName("Rip")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("While wielding a sword or axe, block a mob's melee attack.")
			.cooldown(RIPOSTE_1_COOLDOWN, RIPOSTE_2_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SKELETON_SKULL);
	private static final EnumSet<DamageType> AFFECTED_TYPES = EnumSet.of(DamageType.MELEE, DamageType.MELEE_ENCH);

	private final double mSwordDamage;
	private final int mMaxDuration;
	private final int mStunDuration;
	private final double mStunRadius;
	private final float mKnockAwaySpeed;
	private final double mEnhancementDamage;
	private final double mEnhancementRadius;
	private final int mEnhancementRootDuration;
	private final RiposteCS mCosmetic;

	private @Nullable BukkitRunnable mRunnable = null;
	private int mCurrDuration = -1;
	private boolean mHasTriggeredL2 = false;

	public Riposte(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mSwordDamage = RIPOSTE_SWORD_BONUS_DAMAGE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BONUS_DAMAGE);
		mMaxDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, RIPOSTE_DURATION);
		mStunDuration = CharmManager.getDuration(mPlayer, CHARM_STUN_DURATION, RIPOSTE_AXE_DURATION);
		mStunRadius = CharmManager.getRadius(mPlayer, CHARM_STUN_RADIUS, RIPOSTE_AXE_RADIUS);
		mKnockAwaySpeed = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, RIPOSTE_KNOCKBACK_SPEED);
		mEnhancementDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, ENHANCEMENT_DAMAGE);
		mEnhancementRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ENHANCEMENT_RADIUS);
		mEnhancementRootDuration = CharmManager.getDuration(mPlayer, CHARM_ROOT_DURATION, ENHANCEMENT_ROOT_DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new RiposteCS());
	}

	@Override
	public void onHurt(final DamageEvent event, @Nullable final Entity damager, @Nullable final LivingEntity source) {
		if (isOnCooldown() || source == null || event.getType() != DamageType.MELEE || event.isBlocked()) {
			return;
		}

		final boolean holdingSword = ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand());
		final boolean holdingAxe = ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand());
		if (!holdingAxe && !holdingSword) {
			return;
		}

		final World world = mPlayer.getWorld();
		final Location playerLoc = mPlayer.getLocation();

		if (isLevelTwo()) {
			mCurrDuration = 0;
			mHasTriggeredL2 = false;
			mRunnable = new BukkitRunnable() {
				@Override
				public void run() {
					mCurrDuration++;
					if (mCurrDuration >= mMaxDuration) {
						this.cancel();
					}
				}

				@Override
				public synchronized void cancel() {
					super.cancel();
					mCurrDuration = -1;
					ClientModHandler.updateAbility(mPlayer, Riposte.this);
				}
			};
			cancelOnDeath(mRunnable.runTaskTimer(mPlugin, 0, 1));
		}

		MovementUtils.knockAway(mPlayer, source, mKnockAwaySpeed, true);
		mCosmetic.onParry(mPlayer, world, playerLoc, source);
		putOnCooldown();
		ClientModHandler.updateAbility(mPlayer, this);
		mPlayer.setNoDamageTicks(20);
		mPlayer.setLastDamage(event.getDamage());
		event.setBaseDamage(0);
		event.setCancelled(true);

		if (isEnhanced()) {
			for (final LivingEntity mob : new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), mEnhancementRadius).getHitMobs()) {
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mEnhancementDamage, mInfo.getLinkedSpell(), true, false);
				EntityUtils.applySlow(mPlugin, mEnhancementRootDuration, 1.0f, mob);
			}
			mCosmetic.onEnhancedParry(world, playerLoc);
		}
	}

	@Override
	public boolean onDamage(final DamageEvent event, final LivingEntity enemy) {
		if (AFFECTED_TYPES.contains(event.getType()) && mCurrDuration != -1) {
			if (ItemUtils.isSword(mPlayer.getInventory().getItemInMainHand())) {
				event.updateDamageWithMultiplier(1 + mSwordDamage, AFFECTED_TYPES);
				mCosmetic.onSwordAttack(mPlayer.getWorld(), mPlayer.getLocation());
				removeRunnable();
			} else if (ItemUtils.isAxe(mPlayer.getInventory().getItemInMainHand())) {
				for (LivingEntity mob : new Hitbox.SphereHitbox(enemy.getLocation().add(0, 1, 0), mStunRadius).getHitMobs()) {
					EntityUtils.applyStun(mPlugin, mStunDuration, mob);
				}
				mCosmetic.onAxeStun(mPlayer.getWorld(), mPlayer.getLocation());
				removeRunnable();
			}
		}
		return false; // prevents multiple applications itself by clearing mSwordTimer
	}

	private void removeRunnable() {
		if (mRunnable != null && !mRunnable.isCancelled() && !mHasTriggeredL2) {
			// Disable next tick, buff only for this tick
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				if (mRunnable != null) {
					mRunnable.cancel();
				}
			}, 1);
			mHasTriggeredL2 = true;
			// Prevent it from making one Runnable per event - optimisation
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mMaxDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration == -1 ? 0 : Math.min(mMaxDuration, mMaxDuration - mCurrDuration);
	}

	private static Description<Riposte> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("While holding a sword or axe, periodically")
			.addLine("block an incoming melee attack.")
			.addLine()
			.addStat("Cooldown: %t1")
			.statValues(cooldown(RIPOSTE_1_COOLDOWN))
			.addDashedLine();
	}

	private static Description<Riposte> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Reduce *Riposte*'s cooldown.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Cooldown: %t1 -> %t2")
			.statValues(cooldown(RIPOSTE_1_COOLDOWN), cooldown(RIPOSTE_2_COOLDOWN))
			.addLine()
			.addLine("Activating *Riposte* while holding a sword").styles(UNDERLINED)
			.addLine("or axe empowers your next attack within %t.")
			.statValues(stat(a -> a.mMaxDuration, RIPOSTE_DURATION))
			.addLine()
			.addLine("Sword attacks deal increased damage.")
			.addLine()
			.addStat("Damage Boost: +%p (m)")
			.statValues(stat(a -> a.mSwordDamage, RIPOSTE_SWORD_BONUS_DAMAGE))
			.addLine()
			.addLine("Axe attacks stun mobs in a radius.")
			.addLine()
			.addStat("Effect: Stun for %t")
			.statValues(stat(a -> a.mStunDuration, RIPOSTE_AXE_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mStunRadius, RIPOSTE_AXE_RADIUS))
			.addDashedLine();
	}

	private static Description<Riposte> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("When *Riposte* activates, deal damage").styles(UNDERLINED)
			.addLine("to nearby mobs and root them.")
			.addLine()
			.addStat("Damage: %d (m)")
			.statValues(stat(a -> a.mEnhancementDamage, ENHANCEMENT_DAMAGE))
			.addStat("Effect: Root for %t")
			.statValues(stat(a -> a.mEnhancementRootDuration, ENHANCEMENT_ROOT_DURATION))
			.addStat("Radius: %r")
			.statValues(stat(a -> a.mEnhancementRadius, ENHANCEMENT_RADIUS))
			.addDashedLine();
	}
}
