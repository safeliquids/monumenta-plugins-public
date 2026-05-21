package com.playmonumenta.plugins.bosses.bosses;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.effects.ProjectileIframe;
import com.playmonumenta.plugins.events.CustomEffectApplyEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class WormSegmentBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_wormsegment";
	private @Nullable LivingEntity mHead;
	@Nullable
	private final WormBoss mHeadBoss;
	public static final int detectionRange = 64;

	private static final ImmutableList<Class<? extends Effect>> COPIED_EFFECTS = ImmutableList.of(
		InfernoDamage.class,
		CustomDamageOverTime.class,
		ProjectileIframe.class);
	private double mSegmentDamageThisTick;

	// Helper bosstag to force the transmission of effects
	public WormSegmentBoss(Plugin plugin, LivingEntity boss, @Nullable LivingEntity head, @Nullable WormBoss headBoss) {
		super(plugin, identityTag, boss);
		mHead = head;
		this.mHeadBoss = headBoss;
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), detectionRange, null);
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mHead != null && mHeadBoss != null && event.getSource() != null) {
			mSegmentDamageThisTick += event.getDamage();
			// Do this at the end of the tick so we can't miss the passenger being damaged
			if (MetadataUtils.checkOnceThisTick(mPlugin, mBoss, "ScheduledDamageTransfer")) {
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					Entity damager = event.getDamager();
					LivingEntity livingDamager = null;
					if (damager instanceof LivingEntity livingEntity) {
						livingDamager = livingEntity;
					}
					mHeadBoss.segmentDamage(livingDamager, mSegmentDamageThisTick);
					mSegmentDamageThisTick = 0;
				}, 0);
			}
		}
		event.setBaseDamage(0);
	}

	@Override
	public void customEffectAppliedToBoss(CustomEffectApplyEvent event) {
		if (COPIED_EFFECTS.contains(event.getEffect().getClass())) {
			event.setEntity(this.getHead());
		}
	}

	public void setHead(LivingEntity head) {
		mHead = head;
	}

	public LivingEntity getHead() {
		return (mHead == null ? mBoss : mHead);
	}
}
