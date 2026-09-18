package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityWithChargesOrStacks;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.GaleShotCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.Multishot;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class GaleShot extends Ability implements AbilityWithChargesOrStacks, AbilityWithDuration {
	public static final AbilityInfo<GaleShot> INFO =
		new AbilityInfo<>(GaleShot.class, "Gale Shot", GaleShot::new)
			.linkedSpell(ClassAbility.GALE_SHOT)
			.scoreboardId("GaleShot")
			.shorthandName("GS")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Your projectile will be enhanced after casting multiple skills.")
			.displayItem(Material.BONE_MEAL);

	public static final String GALE_SHOT_PROJECTILE_METAKEY = "GaleShotProjectile";
	public static final String GALE_SHOT_VOLLEY_IMBUE = "GaleShotVolleyImbue";

	private static final String GALE_SHOT_IMBUEMENT = "GaleShotImbuement";
	private static final double DAMAGE_L1 = 10;
	private static final double DAMAGE_L2 = 12;
	private static final double DAMAGE_PERCENT_L1 = 1.0;
	private static final double DAMAGE_PERCENT_L2 = 1.2;
	private static final String GALE_SHOT_IFRAME_METAKEY = "GaleShotIFrame";
	private static final int ABILITY_REQ = 2;
	private static final int DURATION = Constants.TICKS_PER_SECOND * 12;
	private static final int SLOWNESS_DURATION = Constants.TICKS_PER_SECOND * 3;
	private static final double SLOWNESS_AMPLIFIER = 0.25;
	private static final double SIZE = 0.75;
	private static final double VERTICAL_LAUNCH = 0.55;
	private static final double KB_VEL_BASE = 1.5;
	private static final double KB_VEL_PUNCH_LEVEL = 0.5;
	private static final int SHOT_REQ = 2;

	public static final String CHARM_DAMAGE_FLAT = "Gale Shot Flat Damage";
	public static final String CHARM_DAMAGE_PERCENT = "Gale Shot Damage Multiplier";
	public static final String CHARM_RANGE = "Gale Shot Range";
	public static final String CHARM_ABILITY_REQUIREMENT = "Gale Shot Ability Requirement";
	public static final String CHARM_SHOT_REQUIREMENT = "Gale Shot Landing Requirement";
	public static final String CHARM_DURATION = "Gale Shot Duration";
	public static final String CHARM_SLOWNESS_DURATION = "Gale Shot Slowness Duration";
	public static final String CHARM_SLOWNESS_AMPLIFIER = "Gale Shot Slowness Amplifier";
	public static final String CHARM_COUNT = "Gale Shot Count";
	public static final String CHARM_SIZE = "Gale Shot Size";

	private final double mDamageFlat;
	private final double mDamagePercent;
	private final int mAbilityRequirement;
	private final int mDuration;
	private final double mSlownessAmplifier;
	private final int mSlownessDuration;
	private final int mShotCount;
	private final int mShotRequirement;
	private final double mSize;
	private final GaleShotCS mCosmetic;
	private int mAbilityCount = 0;

	private final WeakHashMap<LivingEntity, Integer> mMarkedMobs = new WeakHashMap<>();
	private @Nullable Sharpshooter mSharpshooter;
	private int mCount;
	private int mCastTime = Bukkit.getCurrentTick();

	public GaleShot(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_FLAT, isLevelOne() ? DAMAGE_L1 : DAMAGE_L2);
		mDamagePercent = CharmManager.getExtraPercent(mPlayer, CHARM_DAMAGE_PERCENT, isLevelOne() ? DAMAGE_PERCENT_L1 : DAMAGE_PERCENT_L2);
		mAbilityRequirement = Math.max(0, ABILITY_REQ + (int) CharmManager.getLevel(mPlayer, CHARM_ABILITY_REQUIREMENT));
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_SLOWNESS_DURATION, SLOWNESS_DURATION);
		mShotRequirement = Math.max(0, SHOT_REQ + (int) CharmManager.getLevel(mPlayer, CHARM_SHOT_REQUIREMENT));
		mSlownessAmplifier = CharmManager.getExtraPercent(mPlayer, CHARM_SLOWNESS_AMPLIFIER, SLOWNESS_AMPLIFIER);
		mShotCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_COUNT);
		mSize = CharmManager.getRadius(mPlayer, CHARM_SIZE, SIZE);

		Bukkit.getScheduler().runTask(plugin, () ->
			mSharpshooter = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(mPlayer, Sharpshooter.class));

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new GaleShotCS());
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		int currTick = Bukkit.getServer().getCurrentTick();

		if (!EntityUtils.isAbilityTriggeringProjectile(projectile, false)
			|| MetadataUtils.happenedThisTick(mPlayer, GALE_SHOT_VOLLEY_IMBUE)
			|| Grappling.playerHoldingHook(mPlayer)
			|| currTick - mCastTime < 1
			|| !hasImbuement()
			|| projectile.hasMetadata(Multishot.MULTISHOT_SIDE_METADATA)) {
			return true;
		}

		mCastTime = currTick;
		mCount--;
		mPlugin.mEffectManager.clearEffects(mPlayer, GALE_SHOT_IMBUEMENT);
		if (mCount > 0) {
			galeShotImbuement();
		}

		ItemStack mainHand = mPlayer.getInventory().getItemInMainHand();
		double bowDraw = projectile instanceof AbstractArrow arrow ? PlayerUtils.calculateBowDraw(arrow) : 1;
		double projSpeed = ItemUtils.getVanillaProjectileSpeed(mainHand) * bowDraw;
		EntityType projectileType;
		if (mPlayer.isUnderWater() || BlockUtils.containsWater(mPlayer.getLocation().getBlock())) {
			projectileType = EntityType.TRIDENT;
		} else if (mainHand.getType() == Material.SNOWBALL) {
			projectileType = EntityType.SNOWBALL;
		} else {
			projectileType = EntityType.ARROW;
		}
		Projectile galeProjectile = EntityUtils.spawnProjectile(mPlayer, 0, 0, new Vector(0, 0, 0), (float) projSpeed, projectileType);

		// Destroy the original projectile and use an arrow instead because it pierces

		AbilityUtils.inheritProjectileStats(mPlayer, galeProjectile, projectile); // Needed for Explosive aspect transfer!
		@Nullable
		ItemStatManager.PlayerItemStats stats = DamageListener.getProjectileItemStats(galeProjectile);
		if (stats != null) {
			ItemStatManager.PlayerItemStats.ItemStatsMap statsMap = stats.getItemStats();
			double originalProjDamage = statsMap.get(AttributeType.PROJECTILE_DAMAGE_ADD);
			ItemStat projAddStat = Objects.requireNonNull(AttributeType.PROJECTILE_DAMAGE_ADD.getItemStat());
			statsMap.set(projAddStat, mDamageFlat + mDamagePercent * originalProjDamage * bowDraw);

			for (EnchantmentType enchant : AbilityUtils.PROJ_DAMAGE_ENCHANTS) {
				ItemStat stat = Objects.requireNonNull(enchant.getItemStat());

				statsMap.set(stat, statsMap.get(stat) * mDamagePercent * bowDraw);
			}
		}

		galeProjectile.setVisibleByDefault(false); // BEFORE the launch event

		if (galeProjectile instanceof AbstractArrow galeArrow) {
			galeArrow.setPierceLevel(67);
			galeArrow.setCritical(true);
			galeArrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
		}
		galeProjectile.setShooter(mPlayer);
		galeProjectile.setMetadata(GALE_SHOT_PROJECTILE_METAKEY, new FixedMetadataValue(mPlugin, 0));

		ProjectileLaunchEvent event = new ProjectileLaunchEvent(galeProjectile);
		Bukkit.getPluginManager().callEvent(event);

		AbilityUtils.removeProjectile(projectile);

		if (mSharpshooter != null) {
			mSharpshooter.doNotTrack(projectile);
			mSharpshooter.trackArrow(galeProjectile);
		}

		updateAbility();
		PlayerUtils.callAbilityCastEvent(mPlayer, this, ClassAbility.GALE_SHOT, 0);
		mCosmetic.fire(mPlayer, galeProjectile);

		final int sharpStacks = Sharpshooter.checkSharpshooterType(projectile, mPlayer.getInventory().getItemInMainHand()) + 1;
		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
		final double punch = playerItemStats != null ? playerItemStats.getItemStats().get(EnchantmentType.PUNCH) : 0;

		new BukkitRunnable() {
			final Projectile mGaleProjectile = galeProjectile;
			Location mPastLoc = mGaleProjectile.getLocation();
			Hitbox mCylHitbox = Hitbox.approximateCylinder(mPastLoc, mGaleProjectile.getLocation(), 0, true); // Satisfy the null check

			@Override
			public void run() {
				if (!mGaleProjectile.isValid()
					|| mGaleProjectile.getTicksLived() > 200
					|| (mGaleProjectile instanceof AbstractArrow mGaleArrow && mGaleArrow.isInBlock())) {
					this.cancel();
				}
				mCylHitbox = Hitbox.approximateCylinder(mPastLoc, mGaleProjectile.getLocation(), mSize, true).accuracy(0.6);
				List<Entity> hitMobs = mCylHitbox.getHitEntities(entity -> EntityUtils.isHostileMob(entity) || WindBomb.isWindBomb(entity));

				if (!hitMobs.isEmpty() && mSharpshooter != null && mSharpshooter.isTracking(mGaleProjectile)) {
					mSharpshooter.addStacks(sharpStacks);
					mSharpshooter.doNotTrack(mGaleProjectile);
				}

				for (Entity entity : hitMobs) {
					if (!(entity instanceof LivingEntity enemy)) {
						// Should never happen
						continue;
					}

					// This hacky iframe system needs to stay because of the current L2.
					if (MetadataUtils.checkOnceInRecentTicks(mPlugin, enemy, GALE_SHOT_IFRAME_METAKEY + mPlayer.getUniqueId(), 5)) {
						DamageEvent.Metadata metadata = new DamageEvent.Metadata(DamageEvent.DamageType.PROJECTILE, ClassAbility.GALE_SHOT, null, null);
						// Damage handled by damage pipeline because it thinks this is an arrow
						DamageUtils.damage(mPlayer, mGaleProjectile, enemy, metadata, mDamageFlat, true, false, false);

						Location enemyLoc = enemy.getLocation();
						enemyLoc.setY(Math.clamp(galeProjectile.getY(), enemy.getY(), enemy.getHeight() + enemy.getY()));

						mCosmetic.hit(mPlayer, enemyLoc);

						if (WindBomb.isWindBomb(enemy)) {
							break; // Don't deal KB to wind bombs
						}

						double speed = KB_VEL_BASE + KB_VEL_PUNCH_LEVEL * punch;
						Vector vector = mGaleProjectile.getVelocity().normalize().multiply(speed);
						vector.setY(Math.max(vector.getY(), -VERTICAL_LAUNCH / 2));
						vector.add(new Vector(0, VERTICAL_LAUNCH, 0));
						MovementUtils.knockAwayDirection(vector, enemy, 0.5f);

						if (isLevelTwo()) {
							EntityUtils.applySlow(mPlugin, mSlownessDuration, mSlownessAmplifier, enemy);

							mMarkedMobs.compute(enemy, (k, v) -> {
								int next = (v == null ? 1 : v + 1);

								boolean canImbue = next >= mShotRequirement;
								if (canImbue) {
									mAbilityCount = mAbilityRequirement;
									imbue();
									updateAbility();
									return 0; // Allows gaining gale shot from the same mob
								} else {
									return next;
								}
							});
						}
					}
				}
				mPastLoc = mGaleProjectile.getLocation();
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		ClassAbility ability = event.getSpell();

		if (hasImbuement()) {
			// If volley with imbuement, give the ability a tick later
			if (ability.equals(ClassAbility.VOLLEY)) {
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					if (mCount == 0) {
						mAbilityCount++;
						updateAbility();
					}
				}, 1);
			}
			return false;
		}

		if (ability == null
			|| ability.equals(ClassAbility.GALE_SHOT)
			|| ability.equals(ClassAbility.SWIFTNESS)) {
			return false;
		}

		if (++mAbilityCount >= mAbilityRequirement) {
			// Prevent Volley from instant casting Gale Shot
			if (ability.equals(ClassAbility.VOLLEY)) {
				MetadataUtils.markThisTick(mPlugin, mPlayer, GALE_SHOT_VOLLEY_IMBUE);
			}
			imbue();
		}
		updateAbility();

		return false;
	}


	private void imbue() {
		if (hasImbuement()) {
			return;
		}

		mCosmetic.imbue(mPlayer.getLocation());
		mCount = mShotCount;
		galeShotImbuement();
	}

	private void galeShotImbuement() {
		mPlugin.mEffectManager.addEffect(mPlayer, GALE_SHOT_IMBUEMENT, new Aesthetics(mDuration,
			(entity, fourHertz, twoHertz, oneHertz) -> mCosmetic.tick(mPlayer, mPlayer.getLocation()),
			entity -> Bukkit.getScheduler().runTask(mPlugin, () -> {
				if (mCount <= 0) {
					mAbilityCount = 0;
					mCount = 0;
					updateAbility();
				}
			})
		).deleteOnAbilityUpdate(true));
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mMarkedMobs.entrySet().removeIf((entry) -> !entry.getKey().isValid() || entry.getKey().isDead());
		}
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (!proj.hasMetadata(GALE_SHOT_PROJECTILE_METAKEY)) {
			return;
		}
		if (event.getHitEntity() != null) {
			event.setCancelled(true);
		} else if (event.getHitBlock() != null) {
			mCosmetic.hitBlock(mPlayer, proj.getLocation());
			proj.remove();
		}
	}

	private boolean hasImbuement() {
		return mPlugin.mEffectManager.hasEffect(mPlayer, GALE_SHOT_IMBUEMENT);
	}

	@Override
	public int getCharges() {
		return mAbilityCount;
	}

	@Override
	public int getMaxCharges() {
		return mAbilityRequirement;
	}

	@Override
	public @Nullable String getMode() {
		return mAbilityCount == mAbilityRequirement ? "max" : null;
	}

	private static Description<GaleShot> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Casting %d abilities turns your next")
			.statValues(stat(a -> a.mAbilityRequirement, ABILITY_REQ))
			.addLine("projectile into a *Gale Shot*, granting").styles(UNDERLINED)
			.addLine("infinite pierce and increased damage.")
			.addLine("(Only imbues the central projectile.)")
			.addLine()
			.addStat("Damage: %d1 + %p1 (p) (of weapon damage)")
			.statValues(stat(a -> a.mDamageFlat, DAMAGE_L1), stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L1))
			.addIf((a, p) -> a != null && a.mShotCount != 1, desc -> desc
				.addStat("Shots: %d")
				.statValues(stat(a -> a.mShotCount, 1)))
			.addDashedLine();
	}

	private static Description<GaleShot> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Gale Shot*'s damage. *Gale Shot* now inflicts slowness.").styles(UNDERLINED, UNDERLINED)
			.addLine()
			.addStatComparison("Damage: %d1 + %p1 -> %d2 + %p2 (p) (of weapon damage)")
			.statValues(stat(DAMAGE_L1), stat(DAMAGE_PERCENT_L1),
				stat(a -> a.mDamageFlat, DAMAGE_L2), stat(a -> a.mDamagePercent, DAMAGE_PERCENT_L2))
			.addStat("Effect: %p Slowness for %t")
			.statValues(stat(a -> a.mSlownessAmplifier, SLOWNESS_AMPLIFIER), stat(a -> a.mSlownessDuration, SLOWNESS_DURATION))
			.addLine()
			.addLine("Landing %d *Gale Shots* on the same mob refreshes *Gale Shot*.").styles(UNDERLINED, UNDERLINED)
			.statValues(stat(a -> a.mShotRequirement, SHOT_REQ))
			.addDashedLine();
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		Effect galeBuff = mPlugin.mEffectManager.getActiveEffect(mPlayer, GALE_SHOT_IMBUEMENT);
		return galeBuff != null ? galeBuff.getDuration() : 0;
	}
}
