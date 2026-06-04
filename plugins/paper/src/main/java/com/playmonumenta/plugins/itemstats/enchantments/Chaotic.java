package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.StatPriority;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Chaotic implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Chaotic";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CHAOTIC;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public StatPriority getPriorityAmount() {
		return StatPriority.WEAPON_BASE_DAMAGE;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		boolean isProjectile = event.getType() == DamageType.PROJECTILE || event.getAbility() == ClassAbility.ALCHEMIST_POTION;
		if (isProjectile || event.getType() == DamageType.MELEE) {
			event.setBaseDamage(Math.max(0, event.getBaseDamage() + calculateChaoticDamage(isProjectile, player, value, enemy)));
		}
	}

	public static double calculateChaoticDamage(boolean isProjectile, Player player, double value, LivingEntity enemy) {
		if (value > 0) {
			double random = Math.random();
			int result = (int) Math.floor(random * (2 * (int) value + 1)) - (int) value;
			if (random < 0.001) {
				// SFX: 0.1% chance for boo womp
				World world = player.getWorld();
				world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 2f, Constants.Note.B3.mPitch);
				world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 2f, Constants.Note.B3.mPitch);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.8f, Constants.Note.AS3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1.8f, Constants.Note.AS3.mPitch);
				}, 9);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.8f, Constants.Note.A3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1.8f, Constants.Note.A3.mPitch);
				}, 18);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 2.3f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 2.3f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 2.3f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, SoundCategory.PLAYERS, 2.3f, Constants.Note.AB3.mPitch);
				}, 27);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.9f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1.9f, Constants.Note.AB3.mPitch);
				}, 28);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.8f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1.8f, Constants.Note.AB3.mPitch);
				}, 29);
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1.7f, Constants.Note.AB3.mPitch);
					world.playSound(player, Sound.BLOCK_NOTE_BLOCK_GUITAR, SoundCategory.PLAYERS, 1.7f, Constants.Note.AB3.mPitch);
				}, 30);
			}
			if (isProjectile) {
				return result;
			} else {
				new PartialParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
				return result * player.getCooledAttackStrength(0);
			}
		}
		return 0;
	}
}
