package com.playmonumenta.plugins.abilities.mage.elementalist;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Mage;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.elementalist.StarfallCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.Comparator;
import java.util.List;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class Starfall extends Ability {
	public static final String NAME = "Starfall";
	public static final ClassAbility ABILITY = ClassAbility.STARFALL;

	public static final int DAMAGE_1 = 15;
	public static final int DAMAGE_2 = 27;
	public static final int RADIUS = 6; // Size of Starfall explosion
	public static final int RANGE = 25; // Maximum distance away that you can target
	public static final int FIRE_TICKS = 5 * Constants.TICKS_PER_SECOND;
	public static final float KNOCKBACK = 0.7f;
	public static final int COOLDOWN_TICKS = 18 * 20;
	public static final double FALL_INCREMENT = 0.25;
	public static final double HITBOX = 1.2; // Blocks

	public static final String CHARM_DAMAGE = "Starfall Damage";
	public static final String CHARM_RANGE = "Starfall Range";
	public static final String CHARM_COOLDOWN = "Starfall Cooldown";
	public static final String CHARM_RADIUS = "Starfall Radius";
	public static final String CHARM_FIRE = "Starfall Fire Duration";
	public static final String CHARM_FALL_SPEED = "Starfall Fall Speed";

	public static final AbilityInfo<Starfall> INFO =
		new AbilityInfo<>(Starfall.class, NAME, Starfall::new)
			.linkedSpell(ABILITY)
			.scoreboardId(NAME)
			.shorthandName("SF")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Summon a meteor, which damages and ignites mobs upon impact.")
			.cooldown(COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Starfall::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.MAGMA_BLOCK);

	private final double mLevelDamage;
	private final double mRange;
	private final double mRadius;
	private final int mFireDuration;
	private final double mFallIncrement;
	private final StarfallCS mCosmetic;

	public Starfall(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLevelDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, RANGE);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mFireDuration = CharmManager.getDuration(mPlayer, CHARM_FIRE, FIRE_TICKS);
		mFallIncrement = CharmManager.getExtraPercent(mPlayer, CHARM_FALL_SPEED, FALL_INCREMENT);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new StarfallCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location loc = mPlayer.getEyeLocation();
		World world = mPlayer.getWorld();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);
		double damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		mCosmetic.starfallCastEffect(world, mPlayer, mPlayer.getLocation());
		Vector dir = loc.getDirection().normalize();

		// Go until you hit a block or max range
		RayTraceResult raycast = world.rayTraceBlocks(loc, dir, mRange, FluidCollisionMode.NEVER, true);
		Location fallLoc;
		if (raycast == null) {
			fallLoc = loc.clone().add(dir.clone().multiply(mRange));
		} else {
			fallLoc = raycast.getHitPosition().toLocation(world);
			if (!(BlockFace.DOWN.equals(raycast.getHitBlockFace()) || BlockFace.UP.equals(raycast.getHitBlockFace()))) {
				// If you hit the side of a block, embed slightly into it
				fallLoc.add(dir.clone().multiply(0.07));
			}
		}

		// Check for mobs at the landing location: if you would hit anything, then definitely go there.
		if (EntityUtils.getNearbyMobs(fallLoc, 0.72 * mRadius, mPlayer).isEmpty() && fallLoc.distanceSquared(loc) > 1) {
			// Otherwise, check for mobs along the length of the raycast.
			List<LivingEntity> mobs = Hitbox.approximateCylinder(loc.clone().add(dir), fallLoc, HITBOX, true).accuracy(0.5).getHitMobs();
			if (!mobs.isEmpty()) {
				// Sort by distance along the ray
				mobs.sort(Comparator.comparingDouble(mob -> LocationUtils.getVectorTo(mob.getLocation(), loc).dot(dir)));
				fallLoc = loc.clone().add(dir.clone().multiply(
					Math.min(mRange, LocationUtils.getVectorTo(mobs.getLast().getLocation(), loc).dot(dir))));
			}
		}

		mCosmetic.starfallCastTrail(fallLoc, mPlayer);
		launchMeteor(fallLoc, mPlayer.getLocation(), playerItemStats, damage);

		return true;
	}

	private void launchMeteor(final Location loc, final Location ogPlayerLoc, final ItemStatManager.PlayerItemStats playerItemStats, final double damage) {
		Location ogLoc = loc.clone();
		loc.add(0, 40, 0);

		new BukkitRunnable() {
			double mT = 0;
			@Nullable Location mGroundLoc = null;

			@Override
			public void run() {
				mGroundLoc = null; // "has not been calculated this tick"
				mT += 1;
				World world = mPlayer.getWorld();
				for (int i = 0; i < 8; i++) {
					loc.subtract(0, mFallIncrement, 0);
					double height = loc.getY() - ogLoc.getY(); // Height above the landing location
					if (height <= 2) { // Meteor gains sentience
						// Always stop on a block
						if (!loc.isChunkLoaded() || loc.getBlock().isSolid()) {
							cancel();
							break;
						}

						// If you smack an enemy, check if hitting the ground would still hit this enemy. Else explode on the spot.
						List<LivingEntity> directHitMobs = EntityUtils.getNearbyMobs(loc, Math.min(mRadius, HITBOX), mPlayer);
						if (!directHitMobs.isEmpty()) {
							if (mGroundLoc == null) {
								mGroundLoc = LocationUtils.fallToGround(loc, 0); // If we ever have a world with combat that goes below Y = 0, this minimum height needs to be changed!
								List<LivingEntity> groundHitMobs = EntityUtils.getNearbyMobs(mGroundLoc, mRadius, mPlayer);
								directHitMobs.removeAll(groundHitMobs);
								if (!directHitMobs.isEmpty()) {
									cancel();
									break;
								}
							}
						}

						// Go down as far as you can WITHOUT missing the topmost enemy
						List<LivingEntity> enemiesAbove =
							EntityUtils.getNearbyMobs(loc, mRadius, mPlayer);
						List<LivingEntity> enemiesStillHit =
							EntityUtils.getNearbyMobs(loc.clone().add(0, -mFallIncrement, 0), mRadius, mPlayer);
						enemiesAbove.removeAll(enemiesStillHit);
						if (!enemiesAbove.isEmpty()) {
							cancel();
							break;
						}
					}
				}
				mCosmetic.starfallFallEffect(world, mPlayer, loc, ogPlayerLoc, ogLoc, mT);

				if (mT >= Constants.TICKS_PER_SECOND * 30) {
					cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				mCosmetic.starfallLandEffect(mPlayer.getWorld(), mPlayer, loc, ogPlayerLoc, mRadius);
				Hitbox hitbox = new Hitbox.SphereHitbox(loc, mRadius);
				for (LivingEntity e : hitbox.getHitMobs()) {
					EntityUtils.applyFire(mPlugin, mFireDuration, e, mPlayer, playerItemStats);
					DamageUtils.damage(mPlayer, e, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), damage, true, true, false);
					MovementUtils.knockAway(loc, e, KNOCKBACK, true);
				}
				super.cancel();
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static Description<Starfall> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Call down a falling meteor that explodes")
			.addLine("upon landing, dealing *Fire* damage,").styles(Mage.FIRE_COLOR)
			.addLine("igniting mobs, and knocking them away.")
			.addLine()
			.addStat("Damage: %d1 (s)")
				.statValues(stat(a -> a.mLevelDamage, DAMAGE_1))
			.addStat("Effect: Fire for %t")
				.statValues(stat(a -> a.mFireDuration, FIRE_TICKS))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Range: %r")
				.statValues(stat(a -> a.mRange, RANGE))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN_TICKS))
			.addDashedLine();
	}

	private static Description<Starfall> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Starfall*'s damage.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 -> %d2 (s)")
				.statValues(stat(DAMAGE_1), stat(a -> a.mLevelDamage, DAMAGE_2))
			.addDashedLine();
	}
}
