package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.bosses.bosses.BlockPlacerBoss;
import com.playmonumenta.plugins.bosses.bosses.WormBoss;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.overrides.UnbreakableOnBedrockOverride;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LootingLimiter implements Listener {

	public static final String DEBUG_PERMISSION = "monumenta.lootlimiter.debug";

	private static final int PLAYER_SEARCH_RADIUS = 20;
	private static final int MOB_AND_SPAWNER_SEARCH_RADIUS = 12; // Must not exceed getChunk size (16) - reach distance (3) = 13
	private static final String ANTILR_SPEED_BOOST = "LootingLimiterSpeed";
	private static final String ANTILR_COOLDOWN = "LootingLimiterCooldown";
	private static final int COOLDOWN = Constants.TICKS_PER_SECOND;
	private static final Set<EntityType> IGNORED_ENTITY_TYPES = Set.of(
		EntityType.BLAZE,
		EntityType.GHAST,
		EntityType.RABBIT,
		EntityType.SILVERFISH,
		EntityType.BEE,
		EntityType.ENDERMITE,
		EntityType.BREEZE,
		EntityType.ALLAY,
		EntityType.VEX,
		EntityType.BAT,
		EntityType.PHANTOM,
		EntityType.OCELOT
	);

	protected HashMap<UUID, Integer> mPlayerMobKills = new HashMap<>();
	protected HashMap<UUID, Integer> mPlayerSpawnerBreaks = new HashMap<>();

	// spawner break checks

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEventMonitor(BlockBreakEvent event) {
		Player player = event.getPlayer();
		if (ServerProperties.getLootingLimiterSpawners() <= 0) {
			return;
		}
		if (event.getBlock().getType() == Material.SPAWNER) {
			spawnerBroken(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEventMonitor(EntityExplodeEvent event) {
		if (ServerProperties.getLootingLimiterSpawners() <= 0) {
			return;
		}
		for (Block b : event.blockList()) {
			if (b.getType() == Material.SPAWNER) {
				Player player = EntityUtils.getNearestPlayer(b.getLocation(), PLAYER_SEARCH_RADIUS);
				if (player != null) {
					spawnerBroken(player);
				}
			}
		}
	}

	private void spawnerBroken(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		if (player.hasPermission(DEBUG_PERMISSION)) {
			player.sendMessage("LootingLimiter spawnerBreak");
			player.sendMessage("    beforeSpawnerScore="
				+ mPlayerSpawnerBreaks.getOrDefault(player.getUniqueId(), 0));
		}
		mPlayerSpawnerBreaks.put(player.getUniqueId(),
			Math.min(mPlayerSpawnerBreaks.getOrDefault(player.getUniqueId(), 0) + 1, ServerProperties.getLootingLimiterBankedChests() * ServerProperties.getLootingLimiterSpawners()));
		if (player.hasPermission(DEBUG_PERMISSION)) {
			player.sendMessage("    spawnerScore="
				+ mPlayerSpawnerBreaks.getOrDefault(player.getUniqueId(), 0));
			if (mPlayerSpawnerBreaks.getOrDefault(player.getUniqueId(), 0)
				== ServerProperties.getLootingLimiterBankedChests() * ServerProperties.getLootingLimiterSpawners()) {
				player.sendMessage("    player has hit limit for spawnerBreak");
			}
		}
	}

	// mob kills

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		if (ServerProperties.getLootingLimiterMobKills() <= 0) {
			return;
		}
		LivingEntity entity = event.getEntity();
		if (!EntityUtils.isHostileMob(entity)
			|| entity.getScoreboardTags().contains(EntityUtils.IGNORE_DEATH_TRIGGERS_TAG)) {
			return;
		}
		Player player = entity.getKiller();
		if (player == null || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		int score = EntityUtils.isBoss(entity) ? 5 : EntityUtils.isElite(entity) ? 3 : 1;
		if (player.hasPermission(DEBUG_PERMISSION)) {
			player.sendMessage("LootingLimiter mobKill");
			player.sendMessage("    beforeKillScore="
				+ mPlayerMobKills.getOrDefault(player.getUniqueId(), 0));
		}
		mPlayerMobKills.put(player.getUniqueId(),
			Math.min(mPlayerMobKills.getOrDefault(player.getUniqueId(), 0) + score, ServerProperties.getLootingLimiterBankedChests() * ServerProperties.getLootingLimiterMobKills()));
		if (player.hasPermission(DEBUG_PERMISSION)) {
			player.sendMessage("    scoreForKill=" + score);
			player.sendMessage("    killScore="
				+ mPlayerMobKills.getOrDefault(player.getUniqueId(), 0));
			if (mPlayerMobKills.getOrDefault(player.getUniqueId(), 0)
				== ServerProperties.getLootingLimiterBankedChests() * ServerProperties.getLootingLimiterMobKills()) {
				player.sendMessage("    player has hit limit for mobKills");
			}
		}
	}

	// Chest place event (ignore loot chests the player places, ie from boss loot)

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if ((ServerProperties.getLootingLimiterSpawners() <= 0 && ServerProperties.getLootingLimiterMobKills() <= 0)
			|| ServerProperties.getLootingLimiterIgnoreBreakingChests()) {
			return;
		}
		Player player = event.getPlayer();
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		Block block = event.getBlock();
		if (!ChestUtils.isChestWithLootTable(block)) {
			return;
		}

		ChestUtils.setNonLootLimitedChest(block, true);
	}

	// chest break checks

	// explode events are handled by ChestOverride, as that code is called before this one would be

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEventEarly(BlockBreakEvent event) {
		if ((ServerProperties.getLootingLimiterSpawners() <= 0 && ServerProperties.getLootingLimiterMobKills() <= 0)
			|| ServerProperties.getLootingLimiterIgnoreBreakingChests()) {
			return;
		}
		if (!checkChest(event.getBlock(), event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Event.Result.DENY
			|| event.getAction() != Action.RIGHT_CLICK_BLOCK
			|| (ServerProperties.getLootingLimiterSpawners() <= 0 && ServerProperties.getLootingLimiterMobKills() <= 0)) {
			return;
		}
		Block block = event.getClickedBlock();
		if (block != null && !checkChest(block, event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	public boolean checkChest(Block block, @Nullable Player player) {
		if (player != null && (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR)) {
			return true;
		}
		if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.LOOTING_LIMITER_DISABLED, "looting_limiter_disabled")) {
			return true;
		}
		if (ChestUtils.isNonLootLimitedChest(block)) {
			// The chest is about to have its loot table rolled; remove this marker
			ChestUtils.setNonLootLimitedChest(block, false);
			if (player != null && player.hasPermission(DEBUG_PERMISSION)) {
				player.sendMessage("LL Opened non-antiLR chest");
			}
			return true;
		}
		if (ChestUtils.isChestWithLootTable(block)) {
			Location blockCentreLoc = block.getLocation().toCenterLocation();

			List<Player> players = PlayerUtils.playersInRange(blockCentreLoc, PLAYER_SEARCH_RADIUS, true, true);
			if (players.isEmpty()) {
				return false;
			}
			players.sort(Comparator.comparing(p -> p.getLocation().distanceSquared(blockCentreLoc)));
			if (player != null) { // make sure the breaking/opening player is first in the list, even if another player is closer
				if (MetadataUtils.happenedInRecentTicks(player, ANTILR_COOLDOWN, COOLDOWN)) {
					player.sendActionBar(Component.text("The danger has not passed!", NamedTextColor.RED));
					return false;
				} else {
					players.remove(player);
					players.addFirst(player);
				}
			}
			Set<LivingEntity> affectedMobs = getNearbyMobsWithLoS(blockCentreLoc);
			affectedMobs.addAll(getNearbyMobsWithLoS(players.getFirst().getEyeLocation()));
			Set<Location> affectedSpawnerLocs = getNearbySpawnersWithLoS(blockCentreLoc);
			affectedSpawnerLocs.addAll(getNearbySpawnersWithLoS(players.getFirst().getEyeLocation()));

			int effectiveMobs = 0;
			for (LivingEntity affectedMob : affectedMobs) {
				effectiveMobs += EntityUtils.isBoss(affectedMob) ? 5 : (EntityUtils.isElite(affectedMob) ? 3 : 1);
			}

			boolean hasMobs = effectiveMobs >= (players.getFirst().getWorld().getName().contains("instance")
				? ServerProperties.getLootingLimiterMobCountThresholdStrike()
				: ServerProperties.getLootingLimiterMobCountThreshold()
			);
			boolean hasSpawners = affectedSpawnerLocs.size() >= (players.getFirst().getWorld().getName().contains("instance")
				? ServerProperties.getLootingLimiterSpawnerCountThresholdStrike()
				: ServerProperties.getLootingLimiterSpawnerCountThreshold()
			);

			int totalMobScore = players.stream().mapToInt(p -> mPlayerMobKills.getOrDefault(p.getUniqueId(), 0)).sum();
			int totalSpawnerScore = players.stream().mapToInt(p -> mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0)).sum();

			boolean blockedByMobs = hasMobs && totalMobScore < ServerProperties.getLootingLimiterMobKills();
			boolean blockedBySpawners = hasSpawners && totalSpawnerScore < ServerProperties.getLootingLimiterSpawners();
			if (player != null) {
				if (blockedByMobs && blockedBySpawners) {
					player.sendActionBar(Component.text("The sound alerts nearby enemies and accelerates nearby spawners!", NamedTextColor.RED));
					if (player.hasPermission(DEBUG_PERMISSION)) {
						player.sendMessage("LL blockedByKills: kills sum="
							+ players.stream().mapToInt(p -> mPlayerMobKills.getOrDefault(p.getUniqueId(), 0)).sum());
						player.sendMessage("    requiredKills=" + ServerProperties.getLootingLimiterMobKills());
						for (Player p : players) {
							player.sendMessage("    " + p.getName() + " kills="
								+ mPlayerMobKills.getOrDefault(p.getUniqueId(), 0));
						}
						player.sendMessage("LL: blockedBySpawners spawnerBreaks sum="
							+ players.stream().mapToInt(p -> mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0)).sum());
						player.sendMessage("    requiredSpawners=" + ServerProperties.getLootingLimiterSpawners());
						for (Player p : players) {
							player.sendMessage("    " + p.getName() + " spawners="
								+ mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0));
						}
					}
					if (totalMobScore == 0 || totalSpawnerScore == 0) {
						player.sendMessage(Component.text("Your resolve wavers in the face of the crowd...", NamedTextColor.GRAY));
						Plugin.getInstance().mEffectManager.addEffect(player, ANTILR_SPEED_BOOST,
							new PercentSpeed((totalMobScore == 0 && totalSpawnerScore == 0) ? 25 : 15, -0.6, ANTILR_SPEED_BOOST));
					}
				} else if (blockedByMobs) {
					player.sendActionBar(Component.text("The sound alerts nearby enemies! Fight them off!", NamedTextColor.RED));
					if (player.hasPermission(DEBUG_PERMISSION)) {
						player.sendMessage("LL blockedByKills: kills sum="
							+ players.stream().mapToInt(p -> mPlayerMobKills.getOrDefault(p.getUniqueId(), 0)).sum());
						player.sendMessage("    requiredKills=" + ServerProperties.getLootingLimiterMobKills());
						for (Player p : players) {
							player.sendMessage("    " + p.getName() + " kills="
								+ mPlayerMobKills.getOrDefault(p.getUniqueId(), 0));
						}
					}
					if (totalMobScore == 0) {
						// If you end up in this bracket, you attempted to open a chest, got blocked, was put on 1s antiLR CD,
						// and were told what to do to not get penalised. And you didn't kill a *single* mob.
						// Your kneecaps are mine, now.
						player.sendMessage(Component.text("Your resolve wavers in the face of the crowd...", NamedTextColor.GRAY));
						Plugin.getInstance().mEffectManager.addEffect(player, ANTILR_SPEED_BOOST,
							new PercentSpeed(15, -0.6, ANTILR_SPEED_BOOST));
					}
				} else if (blockedBySpawners) {
					player.sendActionBar(Component.text("It can't be safe to open this chest with so many spawners around...", NamedTextColor.RED));
					if (player.hasPermission(DEBUG_PERMISSION)) {
						player.sendMessage("LL: blockedBySpawners spawnerBreaks sum="
							+ players.stream().mapToInt(p -> mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0)).sum());
						player.sendMessage("    requiredSpawners=" + ServerProperties.getLootingLimiterSpawners());
						for (Player p : players) {
							player.sendMessage("    " + p.getName() + " spawners="
								+ mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0));
						}
					}
					if (totalMobScore == 0) {
						player.sendMessage(Component.text("You feel a shiver run down your spine...", NamedTextColor.GRAY));
						Plugin.getInstance().mEffectManager.addEffect(player, ANTILR_SPEED_BOOST,
							new PercentSpeed(15, -0.6, ANTILR_SPEED_BOOST));
					}
				}
			}

			// Penalise players
			if (hasMobs) {
				int remaining = ServerProperties.getLootingLimiterMobKills();
				for (Player p : players) {
					int playerScore = mPlayerMobKills.getOrDefault(p.getUniqueId(), 0);
					if (playerScore >= remaining) {
						mPlayerMobKills.put(p.getUniqueId(), playerScore - remaining);
						break;
					} else if (playerScore > 0) {
						remaining -= playerScore;
						mPlayerMobKills.put(p.getUniqueId(), 0);
					}
				}
			}
			if (hasSpawners) {
				int remaining = ServerProperties.getLootingLimiterSpawners();
				for (Player p : players) {
					int playerScore = mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0);
					if (playerScore >= remaining) {
						mPlayerSpawnerBreaks.put(p.getUniqueId(), playerScore - remaining);
						break;
					} else if (playerScore > 0) {
						remaining -= playerScore;
						mPlayerSpawnerBreaks.put(p.getUniqueId(), 0);
					}
				}
			}
			if (player != null && player.hasPermission(DEBUG_PERMISSION)) {
				player.sendMessage("LL: chestOpened");
				for (Player p : players) {
					player.sendMessage("    " + p.getName() + " spawners="
						+ mPlayerSpawnerBreaks.getOrDefault(p.getUniqueId(), 0));
					player.sendMessage("    " + p.getName() + " kills="
						+ mPlayerMobKills.getOrDefault(p.getUniqueId(), 0));
				}
			}

			if (blockedByMobs || blockedBySpawners) {
				if (player != null) {
					// Effect: Put the player on antiLR cooldown
					MetadataUtils.markThisTick(Plugin.getInstance(), player, ANTILR_COOLDOWN);

					// SFX: Chest cracking noise
					block.getWorld().playSound(blockCentreLoc, Sound.BLOCK_IRON_DOOR_CLOSE, SoundCategory.HOSTILE, 0.7f, 1.4f);
					block.getWorld().playSound(blockCentreLoc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.HOSTILE, 2, 1);
					block.getWorld().playSound(blockCentreLoc, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 1.8f, 1.5f);
					block.getWorld().playSound(blockCentreLoc, Sound.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.HOSTILE, 2, 0.5f);

					// VFX: Flash
					new PartialParticle(Particle.FLASH, blockCentreLoc, 1)
						.distanceFalloff(8)
						.spawnAsEnemy();
					if (hasMobs) {
						for (LivingEntity mob : affectedMobs) {
							// Effect: Enemies target the player.
							Entity currentTarget = mob.getTargetEntity(16);
							if (!(currentTarget instanceof Player)) {
								EntityUtils.applyTaunt(mob, players.getFirst());
							}

							// VFX: Lightning to enemies
							new PPLine(Particle.SPELL_WITCH,
								blockCentreLoc,
								LocationUtils.getHalfHeightLocation(mob))
								.countPerMeter(8)
								.delay(6)
								.distanceFalloff(16)
								.spawnAsEnemy();
						}
						// Effect (delayed): Enemies glow, speed up, and break free of CC.
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
							for (LivingEntity mob : affectedMobs) {
								if (!mob.isDead() && mob.isValid()) {
									GlowingManager.startGlowing(mob, NamedTextColor.WHITE, 2 * Constants.TICKS_PER_SECOND, 97);
									Plugin.getInstance().mEffectManager.addEffect(mob, ANTILR_SPEED_BOOST, new PercentSpeed(2 * Constants.TICKS_PER_SECOND, 0.4, ANTILR_SPEED_BOOST));
									EntityUtils.removeFreeze(Plugin.getInstance(), mob);
									EntityUtils.removeSilence(mob);
									EntityUtils.removeStagger(mob);
									EntityUtils.removeStun(mob);
									EntityUtils.removeParalysis(Plugin.getInstance(), mob);
									EntityUtils.setSlowTicks(Plugin.getInstance(), mob, 0);
									EntityUtils.setWeakenTicks(Plugin.getInstance(), mob, 0);
								}
							}
						}, 5);
					}
					if (hasSpawners) {
						for (Location spawnerLoc : affectedSpawnerLocs) {
							// Effect: Slightly accelerate the spawners
							if (spawnerLoc.getBlock().getBlockData() instanceof CreatureSpawner spawner) {
								// Should always be true
								spawner.setDelay(Math.max(1, spawner.getDelay() - 3 * Constants.TICKS_PER_SECOND));
								spawner.update(false, false);
							}

							// VFX: Lightning to spawners
							new PPLine(Particle.TRIAL_SPAWNER_DETECTION,
								blockCentreLoc,
								spawnerLoc.toCenterLocation())
								.countPerMeter(6)
								.delay(8)
								.distanceFalloff(16)
								.spawnAsEnemy();
						}
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
							for (Location spawnerLoc : affectedSpawnerLocs) {
								if (spawnerLoc.getBlock().getType() == Material.SPAWNER) {
									new PartialParticle(Particle.TRIAL_SPAWNER_DETECTION,
										spawnerLoc.toCenterLocation())
										.delta(0.5)
										.count(25)
										.distanceFalloff(16)
										.spawnAsEnemy();
								}
							}
						}, 7);
					}
				}
				return false;
			}
		}
		return true;
	}

	// world join event (controls players' scores)

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		playerJoin(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerChangedWorldEvent(PlayerChangedWorldEvent event) {
		playerJoin(event.getPlayer());
	}

	private void playerJoin(Player player) {
		setBankedChests(player, player.getWorld().getName().contains("instance") ? 2 : PlayerUtils.playerIsInPOI(player) ? 0 : 1);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mPlayerMobKills.remove(event.getPlayer().getUniqueId());
		mPlayerSpawnerBreaks.remove(event.getPlayer().getUniqueId());
	}

	// Access / Manipulation methods

	public void setBankedChests(Player player, int bankedChests) {
		bankedChests = Math.clamp(0, bankedChests, ServerProperties.getLootingLimiterBankedChests());
		mPlayerMobKills.put(player.getUniqueId(), bankedChests * ServerProperties.getLootingLimiterMobKills());
		mPlayerSpawnerBreaks.put(player.getUniqueId(), bankedChests * ServerProperties.getLootingLimiterSpawners());
	}

	public int getMobsKilled(@NotNull Player player) {
		return mPlayerMobKills.getOrDefault(player.getUniqueId(), 0);
	}

	public int getSpawnersBroken(@NotNull Player player) {
		return mPlayerSpawnerBreaks.getOrDefault(player.getUniqueId(), 0);
	}

	// Helper methods

	// Ignores invulnerable enemies, worm segments, etc.
	private static Set<LivingEntity> getNearbyMobsWithLoS(Location loc) {
		Set<LivingEntity> mobs = new HashSet<>(EntityUtils.getNearbyMobs(loc, MOB_AND_SPAWNER_SEARCH_RADIUS));
		mobs.removeIf(mob -> {
			if (!EntityUtils.isHostileMob(mob)
				|| EntityUtils.isBoss(mob)
				|| DamageUtils.isImmuneToDamage(mob, null)
				|| IGNORED_ENTITY_TYPES.contains(mob.getType())
				|| mob.getScoreboardTags().contains(WormBoss.IGNORE_WORM_TAG)
				|| mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
				return true;
			}

			double range = loc.distance(LocationUtils.getHalfHeightLocation(mob));
			Vector direction = LocationUtils.getDirectionTo(LocationUtils.getHalfHeightLocation(mob), loc);
			if (!Double.isFinite(direction.getX())) {
				return true;
			}

			double score;
			try {
				score = score(loc, direction, range);
			} catch (IllegalStateException e) {
				// Thrown sometimes when chunks aren't loaded at exactly the right time
				return true;
			}

			if (EntityUtils.isElite(mob)) {
				score /= 1.6d;
			}
			if (mob.getScoreboardTags().contains(BlockBreakBoss.identityTag)) {
				score /= 2d;
			} else if (mob.getScoreboardTags().contains(BlockPlacerBoss.identityTag)) {
				score /= 4d;
			}

			for (Player player : PlayerUtils.playersInRange(loc, 5, true)) {
				if (player.hasPermission(DEBUG_PERMISSION)) {
					player.sendMessage("LL: Score of enemy " + mob.getName() + " is: " + score);
				}
			}

			return score > (loc.getWorld().getName().contains("instance")
				? ServerProperties.getLootingLimiterModifiedLoSScoreMobsStrike()
				: ServerProperties.getLootingLimiterModifiedLoSScoreMobs()
			);
		});
		return mobs;
	}

	private static Set<Location> getNearbySpawnersWithLoS(Location loc) {
		Set<BlockState> spawners = new HashSet<>(EntityUtils.getTileEntitiesInRange(loc, MOB_AND_SPAWNER_SEARCH_RADIUS, b -> b.getType() == Material.SPAWNER
			&& b.getLocation().add(0, -1, 0).getBlock().getType() != Material.BEDROCK));
		spawners.removeIf(spawner -> {
			Location spawnerLoc = spawner.getLocation();
			if (!UnbreakableOnBedrockOverride.breakable(spawnerLoc.getBlock())) {
				// Don't count unbreakable spawners
				return true;
			}
			if (spawner instanceof CreatureSpawner creatureSpawner
				&& creatureSpawner.getDelay() > 0.9 * creatureSpawner.getMinSpawnDelay()) {
				// Don't count spawners that just spawned
				return true;
			}
			if (SpawnerUtils.isSpecialSpawner(spawnerLoc.getBlock())) {
				return true;
			}
			double range = loc.distance(spawnerLoc);
			Vector direction = LocationUtils.getDirectionTo(spawnerLoc, loc);
			if (!Double.isFinite(direction.getX())) {
				return true;
			}

			double score;
			try {
				score = score(loc, direction, range);
			} catch (IllegalStateException e) {
				// Thrown sometimes when chunks aren't loaded at exactly the right time
				return true;
			}

			// Don't count a spawner if it's completely surrounded in solid blocks or stairs
			if ((BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.NORTH)) || loc.getBlock().getRelative(BlockFace.NORTH).getBlockData() instanceof Stairs)
				&& (BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.EAST)) || loc.getBlock().getRelative(BlockFace.EAST).getBlockData() instanceof Stairs)
				&& (BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.SOUTH)) || loc.getBlock().getRelative(BlockFace.SOUTH).getBlockData() instanceof Stairs)
				&& (BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.WEST)) || loc.getBlock().getRelative(BlockFace.WEST).getBlockData() instanceof Stairs)
				&& (BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.UP)) || loc.getBlock().getRelative(BlockFace.UP).getBlockData() instanceof Stairs)
				&& (BlockUtils.isFullBlock(loc.getBlock().getRelative(BlockFace.DOWN)) || loc.getBlock().getRelative(BlockFace.DOWN).getBlockData() instanceof Stairs)) {
				return true;
			}

			for (Player player : PlayerUtils.playersInRange(loc, 5, true)) {
				if (player.hasPermission(DEBUG_PERMISSION)) {
					player.sendMessage("LL: Score of spawner at " + spawnerLoc + " is: " + score);
				}
			}
			return score > (loc.getWorld().getName().contains("instance")
				? ServerProperties.getLootingLimiterModifiedLoSScoreSpawnersStrike()
				: ServerProperties.getLootingLimiterModifiedLoSScoreSpawners()
			);
		});

		HashSet<Location> locations = new HashSet<>();
		spawners.forEach(blockState -> locations.add(blockState.getLocation()));
		return locations;
	}

	private static double score(Location start, Vector direction, double range) {
		double score = 0;
		List<Pair<Location, Double>> modifiedLoS = lineOfSight(start, direction, range);
		double coefficient = 0.3;

		for (Pair<Location, Double> pair : modifiedLoS) {
			Block b = pair.first().getBlock();

			// If block was player-placed, ignore it; prevents cheesing antiLR with walls
			if (Plugin.getInstance().mPlacedBlocksListener.isPlayerPlaced(b.getLocation())) {
				continue;
			}

			// If block is unbreakable, immediately reject
			if (ServerProperties.getUnbreakableBlocks().contains(b.getType()) || ItemUtils.noPassthrough.contains(b.getType())) {
				return Double.MAX_VALUE;
			}

			// Rearrange in order of most common / most performant
			if (BlockUtils.LIQUIDS.contains(b.getType()) || b.isEmpty()) {
				coefficient = 0;
			} else if (b.isPassable() || b.getType() == Material.MOVING_PISTON) {
				coefficient = 0.01;
			} else if (b.getType().getHardness() < 0) {
				// After Passable, because Light / Nether Portal / End Portal is considered -1 Hardness
				return Double.MAX_VALUE;
			} else if (ItemUtils.CARPETS.contains(b.getType()) || b.getBlockData() instanceof TrapDoor) {
				coefficient = 0.02;
			} else if (b.getBlockData() instanceof Door) {
				coefficient = 0.1;
			} else if (ItemUtils.GOOD_OCCLUDERS.contains(b.getType())) {
				coefficient = 0.4;
			} else if (b instanceof Slab slab && slab.getType() != Slab.Type.DOUBLE) {
				coefficient = 0.5;
			} else if (ItemUtils.HEADS.contains(b.getType())) {
				coefficient = 0.6;
			} else if (b.getBlockData() instanceof Stairs) {
				coefficient = 0.75;
			} else if (b.getType().getHardness() > 20) {
				// After Liquids, because those have 100 hardness
				// Ender Chests, Vaults, Trial Spawners, Respawn Anchors, all Obsidians, Netherite, Reinforced Deepslate
				coefficient = 3;
			} else if (BlockUtils.isFullBlock(b)) {
				coefficient = 1;
			}

			// Weight roughly by the location of the occluding blocks.
			// Blocks near the start or end position usually are environmental / spawner cages;
			// a wall in the middle of the LoS check is usually a real wall.
			// Distance check is approximate... it's fine. Arctangent is an arbitrary choice for a function.
			coefficient *= 1 + Math.atan(Math.pow(Math.abs(b.getLocation().toCenterLocation().distance(start) - range / 2) - 1, 3) / 8d);

			// Multiply by the distance travelled in the occluding block
			coefficient *= pair.second();
			score += coefficient;
		}
		return score;
	}

	public static List<Pair<Location, Double>> lineOfSight(Location start, Vector direction, double maxDistance) {
		ArrayList<Pair<Location, Double>> result = new ArrayList<>((int) maxDistance);
		if (direction.lengthSquared() == 0) {
			return result;
		}
		direction.normalize();

		if (Math.abs(direction.getX()) < 1 / (2 * maxDistance)) {
			direction.setX(1 / (2 * maxDistance));
		}
		if (Math.abs(direction.getY()) < 1 / (2 * maxDistance)) {
			direction.setY(1 / (2 * maxDistance));
		}
		if (Math.abs(direction.getZ()) < 1 / (2 * maxDistance)) {
			direction.setZ(1 / (2 * maxDistance));
		}

		double distance;
		double total = 0;

		int x = 0;
		int y = 0;
		int z = 0;

		double toXWall;
		double toYWall;
		double toZWall;
		double minWallDistance;
		while (total < maxDistance) {
			toXWall = Math.abs((x + start.getX() % 1) / direction.getX());
			toYWall = Math.abs((y + start.getY() % 1) / direction.getY());
			toZWall = Math.abs((z + start.getZ() % 1) / direction.getZ());

			minWallDistance = Math.min(Math.min(toXWall, toYWall), toZWall);
			if (minWallDistance == toXWall) {
				distance = Math.min(toXWall, maxDistance) - total;
				total += distance;

				result.add(Pair.of(start.clone().add(x, y, z), distance));

				x += (direction.getX() > 0) ? 1 : -1;
			} else if (minWallDistance == toYWall) {
				distance = Math.min(toYWall, maxDistance) - total;
				total += distance;

				result.add(Pair.of(start.clone().add(x, y, z), distance));

				y += (direction.getY() > 0) ? 1 : -1;
			} else if (minWallDistance == toZWall) {
				distance = Math.min(toZWall, maxDistance) - total;
				total += distance;

				result.add(Pair.of(start.clone().add(x, y, z), distance));

				z += (direction.getZ() > 0) ? 1 : -1;
			} else {
				// Should never happen
				MMLog.severe("Custom LoS implementation broke, see LootingLimiter.java!");
				return result;
			}
		}

		return result;
	}
}
