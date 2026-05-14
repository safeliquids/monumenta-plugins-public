package com.playmonumenta.plugins.bosses.spells;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.effects.Blindness;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.protocollib.CursedListener;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellRiptide extends Spell {
	private static final String TELEGRAPH_SLOW_SOURCE = "RiptideTelegraphSlow";
	private static final String BLINDNESS_SOURCE = "SpellRiptideNoMelee";

	public final Plugin mPlugin;
	public final LivingEntity mBoss;
	public final int mCooldown;
	public final double mMinRange;
	public final EntityTargets mTarget;
	public final boolean mPreferTarget;
	public final double mVelocity;
	public final double mHitboxSize;
	private final int mTelegraphDuration;
	private final int mDuration;
	public final SoundsList mSoundTelegraph;
	public final SoundsList mSoundsStart;
	public final SoundsList mSoundsHit;
	public final SoundsList mSoundsLand;
	public final ParticlesList mParticleTelegraph;
	public final ParticlesList mParticlesStart;
	public final ParticlesList mParticlesAir;
	public final ParticlesList mParticlesHit;
	public final ParticlesList mParticlesLand;
	public final BiConsumer<Location, Player> mHitAction;
	private final HashSet<UUID> mHitPlayers = new HashSet<>();

	public SpellRiptide(Plugin plugin, LivingEntity boss, int cooldown, double minRange, EntityTargets target,
						boolean preferTarget, double velocity, double hitboxSize, int telegraphDuration, int duration,
	                    SoundsList soundTelegraph, SoundsList soundsStart, SoundsList soundsHit, SoundsList soundsLand,
	                    ParticlesList particleTelegraph, ParticlesList particlesStart, ParticlesList particlesAir, ParticlesList particlesHit, ParticlesList particlesLand,
	                    BiConsumer<Location, Player> hitAction) {
		mPlugin = plugin;
		mBoss = boss;
		mCooldown = cooldown;
		mMinRange = minRange;
		mTarget = target;
		mPreferTarget = preferTarget;
		mVelocity = velocity;
		mHitboxSize = hitboxSize;
		mTelegraphDuration = telegraphDuration;
		mDuration = duration;
		mSoundTelegraph = soundTelegraph;
		mSoundsStart = soundsStart;
		mSoundsHit = soundsHit;
		mSoundsLand = soundsLand;
		mParticleTelegraph = particleTelegraph;
		mParticlesStart = particlesStart;
		mParticlesAir = particlesAir;
		mParticlesHit = particlesHit;
		mParticlesLand = particlesLand;
		mHitAction = hitAction;
	}

	@Override
	public void run() {
		mHitPlayers.clear();
		Location loc = mBoss.getLocation();
		mSoundTelegraph.play(loc);
		mPlugin.mEffectManager.addEffect(mBoss, TELEGRAPH_SLOW_SOURCE, new PercentSpeed(mTelegraphDuration, -0.6, TELEGRAPH_SLOW_SOURCE));
		mBoss.setPose(Pose.SNEAKING, true);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks > mTelegraphDuration) {
					this.cancel();

					mBoss.setPose(Pose.STANDING);
					jump();
					return;
				}
				Location bossLoc = LocationUtils.getHalfHeightLocation(mBoss);
				float finishRatio = 1 - (float) mTicks / mTelegraphDuration;
				Vector vec = new Vector(FastUtils.cosDeg(360 * finishRatio), 0, FastUtils.sinDeg(360 * finishRatio)).multiply(2 * finishRatio);
				mParticleTelegraph.spawn(mBoss, bossLoc.clone().add(vec));
				mParticleTelegraph.spawn(mBoss, bossLoc.clone().subtract(vec));
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void jump() {
		Location bossLoc = mBoss.getLocation();
		List<? extends LivingEntity> targets = mTarget.getTargetsList(mBoss);
		if (targets.isEmpty()) {
			return;
		}
		LivingEntity dashTarget = mPreferTarget && mBoss instanceof Mob bossMob ? bossMob.getTarget() : targets.getFirst();
		if (dashTarget == null) {
			return;
		}
		Location dashLoc = dashTarget.getLocation();
		if (mBoss.getLocation().distance(dashLoc) >= Math.max(0.05, mMinRange)) {
			Vector velocity = LocationUtils.getDirectionTo(dashLoc, bossLoc);
			double distance = dashLoc.distance(bossLoc);
			velocity.setY(velocity.getY() + 0.35);
			velocity.multiply(new Vector(
				mVelocity + (distance + 5) * 0.08,
				mVelocity + distance * 0.02,
				mVelocity + (distance + 5) * 0.08
			));
			mBoss.setVelocity(velocity);

			// Aesthetics
			Effect noMelee = new Blindness(mDuration, null);
			mPlugin.mEffectManager.addEffect(mBoss, BLINDNESS_SOURCE, noMelee);
			playJumpAesthetics();
			BukkitRunnable runnable = new BukkitRunnable() {
				private Location mPrevLoc = bossLoc;
				private int mTicks = 0;

				@Override
				public void run() {
					if (!mBoss.isValid() || mTicks > mDuration) {
						this.cancel();
						return;
					}
					mTicks++;
					if (mTicks >= 5 && mBoss.isOnGround()) {

						this.cancel();
						return;
					}
					playAirAesthetics();

					Location nextLoc = mBoss.getLocation();
					Hitbox.approximateCylinder(mPrevLoc, nextLoc, mHitboxSize, false).getHitPlayers(true).forEach(player ->
						tryHitPlayer(player, mPrevLoc)
					);
					mPrevLoc = nextLoc;
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					mPlugin.mEffectManager.clearEffects(mBoss, BLINDNESS_SOURCE);
					if (mBoss instanceof Mob mob) {
						mob.getPathfinder().findPath(dashTarget);
					}
					playLandingAesthetics();
				}
			};
			mActiveRunnables.add(runnable);
			runnable.runTaskTimer(mPlugin, 1, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void sendMetadataPacket(Player receivingPlayer, int id, boolean riptiding) {
		PacketContainer playerMetadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

		WrappedDataValue animationValue = new WrappedDataValue(8, WrappedDataWatcher.Registry.get(Byte.class, false), (byte) (riptiding ? 4 : 0));

		playerMetadataPacket.getIntegers().write(0, id);
		playerMetadataPacket.getDataValueCollectionModifier().write(0, List.of(animationValue));

		ProtocolLibrary.getProtocolManager().sendServerPacket(receivingPlayer, playerMetadataPacket, false);
	}

	private void playJumpAesthetics() {
		Location bossLocation = mBoss.getLocation();
		mSoundsStart.play(bossLocation);
		mParticlesStart.spawn(mBoss, bossLocation);

		mBoss.setPose(Pose.SPIN_ATTACK, true);
		for (Player viewer : mBoss.getTrackedBy()) {
			if (CursedListener.seesFakePlayer(viewer)) {
				sendMetadataPacket(viewer, mBoss.getEntityId(), true);
			}
		}
	}

	private void playAirAesthetics() {
		// Air Particles
		mParticlesAir.spawn(mBoss, mBoss.getLocation());
		Vector velocity = mBoss.getVelocity();
		if (velocity.lengthSquared() <= 0.01) {
			return;
		}
		double[] yawPitch = VectorUtils.vectorToRotation(velocity);
		mBoss.setRotation((float) yawPitch[0], (float) yawPitch[1]);
	}

	private void tryHitPlayer(Player player, Location loc) {
		// Hit Player
		if (mHitPlayers.contains(player.getUniqueId())) {
			return;
		}
		mHitPlayers.add(player.getUniqueId());
		mHitAction.accept(loc, player);
		mSoundsHit.play(loc);
		mParticlesHit.spawn(mBoss, loc);
	}

	private void playLandingAesthetics() {
		// Landing Sound
		mSoundsLand.play(mBoss.getLocation());
		// Landing Particles
		ParticleUtils.explodingRingEffect(mPlugin, mBoss.getLocation(), 2, 1, 4,
			List.of(
				new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> mParticlesLand.spawn(mBoss, location))
			)
		);

		mBoss.setPose(Pose.STANDING);
		for (Player viewer : mBoss.getTrackedBy()) {
			if (CursedListener.seesFakePlayer(viewer)) {
				sendMetadataPacket(viewer, mBoss.getEntityId(), false);
			}
		}
	}
}
