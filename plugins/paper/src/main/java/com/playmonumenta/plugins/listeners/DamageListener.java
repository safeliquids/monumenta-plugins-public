package com.playmonumenta.plugins.listeners;

import com.google.common.collect.ImmutableMultimap;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.bosses.bosses.TrainingDummyBoss;
import com.playmonumenta.plugins.bosses.parameters.Tokenizer;
import com.playmonumenta.plugins.commands.DamageTraceCommand;
import com.playmonumenta.plugins.commands.ShowMyDpsCommand;
import com.playmonumenta.plugins.depths.abilities.steelsage.RapidFire;
import com.playmonumenta.plugins.effects.ProjectileIframe;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageShieldedEvent;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.player.activity.ActivityManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MMLog;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

public class DamageListener implements Listener {

	private final Plugin mPlugin;
	public static final String DO_NOT_REPLACE_METADATA = "PlayerItemStatsMapUnreplacable";
	@SuppressWarnings("deprecation")
	public static final DamageModifier VANILLA_IFRAMES_MODIFIER = DamageModifier.valueOf("IFRAMES");

	private static final WeakHashMap<UUID, PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();

	public DamageListener(Plugin plugin) {
		mPlugin = plugin;
	}

	// Bukkit deprecates EntityDamageEvent.DamageModifier.
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			// don't allow dealing damage across worlds, no matter how (can e.g. happen via damage over time effects or delayed damage)
			if (event.getEntity().getWorld() != entityDamageByEntityEvent.getDamager().getWorld()
				|| (entityDamageByEntityEvent.getDamager() instanceof Projectile projectile
				&& projectile.getShooter() instanceof Entity shooter
				&& event.getEntity().getWorld() != shooter.getWorld())) {
				event.setCancelled(true);
				return;
			}

			if (event.getCause().equals(DamageCause.ENTITY_EXPLOSION)
				&& event.getEntity() instanceof LivingEntity le) {
				Entity damager = entityDamageByEntityEvent.getDamager();
				if (damager instanceof Creeper creeper) {
					event.setDamage(EntityUtils.calculateCreeperExplosionDamage(creeper, le, event.getDamage()));
				}
			}
			if (entityDamageByEntityEvent.getDamager() instanceof WitherSkull witherSkull
				&& witherSkull.getShooter() instanceof Wither wither) {
				event.setDamage(EntityUtils.getAttributeOrDefault(wither, Attribute.GENERIC_ATTACK_DAMAGE, event.getDamage()));
			}

