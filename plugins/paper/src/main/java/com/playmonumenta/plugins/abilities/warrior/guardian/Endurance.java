package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Warrior;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;

public class Endurance extends Ability {
	public static final String NAME = "Endurance";

	public static final String CHARM_BLOCKING_SPEED = "Endurance Shield Stun Reduction";

	private static final double STUN_REDUCTION = 0.25;

	public static final AbilityInfo<Endurance> INFO =
		new AbilityInfo<>(Endurance.class, NAME, Endurance::new)
			.linkedSpell(ClassAbility.ENDURANCE)
			.description(getDescription())
			.canUse(player -> AbilityUtils.getSpecNum(player) == Warrior.GUARDIAN_SPEC_ID);

	private final double mShieldStunReduction;

	public Endurance(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mShieldStunReduction = STUN_REDUCTION + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLOCKING_SPEED);
	}

	@Override
	public void damageShieldedEvent(DamageShieldedEvent event) {
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			int shieldBrokenTicks = mPlayer.getCooldown(Material.SHIELD);
			if (shieldBrokenTicks > 0) {
				int finalTicks = updateStunCooldown(mPlayer, shieldBrokenTicks);
				mPlayer.setCooldown(Material.SHIELD, finalTicks);
			}
		});
	}

	public static int updateStunCooldown(Player player, int ticks) {
		Endurance endurance = Plugin.getInstance().mAbilityManager.getPlayerAbilityIgnoringSilence(player, Endurance.class);

		return (int) (endurance != null ? (ticks * (1 - endurance.mShieldStunReduction)) : ticks);
	}

	public static Description<Endurance> getDescription() {
		return new FormattedDescriptionBuilder<>(() -> INFO)
			.addLine("Reduces the duration shields are")
			.addLine("stunned for by %p.")
				.statValues(stat(a -> a.mShieldStunReduction, STUN_REDUCTION));
	}
}
