package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPRectPrism;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.structures.events.StructureRespawnEvent;
import com.playmonumenta.structures.managers.RespawningStructure;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlacedBlocksListener implements Listener {

	protected ConcurrentHashMap<Subchunk, BitSet> mPlayerPlacedBlocks = new ConcurrentHashMap<>();

	// Access methods

	public int loadedSubchunks() {
		return mPlayerPlacedBlocks.size();
	}

	@NotNull
	public Collection<Vector> getPlayerPlacedBlocks(Chunk chunk) {
		Collection<Vector> result = new ArrayList<>();

		Iterator<Subchunk> smallChunks = mPlayerPlacedBlocks.keySet().stream().iterator();
		smallChunks.forEachRemaining(subchunk -> {
			if (subchunk.withinChunk(chunk)) {
				BitSet bitSet = mPlayerPlacedBlocks.get(subchunk);
				if (bitSet != null) {
					for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
						// operate on index i here
						result.add(unmaskVec(subchunk, i));
					}
				}
			}
		});

		return result;
	}

	public boolean isPlayerPlaced(Location location) {
		@Nullable BitSet bs = mPlayerPlacedBlocks.get(new Subchunk(location));
		if (bs == null) {
			return false;
		}
		return bs.get(bitMask(location));
	}

	// Commands

	public void indicatePlayerPlacedBlocks(Chunk chunk, Player player) {
		Iterator<Subchunk> smallChunks = mPlayerPlacedBlocks.keySet().stream().iterator();
		smallChunks.forEachRemaining(subchunk -> {
			if (subchunk.withinChunk(chunk)) {
				BitSet bitSet = mPlayerPlacedBlocks.get(subchunk);
				if (bitSet != null) {
					for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
						// operate on index i here
						Location loc = unmaskLoc(subchunk, i);
						new PPRectPrism(Particle.REDSTONE, loc.clone(), loc.clone().add(1, 1, 1))
							.countPerMeter(10).edgeMode(true).gradientColor(Color.fromRGB(247, 188, 37), Color.fromRGB(235, 69, 28), 1.5f)
							.data(new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f)).spawnForPlayers(ParticleCategory.FULL, player);
					}
				}
			}
		});
	}

	public void clearShard() {
		mPlayerPlacedBlocks.clear();
	}

	public void clearLoadedChunks(World world) {
		mPlayerPlacedBlocks.keySet().removeIf(subchunk -> subchunk.getWorld().equals(world));
	}

	public void clearChunk(Chunk chunk) {
		Iterator<Subchunk> smallChunks = mPlayerPlacedBlocks.keySet().stream().iterator();
		smallChunks.forEachRemaining(subchunk -> {
			if (subchunk.withinChunk(chunk)) {
				clearSubchunk(subchunk);
			}
		});
	}

	public void clearSubchunk(Subchunk subchunk) {
		mPlayerPlacedBlocks.remove(subchunk);
	}

	private void clearLocations(World world, RespawningStructure.StructureBounds bounds) {
		Vector lowerCorner = bounds.mLowerCorner;
		Vector upperCorner = bounds.mUpperCorner;

		// These are chunk coordinates! Multiply by 16 to restore normal coordinates.
		int lowerX = lowerCorner.getBlockX() >> 4;
		int lowerY = lowerCorner.getBlockY() >> 4;
		int lowerZ = lowerCorner.getBlockZ() >> 4;
		int upperX = upperCorner.getBlockX() >> 4;
		int upperY = upperCorner.getBlockY() >> 4;
		int upperZ = upperCorner.getBlockZ() >> 4;

		mPlayerPlacedBlocks.keySet().forEach(subchunk -> {
			if (!subchunk.getWorld().equals(world)) {
				return;
			}
			int x = subchunk.getX();
			int y = subchunk.getY();
			int z = subchunk.getZ();
			if (x < lowerX || x > upperX) {
				return;
			}
			if (y < lowerY || y > upperY) {
				return;
			}
			if (z < lowerZ || z > upperZ) {
				return;
			}

			if (lowerX + 1 <= x && upperX - 1 >= x
				&& lowerY + 1 <= y && upperY - 1 >= y
				&& lowerZ + 1 <= z && upperZ - 1 >= z) {
				// subchunk is fully enclosed within the PoI bounds
				clearSubchunk(subchunk);
				return;
			}

			BitSet bs = mPlayerPlacedBlocks.get(subchunk);
			if (bs != null) {
				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
					// operate on index i here
					if (bounds.within(unmaskVec(subchunk, i))) {
						bs.clear(i);
					}
				}
			}

			if (mPlayerPlacedBlocks.get(subchunk) == null || mPlayerPlacedBlocks.get(subchunk).isEmpty()) {
				clearSubchunk(subchunk);
			}
		});
	}

	// The cost of this function is linear WRT explosion size and linear WRT tracked subchunks.
	// For now, this isn't an issue, because the largest explosion in a relevant area is the bridge bomb in Tolumaeus.
	// However, if we roll this out to dungeons, we might need to re-evaluate this method for EX White / EX Light Blue's Roomfucker 9000 traps.
	private void clearExplosionBlocks(Collection<Block> blocks) {
		blocks.removeIf(Block::isEmpty);
		if (blocks.isEmpty()) {
			return;
		}
		World world = blocks.iterator().next().getWorld(); // Explosions don't cross worlds

		HashMap<Long, BitSet> buckets = new HashMap<>();
		for (Block block : blocks) {
			Location loc = block.getLocation();
			long subchunkKey = getChunkSectionKey(block.getX(), block.getY(), block.getZ());
			buckets.computeIfAbsent(subchunkKey, unused -> new BitSet(Subchunk.SIZE));
			buckets.get(subchunkKey).set(bitMask(loc));
		}

		for (long key : buckets.keySet()) {
			Subchunk subchunk = new Subchunk(
				getChunkSectionX(key),
				getChunkSectionY(key),
				getChunkSectionZ(key),
				world);
			@Nullable BitSet bs = mPlayerPlacedBlocks.get(subchunk);
			if (bs == null) {
				break;
			}
			bs.andNot(buckets.get(key));
			if (bs.isEmpty()) {
				clearSubchunk(subchunk);
			}
		}
	}

	// Functionality

	// Conversion to BitSet's mappings

	public static int bitMask(Location loc) {
		// [number] & 0xF is like [number] mod 16, but it's also favourable
		return bitMask(loc.getBlockX() & 0xF, loc.getBlockY() & 0xF, loc.getBlockZ() & 0xF);
	}

	public static int bitMask(int x, int y, int z) {
		assert x >= 0;
		assert y >= 0;
		assert z >= 0;
		assert x < 16;
		assert y < 16;
		assert z < 16;
		return (x << 8) | (y << 4) | z; // Should not exceed 4095
	}

	public static Vector fromCorner(int index) {
		assert index < 4096;
		// [number] & 0xF is like [number] mod 16
		return new Vector(index >> 8, (index >> 4) & 0xF, index & 0xF);
	}

	public static Vector unmaskVec(Subchunk subchunk, int index) {
		return new Vector(subchunk.getX() << 4, subchunk.getY() << 4, subchunk.getZ() << 4)
			.add(fromCorner(index));
	}

	public static Location unmaskLoc(Subchunk subchunk, int index) {
		return unmaskVec(subchunk, index).toLocation(subchunk.getWorld());
	}

	// Block addition

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEventMonitor(BlockPlaceEvent event) {
		placeBlock(event.getPlayer(), event.getBlock());
	}

	public void placeBlock(Player player, Block block) {
		if (player.getGameMode().equals(GameMode.CREATIVE) || !shouldRecordChunkData()) {
			return;
		}

		Location blockLoc = block.getLocation();
		if (!shouldRecordBlock(blockLoc)) {
			return;
		}

		Subchunk subchunk = new Subchunk(blockLoc);
		mPlayerPlacedBlocks.computeIfAbsent(subchunk, unused -> new BitSet(Subchunk.SIZE));
		mPlayerPlacedBlocks.get(subchunk).set(bitMask(blockLoc));

		if (mPlayerPlacedBlocks.size() > 147) {
			mPlayerPlacedBlocks.clear(); // Don't exceed ~100 MB
		}
	}

	// Block removals

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEventMonitor(BlockBreakEvent event) {
		Location blockLoc = event.getBlock().getLocation();
		clearLocation(blockLoc);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void tntPrimeEventMonitor(TNTPrimeEvent event) {
		Location blockLoc = event.getBlock().getLocation();
		clearLocation(blockLoc);
	}

	private void clearLocation(Location loc) {
		Subchunk subchunk = new Subchunk(loc);

		BitSet placedMap = mPlayerPlacedBlocks.get(subchunk);
		if (placedMap != null) {
			placedMap.clear(bitMask(loc));
			if (placedMap.isEmpty()) {
				mPlayerPlacedBlocks.remove(subchunk);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void structureRespawnEvent(StructureRespawnEvent event) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			RespawningStructure structure = event.getStructure();
			RespawningStructure.StructureBounds bounds = structure.getOuterBounds();

			clearLocations(structure.getWorld(), bounds);
		});

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		clearExplosionBlocks(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		clearExplosionBlocks(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		mPlayerPlacedBlocks.keySet().removeIf(subchunk -> subchunk.withinChunk(event.getChunk()));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		clearLoadedChunks(event.getWorld());
	}

	// Feel free to modify this if you use this class for anything else. At the moment, the listener doesn't record anything if antiLR won't use the information.
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

	public record Subchunk(int x, int y, int z, World world) {
		public static int SIZE = 16 * 16 * 16;

		public Subchunk(Location loc) {
			this(loc.getBlockX() >> 4,
				loc.getBlockY() >> 4,
				loc.getBlockZ() >> 4,
				loc.getWorld());
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		public int getZ() {
			return z;
		}

		public World getWorld() {
			return world;
		}

		public boolean withinChunk(Chunk chunk) {
			return chunk.getX() == x && chunk.getZ() == z && chunk.getWorld().equals(world);
		}

		@SuppressWarnings("unused")
		public boolean isWithin(Location loc) {
			return loc.getBlockX() >> 4 == x
				&& loc.getBlockY() >> 4 == y
				&& loc.getBlockZ() >> 4 == z
				&& loc.getWorld().equals(world);
		}
	}

	// Temporarily stuff these here until I figure out how to make CoordinateUtils.class compile
	private static long getChunkSectionKey(int x, int y, int z) {
		return ((long) x & 4194303L) << 42 | ((long) y & 1048575L) | ((long) z & 4194303L) << 20;
	}

	private static int getChunkSectionX(long key) {
		return (int) (key >> 42);
	}

	private static int getChunkSectionY(long key) {
		return (int) (key << 44 >> 44);
	}

	private static int getChunkSectionZ(long key) {
		return (int) (key << 22 >> 42);
	}
}
