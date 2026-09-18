package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.BodyguardCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class BodyguardDamageTransfer extends Effect {
	public static final String EFFECT_ID = "BodyguardTransferDamage";

	private final Player mPlayer;

	private BodyguardCS mCosmetic;
	private final BodyguardCS.BodyguardDisplay mDisplay;
	private double mRadius;
	private Player mGuardian;
	private double mDamageTransferPercent;

	private boolean mWasTooFar = false;

	public BodyguardDamageTransfer(int duration, Player guardian, Player player, double damageTransferPercent,
								   double radius, BodyguardCS bodyguardCS) {
		super(duration, EFFECT_ID);
		mGuardian = guardian;
		mPlayer = player;
		mDamageTransferPercent = damageTransferPercent;
		mCosmetic = bodyguardCS;
		mDisplay = mCosmetic.bodyguardEffect(player, mDuration);
		mRadius = radius;
	}

	public void updateTransfer(Player newGuardian, double damageTransferPercent, int duration, double radius,
							   BodyguardCS cosmetic) {
		mGuardian = newGuardian;
		mDamageTransferPercent = damageTransferPercent;
		mCosmetic = cosmetic;
		mDuration = duration;
		mRadius = radius;

		mDisplay.updateItemDisplay(mCosmetic.getActiveMaterial(), mCosmetic.getActiveName());
		mDisplay.extend(duration);
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		boolean isTooFar = mPlayer.getLocation().distanceSquared(mGuardian.getLocation()) > mRadius * mRadius;

		if (mWasTooFar && !isTooFar) {
			mDisplay.updateItemDisplay(mCosmetic.getActiveMaterial(), mCosmetic.getActiveName());
		} else if (!mWasTooFar && isTooFar) {
			mDisplay.updateItemDisplay(mCosmetic.getInactiveMaterial(), mCosmetic.getInactiveName());
		}

		if (!isTooFar) {
			mCosmetic.bodyguardLink(mGuardian, mPlayer);
		}

		mWasTooFar = isTooFar;
	}

	// Need to receive the damage before equipment calculation
	public static void bodyguardPlayerHurt(Player player, DamageEvent event) {
		Effect targetBodyguardEffect = EffectManager.getInstance().getActiveEffect(player, EFFECT_ID);

		if (targetBodyguardEffect instanceof BodyguardDamageTransfer transfer) {
			transfer.attemptTransfer(event);
		}
	}

	public void attemptTransfer(final DamageEvent event) {
		if (event.getType() == DamageEvent.DamageType.TRUE
			|| !mPlayer.isOnline()
			|| !mGuardian.isOnline()
			|| !mPlayer.getWorld().equals(mGuardian.getWorld())
			|| mPlayer.getLocation().distanceSquared(mGuardian.getLocation()) > mRadius * mRadius
			|| event.isCancelled()
			|| event.isBlocked()) {
			return;
		}

		double guardianDamage = event.getBaseDamage() * mDamageTransferPercent;
		double playerDamage = event.getBaseDamage() * (1 - mDamageTransferPercent);

		event.setBaseDamage(playerDamage);
		mPlayer.setNoDamageTicks(10);

		mCosmetic.damageTransfer(mGuardian, mPlayer);

		DamageUtils.damage(event.getSource(), mGuardian, event.getType(), guardianDamage, ClassAbility.BODYGUARD, true, false);
	}

	public Player getGuardian() {
		return mGuardian;
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		mDisplay.remove();
		Bodyguard.removePlayerFromGuardian(mGuardian, mPlayer);
	}

	@Override
	public boolean shouldDeleteOnDeath() {
		return true;
	}

	@Override
	public boolean shouldDeleteOnAbilityUpdate() {
		return true;
	}

	@Override
	public boolean shouldDeleteOnLogout() {
		return true;
	}

	@Override
	public boolean doesDisplay() {
		return false;
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public String toString() {
		return EFFECT_ID + " duration: " + getDuration();
	}

}
