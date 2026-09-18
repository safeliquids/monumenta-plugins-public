package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class SpellAuroraVoid extends Spell {
	private static final double EXIT_ARENA_DAMAGE = 0.5;
	private static final double ANTI_CHEESE_DAMAGE = 0.25;
	private static final int VOID_IFRAMES = 20;
	private static final int IMMUNITY_DURATION = 2 * 20;
	private static final String VOID_NAME = "Astral Void";
	private static final String ANTI_CHEESE_NAME = "suffocation";

	private final double mVoidThreshold;
	private final double mSpaceThreshold;
	private final double mSpaceGroundThreshold;
	private final Location mCenter;
	private final Consumer<Player> mOnVoid;

	private final Map<Player, Long> mRecentlySeenPlayers = new HashMap<>();
	private long mTicks = 1;
	private boolean mSupernova = true;

	public SpellAuroraVoid(Location center, Consumer<Player> onVoid) {
		mVoidThreshold = center.getY() - 12;
		mSpaceThreshold = center.getY() + 18;
		mSpaceGroundThreshold = center.getY() + 6;
		mCenter = center;
		mOnVoid = onVoid;
	}

	public void setSupernova(boolean enabled) {
		mSupernova = enabled;
	}

	@Override
	public void run() {
		EntityUtils.getNearbyMobs(mCenter, Aurora.DETECTION_RANGE).stream()
			.filter(entity -> entity.getLocation().getY() <= mVoidThreshold)
			.forEach(Entity::remove);
		List<Player> players = Aurora.playersInRange(mCenter);
		for (Player player : players) {
			if (mRecentlySeenPlayers.getOrDefault(player, 0L) <= mTicks && player.getLocation().getY() <= mVoidThreshold) {
				DamageUtils.damagePercentHealth(null, player, EXIT_ARENA_DAMAGE, false, false, VOID_NAME);
				player.teleport(mCenter.clone().add(0, 10, 0));
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 2 * 20, 1));

				mOnVoid.accept(player);
				mRecentlySeenPlayers.put(player, mTicks + VOID_IFRAMES);
			}
		}

		if (!mSupernova) {
			for (Player player : players) {
				Location pLoc = player.getLocation();
				if (mRecentlySeenPlayers.getOrDefault(player, 0L) <= mTicks &&
					pLoc.getY() >= (PlayerUtils.isFreeFalling(player) ? mSpaceThreshold : mSpaceGroundThreshold)
				) {
					player.sendMessage(Component.text("The air feels thin up here...", NamedTextColor.GRAY, TextDecoration.ITALIC));
					player.setFreezeTicks(20);
					player.playSound(pLoc, Sound.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.6f, 0.8f);
					player.playSound(pLoc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.5f, 0.8f);
					player.playSound(pLoc, Sound.ENTITY_WARDEN_DEATH, SoundCategory.PLAYERS, 0.7f, 0.8f);
					player.playSound(pLoc, Sound.ENTITY_DONKEY_DEATH, SoundCategory.PLAYERS, 0.4f, 0.8f);

					DamageUtils.damagePercentHealth(null, player, ANTI_CHEESE_DAMAGE, false, false, ANTI_CHEESE_NAME);
					player.setVelocity(new Vector(0, -0.5, 0));

					mRecentlySeenPlayers.put(player, mTicks + VOID_IFRAMES);
				}
			}
		}

		for (Player player : players) {
			if (mRecentlySeenPlayers.getOrDefault(player, 0L) <= mTicks && LocationUtils.xzDistance(player.getLocation(), mCenter) > Aurora.ARENA_RADIUS + 1) {
				DamageUtils.damagePercentHealth(null, player, EXIT_ARENA_DAMAGE, false, false, VOID_NAME);
				launchCenter(player);

				mRecentlySeenPlayers.put(player, mTicks + VOID_IFRAMES);
			}
		}
		mTicks++;
	}

	private void launchCenter(Player player) {
		Location tpLoc = player.getLocation();
		tpLoc.setY(mCenter.getY() + 5.5);
		player.teleport(tpLoc);
		player.setVelocity(mCenter.clone().subtract(tpLoc).toVector().multiply(0.1).setY(0));

		EffectManager.getInstance().addEffect(player, "AstralVoidImmunity", new DamageImmunity(IMMUNITY_DURATION, EnumSet.complementOf(EnumSet.of(DamageEvent.DamageType.TRUE))));
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
