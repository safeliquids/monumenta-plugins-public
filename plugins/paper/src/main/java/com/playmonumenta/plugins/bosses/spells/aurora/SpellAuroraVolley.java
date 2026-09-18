package com.playmonumenta.plugins.bosses.spells.aurora;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellAuroraVolley extends Spell {
	private static final int DAMAGE = 36;
	private static final int TELEGRAPH_DURATION = 2 * 20;
	private static final double JUMP_VELOCITY = 1.4;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;

	public SpellAuroraVolley(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		Location bossLoc = mBoss.getLocation();
		world.playSound(bossLoc, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 2.5f, 1.0f);
		world.playSound(bossLoc, Sound.ITEM_CROSSBOW_QUICK_CHARGE_2, SoundCategory.HOSTILE, 2.5f, 0.1f);
		world.playSound(bossLoc, Sound.ITEM_CROSSBOW_QUICK_CHARGE_3, SoundCategory.HOSTILE, 2.5f, 0.1f);
		world.playSound(bossLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 3, 1.0f);
		world.playSound(bossLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 3, 1.3f);
		world.playSound(bossLoc, Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 3, 1.5f);

		Vector bossFacing = bossLoc.getDirection();
		bossFacing.setY(0.4);

		mBoss.setVelocity(bossFacing.multiply(JUMP_VELOCITY));

		new PPCircle(Particle.CLOUD, bossLoc, 1)
			.count(35)
			.extra(0.12)
			.spawnAsBoss();

		mActiveTasks.add(new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				Location bossLoc2 = mBoss.getLocation();
				new PartialParticle(Particle.TOTEM, mBoss.getLocation())
					.count(4)
					.delta(0.1, 0.2, 0.1)
					.extra(0.1)
					.spawnAsBoss();

				if (mTicks == TELEGRAPH_DURATION) {
					world.playSound(bossLoc2, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.HOSTILE, 3, 2);
					world.playSound(bossLoc2, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 0.8f);
					world.playSound(bossLoc2, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 3, 0.6f);
				}
				if (mTicks <= TELEGRAPH_DURATION) {
					return;
				}
				world.playSound(bossLoc2, Sound.ENTITY_PHANTOM_SWOOP, SoundCategory.HOSTILE, 3, 1.5f);
				world.playSound(bossLoc2, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 3, 2);
				world.playSound(bossLoc2, Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.HOSTILE, 3, 1);

				List<Projectile> projectiles = EntityUtils.spawnVolley(mBoss, 20, 2.0f, 4.0, EntityType.ARROW);

				for (Projectile projectile : projectiles) {
					AbstractArrow proj = (AbstractArrow) projectile;

					proj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
					proj.setPierceLevel(10);
					proj.setDamage(DAMAGE);

					mActiveTasks.add(new BukkitRunnable() {
						@Override
						public void run() {
							// spawn particle
							new PartialParticle(Particle.CRIT_MAGIC, proj.getLocation())
								.extra(0.25)
								.spawnAsEnemy();

							if (proj.isInBlock() || !proj.isValid()) {
								proj.remove();
								this.cancel();
							}
						}
					}.runTaskTimer(mPlugin, 0, 1));
				}

				this.cancel();
			}

		}.runTaskTimer(mPlugin, 0, 1));
	}

	@Override
	public int cooldownTicks() {
		return TELEGRAPH_DURATION + 5 * 20;
	}

}
