package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.structures.events.StructureRespawnEvent;
import com.playmonumenta.structures.managers.RespawningStructure;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class PlacedBlocksListener implements Listener {

	protected HashMap<Chunk, HashSet<Location>> mPlayerPlacedBlocks = new HashMap<>();

	// Access methods

	public int loadedChunks() {
		return mPlayerPlacedBlocks.size();
	}

	@NotNull
	public Set<Location> getPlayerPlacedBlocks(Chunk chunk) {
		if (mPlayerPlacedBlocks.containsKey(chunk)) {
			return mPlayerPlacedBlocks.get(chunk);
		} else {
			return new HashSet<>();
		}
	}

	// Commands

	public void indicatePlayerPlacedBlocks(Chunk chunk, Player player) {
		Set<Location> locations = getPlayerPlacedBlocks(chunk);
		for (Location loc : locations) {
			new PPRectPrism(Particle.REDSTONE, loc.clone(), loc.clone().add(1, 1, 1))
				.countPerMeter(10).edgeMode(true).gradientColor(Color.fromRGB(247, 188, 37), Color.fromRGB(235, 69, 28), 0.75f)
				.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1f)).spawnForPlayers(ParticleCategory.FULL, player);
		}
	}

	public void clearShard() {
		mPlayerPlacedBlocks.clear();
	}

	public void clearLoadedChunks(World world) {
		for (Chunk chunk : world.getLoadedChunks()) {
			clearChunk(chunk);
		}
	}

	public void clearChunk(Chunk chunk) {
		mPlayerPlacedBlocks.remove(chunk);
	}

	private void clearLocations(Collection<Location> locations) {
		// Set of all affected chunks, in case it occurred at a chunk boundary
		HashSet<Chunk> chunkSet = new HashSet<>();
		for (Location location : locations) {
			chunkSet.add(location.getChunk());
		}

		// Sort by chunks so that it can all be handled at once
		for (Chunk chunk : chunkSet) {
			// Is it more performant to sort the location list to make sure the chunk matches first,
			// or should I just let .removeAll() handle it?
			getPlayerPlacedBlocks(chunk).removeAll(locations);
		}
	}

	// Functionality

	// Block addition

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEventMonitor(BlockPlaceEvent event) {
		placeBlock(event.getPlayer(), event.getBlock());
	}

	public void placeBlock(Player player, Block block) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}
		Chunk chunk = block.getChunk();
		if (!shouldRecordChunkData()) {
			return;
		}
		Location blockLoc = block.getLocation();
		if (!shouldRecordBlock(blockLoc)) {
			return;
		}

		if (mPlayerPlacedBlocks.containsKey(chunk)) {
			getPlayerPlacedBlocks(chunk).add(blockLoc);
		} else {
			HashSet<Location> hashSet = new HashSet<>(8);
			hashSet.add(blockLoc);
			mPlayerPlacedBlocks.put(chunk, hashSet);
		}

		if (getPlayerPlacedBlocks(chunk).size() > 100) {
			// They're probably building something here, wipe half the blocks randomly.
			// Maintains performance so we don't hold on to a lot of useless entries.
			// It's not like the players will notice...
			clearHalf(chunk);
		}
	}

	// Block removals

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEventMonitor(BlockBreakEvent event) {
		Block block = event.getBlock();
		Chunk chunk = block.getChunk();

		getPlayerPlacedBlocks(chunk).remove(block.getLocation());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void structureRespawnEvent(StructureRespawnEvent event) {
		HashSet<Location> locations = new HashSet<>();
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RespawningStructure structure = event.getStructure();
			RespawningStructure.StructureBounds bounds = structure.getOuterBounds();
			Vector corner1 = bounds.mLowerCorner;
			Vector corner2 = bounds.mUpperCorner;
			CuboidRegion cuboidRegion = new CuboidRegion(
				BlockVector3.at(corner1.getBlockX(), corner1.getBlockY(), corner1.getBlockZ()),
				BlockVector3.at(corner2.getBlockX(), corner2.getBlockY(), corner2.getBlockZ())
			);
			cuboidRegion.iterator().forEachRemaining(blockVector3 -> locations.add(BukkitAdapter.adapt(structure.getWorld(), blockVector3)));
		});

		clearLocations(locations);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		clearBlocks(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		clearBlocks(event.blockList());
	}

	private void clearBlocks(Collection<Block> blocks) {
		HashSet<Location> locations = new HashSet<>();
		blocks.forEach(block -> locations.add(block.getLocation()));
		clearLocations(locations);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		mPlayerPlacedBlocks.remove(event.getChunk());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		clearLoadedChunks(event.getWorld());
	}

	// Feel free to modify this, if you use this class for anything else. At the moment, the listener doesn't record anything if antiLR won't use the information.
	private boolean shouldRecordChunkData() {
		return ServerProperties.getLootingLimiterMobKills() != 0 || ServerProperties.getLootingLimiterSpawners() != 0;
	}

	private boolean shouldRecordBlock(Location location) {
		String shardName = ServerProperties.getShardName();
		String worldName = location.getWorld().getName();
		if (shardName.contains("valley") || shardName.contains("isles") || shardName.contains("ring")) {
			// Overworld shards, only log blocks placed within PoIs and coalrupted
			if (LocationUtils.isInPoI(location)) {
				return true;
			} else if (worldName.contains("instance")) {
				return ZoneUtils.hasZoneProperty(location, ZoneUtils.ZoneProperty.COALRUPTED_SNOW_PERKS);
			}
		}
		return true;
	}

	// Randomly remove half of the locations, for... performance, of course!
	private void clearHalf(Chunk chunk) {
		ArrayList<Location> list = new ArrayList<>(mPlayerPlacedBlocks.get(chunk));
		Collections.shuffle(list);
		list.subList(list.size() / 4, 3 * list.size() / 4).clear();
		list.forEach(mPlayerPlacedBlocks.get(chunk)::remove);
	}
}
