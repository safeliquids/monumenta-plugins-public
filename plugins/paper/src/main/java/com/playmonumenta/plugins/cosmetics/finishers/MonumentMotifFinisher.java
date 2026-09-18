package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MonumentMotifFinisher implements EliteFinisher {

	public static final String NAME = "Monument Motif";

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		Location loc1 = killedMob.getLocation().clone().add(0, 1, 0);
		loc1.setPitch(0.0F);
		Vector horizontalDir = VectorUtils.crossProd(loc1.getDirection(), new Vector(0.0, 1.0, 0.0));
		Location loc2 = loc1.clone().add(horizontalDir);
		Location loc3 = loc1.clone().subtract(horizontalDir);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				switch (mTicks) {
					case 0 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 4, 16 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 8, 20, 32 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}
					case 12 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(0));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 24 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(0));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(19));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 28 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 36 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(0));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(12));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 40 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(14));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 44 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(0));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(9));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 48 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 52 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(10));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(6));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 56 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(9));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(9));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(9));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(8));
						world.playSound(loc1, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 2f);
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					case 60 -> {
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						world.playSound(loc1, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, Constants.NotePitches.calculatePitch(7));
						new PartialParticle(Particle.NOTE, loc1, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc2, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
						new PartialParticle(Particle.NOTE, loc3, 1, 0, 0, 0, 1).minimumCount(1).spawnAsPlayerActive(p);
					}

					default -> {
					}
				}
				if (mTicks >= 61) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override
	public Material getDisplayItem() {
		return Material.NOTE_BLOCK;
	}

}