			if (event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
				event.setDamage(1);
			}
		}

		/*
		 * Puts the wrapper DamageEvent on EntityDamageEvents not caused by the
		 * plugin (DamageCause.CUSTOM), which should wrap events manually to
		 * set the correct DamageType.
		 */
		double originalDamage = event.getDamage();
		if (event.getEntity() instanceof LivingEntity le) {
			if (DamageUtils.nextEventMetadata != null) {
				DamageEvent.Metadata nextEventMetadata = DamageUtils.nextEventMetadata;
				DamageUtils.nextEventMetadata = null;
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le, nextEventMetadata));
			} else if (event.getCause() != DamageCause.CUSTOM) {
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le));
			}
		}
		// If the damage is blocked, revert to the initial damage to make sure the shield gets proper durability damage.
		// This also prevents knockback going through shields sometimes for some reason.
		// Needs to check for holding a shield since the mob's attack may have disabled it.
		if (event.getDamage(DamageModifier.BLOCKING) < 0
			&& event.getEntity() instanceof Player player
			&& player.getActiveItem().getType() == Material.SHIELD) {
			event.setDamage(originalDamage);
		}

		// Negative damage fixes (negative damage can make mobs unkillable)
		if (event.getFinalDamage() < 0 && event.getFinalDamage() > -0.1) {
			// Small amount of negative damage - can happen as the Paper damage calculation mixes floats and doubles
			// Add the final damage to the base damage to make the calculation 0, while still damaging absorption
			// Uses Math.nextUp to prevent a small final damage value from not affecting the addition
			event.setDamage(DamageModifier.BASE, Math.nextUp(event.getDamage()) - event.getFinalDamage());
		}
		if (event.getDamage() < 0 || event.getFinalDamage() < 0) {
			// (Still) negative: log and fix
			MMLog.warning("Negative damage dealt! finalDamage=" + event.getFinalDamage() + ", "
				+ Arrays.stream(DamageModifier.values()).map(mod -> mod + "=" + event.getDamage(mod)).collect(Collectors.joining(", ")));
			if (!(event.getEntity() instanceof Player)) { // the negative damage bug doesn't apply to players, and can cause issues with absorption making players invulnerable
				event.setDamage(0);
			}
		}

		if (!Double.isFinite(event.getDamage()) || !Double.isFinite(event.getFinalDamage())) {
			// NaN or infinite damage dealt: log and set damage to 0
			MMLog.warning("Non-finite damage dealt! finalDamage=" + event.getFinalDamage() + ", "
				+ Arrays.stream(EntityDamageEvent.DamageModifier.values()).map(mod -> mod + "=" + event.getDamage(mod)).collect(Collectors.joining(", ")), new Exception());
			event.setDamage(0);
		}

		// Damaging a dead entity can make it immortal, so prevent that
		if (!event.getEntity().isValid()) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile projectile = event.getEntity();
		ProjectileSource source = projectile.getShooter();

		// For projectiles whose itemStats are modified prior to launch event.
		if (source instanceof Player player && !projectile.hasMetadata(DO_NOT_REPLACE_METADATA)) {
			addProjectileItemStats(projectile, player);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		// Why does this exist?
		event.updateDamageWithMultiplier(EntityUtils.vulnerabilityMult(damagee), DamageEvent.DamageType.getScalableDamageType());

		// If this event was caused by /kill, the entity should immediately die with no further processing
		if (event.getCause().equals(DamageCause.KILL)) {
			damagee.setHealth(0);
		}

		// Player getting damaged
		if (damagee instanceof Player player) {
			mPlugin.mItemStatManager.onHurt(mPlugin, player, event, damager, source);
			mPlugin.mAbilityManager.onHurt(player, event, damager, source);
			mPlugin.mDoubleJumpManager.onHurt(player, event);
			if (event.isBlockedByShield()) {
				Bukkit.getPluginManager().callEvent(new DamageShieldedEvent(player, source, event.getCause(), -1));
			}
			if (event.getFinalDamage(true) >= player.getHealth() && !event.isCancelled()) {
				mPlugin.mAbilityManager.onHurtFatal(player, event);
				mPlugin.mItemStatManager.onHurtFatal(mPlugin, player, event);
			}
		} else {
			if (source instanceof Player player) {
				// Check if projectile
				if (damager instanceof Projectile proj) {
					PlayerItemStats playerItemStats = mPlayerItemStatsMap.get(proj.getUniqueId());

					if (playerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, playerItemStats, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				} else {
					PlayerItemStats eventPlayerItemStats = event.getPlayerItemStats();
					if (eventPlayerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, eventPlayerItemStats, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					} else {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				}
				// Check for activity purposes
				if (damagee.customName() != null && !damagee.getScoreboardTags().contains(TrainingDummyBoss.identityTag) && EntityUtils.isHostileMob(damagee)) {
					ActivityManager.getManager().addDamageDealt(player, Math.min(event.getDamage(), damagee.getHealth()));
				}
			}
		}
		if (!event.isLifelineCancel()) {
			mPlugin.mEffectManager.damageEvent(event);
			GalleryManager.onEntityDamageEvent(event);
		}

		boolean isNotRapidfire = damager instanceof Projectile proj
			&& !(proj.hasMetadata(RapidFire.META_DATA_TAG)
			|| proj.hasMetadata(QuiverStorm.ARROW_METADATA));

		// Reverb custom enchant needs to calculate final damage after effects are applied.
		if (source instanceof Player player) {
			PlayerItemStats eventPlayerItemStats = event.getPlayerItemStats();
			if (eventPlayerItemStats != null) {
				mPlugin.mItemStatManager.onDamageDelayed(mPlugin, player, eventPlayerItemStats, event, damagee);
			} else {
				mPlugin.mItemStatManager.onDamageDelayed(mPlugin, player, event, damagee);
			}
			mPlugin.mAbilityManager.onDamageDelayed(player, event, damagee);
		}

		// Projectile Iframes rework. Need to be placed at the end in order to get final damage.
		if (!event.isCancelled() && source instanceof Player
			&& (isNotRapidfire || ElementalArrows.isElementalArrowDamage(event))
			&& event.getType() != DamageEvent.DamageType.TRUE) {
			double damage = event.getDamage();

			ProjectileIframe projectileIframe = mPlugin.mEffectManager.getActiveEffect(damagee, ProjectileIframe.class);
			if (projectileIframe != null) {
				int duration = projectileIframe.getDuration();
				double magnitude = projectileIframe.getMagnitude();

				// If incoming damage is greater than magnitude, subtract and deal damage.
				// Otherwise, do nothing.
				if (damage > magnitude) {
					double extraDamage = damage - magnitude;
					event.getEvent().setDamage(VANILLA_IFRAMES_MODIFIER, 0);
					event.setDamageCap(extraDamage);
					mPlugin.mEffectManager.addEffect(damagee, ProjectileIframe.SOURCE, new ProjectileIframe(duration, damage));
				} else {
					event.setDamageCap(0.0);
				}
			} else {
				mPlugin.mEffectManager.addEffect(damagee, ProjectileIframe.SOURCE, new ProjectileIframe(ProjectileIframe.IFRAME_DURATION, damage));
			}
		}

		if (event.getSource() instanceof Player player && player.getScoreboardTags().contains(DamageTraceCommand.TAG)) {
			sendDebugMessage(player, event);
		}
		if (event.getDamagee() instanceof Player player && player.getScoreboardTags().contains(DamageTraceCommand.TAG)) {
			sendDebugMessage(player, event);
		}

		ShowMyDpsCommand.onDamage(event);
	}

	public static void sendDebugMessage(Player player, DamageEvent event) {
		ImmutableMultimap<DamageEvent.DamageModifier.Stage, DamageEvent.DamageModifier> damageModifiers = event.damageModifiersCopy();
		player.sendMessage(Component.text("[Damage Trace] Click to reveal %d damage modifiers".formatted(damageModifiers.size()), NamedTextColor.YELLOW)
			.clickEvent(ClickEvent.callback(audience -> {
				for (DamageEvent.DamageModifier modifier : damageModifiers.values()) {
					// Syntax is similar to bosstag, so I will use that to pretty print it!
					String string = modifier.toString();
					audience.sendMessage(Component.text("-".repeat(string.indexOf("\n")), NamedTextColor.GRAY));
					audience.sendMessage(new Tokenizer(string).getTokens().syntaxHighlight());
				}
			}))
		);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void damageShieldedEvent(DamageShieldedEvent event) {
		mPlugin.mItemStatManager.onDamageShielded(mPlugin, event.getPlayer(), event);
	}

	public static @Nullable PlayerItemStats getProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.get(proj.getUniqueId());
	}

	public static void addProjectileItemStats(Projectile proj, Player player) {
		Plugin plugin = Plugin.getInstance();
		PlayerItemStats stats = plugin.mItemStatManager.getPlayerItemStatsCopy(player);
		addProjectileItemStats(proj.getUniqueId(), proj, stats);
	}

	public static void addProjectileItemStats(UUID uuid, Projectile proj, PlayerItemStats stats) {
		appendProjectileStats(stats, proj);
		addProjectileItemStats(uuid, stats);
	}

	public static void addProjectileItemStats(UUID uuid, PlayerItemStats stats) {
		mPlayerItemStatsMap.put(uuid, stats);
	}

	/**
	 * To add the stats of a projectile onto a PlayerItemStats.
	 *
	 * @param stats The PlayerItemStats to add onto
	 * @param proj  The projectile with attributes
	 */
	public static void appendProjectileStats(PlayerItemStats stats, Projectile proj) {
		PlayerItemStats.ItemStatsMap map = stats.getItemStats();

		if (proj instanceof AbstractArrow arrow && !(proj instanceof Trident)) {
			ItemStack item = arrow.getItemStack();
			if (item.getType() != Material.AIR) {
				NBT.get(item, nbt -> {
					ReadableNBT enchantments = ItemStatUtils.getEnchantments(nbt);

					for (EnchantmentType ench : EnchantmentType.PROJECTILE_ENCHANTMENTS) {
						int level = ItemStatUtils.getEnchantmentLevel(enchantments, ench);
						if (level > 0) {
							map.add(Objects.requireNonNull(ench.getItemStat()), level);
						}
					}

					ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(nbt);

					for (AttributeType attr : AttributeType.PROJECTILE_ATTRIBUTE_TYPES) {
						double value = ItemStatUtils.getAttributeAmount(attributes, attr, Operation.MULTIPLY, Slot.PROJECTILE);
						if (value != 0) {
							ItemStat stat = Objects.requireNonNull(attr.getItemStat());
							if (map.get(stat) == stat.getDefaultValue()) {
								value += stat.getDefaultValue();
							}
							map.add(Objects.requireNonNull(attr.getItemStat()), value);
						}
					}
				});
			}
		}
	}

	public static PlayerItemStats removeProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.remove(proj.getUniqueId());
	}
}
