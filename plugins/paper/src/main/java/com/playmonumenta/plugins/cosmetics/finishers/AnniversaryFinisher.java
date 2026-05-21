package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class AnniversaryFinisher implements EliteFinisher {
	public static final String NAME = "Anniversary Finisher";
	public static final LocalDate LAUNCH = LocalDate.of(2016, 5, 28);
	public static final Particle.DustTransition DUST_TRANSITION = new Particle.DustTransition(
		Color.WHITE,
		Color.fromRGB(0xff, 0xd7, 0x00), // gold
		1.5f
	);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		int yearsSinceLaunch = (int) LAUNCH.until(DateUtils.localDateTime().toLocalDate(), ChronoUnit.YEARS);

		Location digitLoc = loc.clone().add(0.0, 1.4, 0.0);

		digitLoc.getNearbyPlayers(64).forEach(nearbyPlayer -> {
			nearbyPlayer.playSound(digitLoc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
				SoundCategory.PLAYERS, 1.0f, 1.0f);
			ParticleUtils.drawSevenSegmentNumber(yearsSinceLaunch, digitLoc, nearbyPlayer,
				1.0, 1.5, Particle.DUST_COLOR_TRANSITION, DUST_TRANSITION);
		});
	}

	@Override
	public Material getDisplayItem() {
		return Material.FIREWORK_ROCKET;
	}
}
