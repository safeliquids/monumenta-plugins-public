package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.aurora.Aurora;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.function.DoublePredicate;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellFrigidCollapse extends Spell {
	private static final double SIZE = Aurora.ARENA_RADIUS;
	private static final int TELEGRAPH_DURATION = 2 * 20;
	private static final int DAMAGE = 55;
	private static final float PULL_SPEED = 0.35f;
	private static final int DEBUFF_DURATION = 8 * 20;
	private static final double DEBUFF = 0.35;
	private static final String SPELL_NAME = "Frigid Collapse";

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	public SpellFrigidCollapse(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation().add(0, 1.5, 0);

		world.playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2.5f, 1.33f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 2.0f, 1f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_AIR, SoundCategory.HOSTILE, 1.8f, 0.5f);
		world.playSound(bossLoc, Sound.ENTITY_BREEZE_INHALE, SoundCategory.HOSTILE, 1.8f, 0.71f);

		EntityUtils.selfRoot(mBoss, TELEGRAPH_DURATION);

		float yawOffset = bossLoc.getYaw() + 15.0f;
		DoublePredicate anglePredicate = angle -> FastUtils.wrapMod(angle - yawOffset, 90) < 30;

		new PPCircle(Particle.REDSTONE, bossLoc, SIZE)
			.data(new Particle.DustOptions(Color.fromRGB(0x15dfe4), 2.8f))
			.countPerMeter(40)
			.distanceFalloff(SIZE)
			.anglePredicate(anglePredicate)
			.ringMode(false)
			.delta(0, 0.66, 0)
			.spawnAsBoss();

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {

				if (mTicks <= TELEGRAPH_DURATION) {
					mTicks++;

					world.playSound(bossLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.4f, 0.3f + 1.5f * mTicks / TELEGRAPH_DURATION);
					return;
				}

				world.playSound(bossLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 2.5f, 1.33f);
				world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 1.6f, 1f);
				world.playSound(bossLoc, Sound.ENTITY_BREEZE_IDLE_GROUND, SoundCategory.HOSTILE, 1.6f, 0.67f);
				world.playSound(bossLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.8f, 0.7f);
				world.playSound(bossLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.HOSTILE, 2.0f, 0.1f);

				new PPCircle(Particle.DUST_COLOR_TRANSITION, bossLoc, SIZE * mTicks / TELEGRAPH_DURATION)
					.countPerMeter(16)
					.distanceFalloff(SIZE)
					.data(new Particle.DustTransition(Color.AQUA, Color.BLUE, 2.0f))
					.ringMode(false)
					.anglePredicate(anglePredicate)
					.delta(0, 0.6, 0)
					.spawnAsBoss();

				new PPCircle(Particle.END_ROD, bossLoc, SIZE * mTicks / TELEGRAPH_DURATION)
					.countPerMeter(20)
					.distanceFalloff(SIZE)
					.ringMode(false)
					.anglePredicate(anglePredicate)
					.extra(0.01)
					.delta(0, 0.6, 0)
					.spawnAsBoss();

				new PPPillar(Particle.END_ROD, bossLoc, 10)
					.count(30)
					.spawnAsBoss();

				Aurora.playersInRange(bossLoc).stream()
					.filter(player -> {
						Location pLoc = player.getLocation();
						Vector vector = pLoc.clone().subtract(bossLoc).toVector().setY(0);
						double distance = vector.length();
						return distance <= SIZE &&
							Math.abs(player.getBoundingBox().getCenterY() - bossLoc.getY()) < 2 &&
							(distance <= 1 || anglePredicate.test(Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()))));
					}).forEach(player -> {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, true, false, SPELL_NAME);
						MovementUtils.pullTowards(bossLoc, player, PULL_SPEED);
						EffectManager.getInstance().addEffect(player, "FrigidCollapseSlowness", new PercentSpeed(DEBUFF_DURATION, -DEBUFF, "FrigidCollapseSlowness"));
					});

				this.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public int cooldownTicks() {
		return TELEGRAPH_DURATION + Aurora.SPELL_INTERVAL / 2;
	}

}
