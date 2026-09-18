package com.playmonumenta.plugins.protocollib;

import com.bergerkiller.bukkit.common.entity.CommonEntityType;
import com.bergerkiller.bukkit.common.wrappers.ChatText;
import com.bergerkiller.bukkit.common.wrappers.DataWatcher;
import com.bergerkiller.generated.com.mojang.authlib.GameProfileHandle;
import com.bergerkiller.generated.com.mojang.authlib.properties.PropertyHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.PacketHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle.EnumPlayerInfoActionHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacketHandle.PlayerInfoDataHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityDestroyHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutEntityMetadataHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeamHandle;
import com.bergerkiller.generated.net.minecraft.network.protocol.game.PacketPlayOutSpawnEntityHandle;
import com.bergerkiller.generated.net.minecraft.world.entity.DisplayHandle;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.bosses.FakePlayerBoss;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.managers.PlayerSkinManager;
import com.playmonumenta.plugins.managers.PlayerSkinManager.SkinData;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/*
 * Why aren't we using a better plugin like Citizens or something?
 * Because this works and is funny :suffer:
 * - U5B_
 */
public class CursedListener extends PacketAdapter {
	public CursedListener(Plugin plugin) {
		super(plugin, ListenerPriority.LOW,
			PacketType.Play.Server.SPAWN_ENTITY,
			PacketType.Play.Server.ENTITY_DESTROY,
			PacketType.Play.Server.ENTITY_METADATA,
			PacketType.Play.Server.ENTITY_EQUIPMENT,
			PacketType.Play.Server.ENTITY_HEAD_ROTATION,
			PacketType.Play.Server.SCOREBOARD_TEAM
		);
	}

	// Humanoid-looking mobs
	// Similar hitboxes to Players, with the exception of Giants (Eldrask)
	public static final Set<EntityType> WHITELISTED_ENTITIES = EnumSet.of(
		EntityType.VILLAGER,
		EntityType.WANDERING_TRADER,
		EntityType.ZOMBIE_VILLAGER
	);
	// TODO: make npc features
	public static final boolean NPCS_ENABLED = false;

	private record EntityData(UUID entityUuid, String entityName, SkinData entitySkin,
	                          FakePlayerBoss.Parameters params, Set<UUID> visibleToPlayers,
	                          @Nullable NametagData nametag) {

		public static EntityData of(UUID entityUuid, int entityId, SkinData entitySkin, FakePlayerBoss.Parameters params, @Nullable NametagData nametag) {
			return new EntityData(
				entityUuid,
				FakePlayerBoss.convertIdToFakeName(entityId),
				entitySkin,
				params,
				new HashSet<>(),
				nametag
			);
		}
	}

	private record NametagData(int entityId, UUID entityUuid, DataWatcher dataWatcher, Vector worldPos) {
		public static NametagData of(TextDisplay nameTag, Location loc) {
			return new NametagData(
				nameTag.getEntityId(),
				nameTag.getUniqueId(),
				DisplayHandle.TextDisplayHandle.fromBukkit(nameTag).getDataWatcher(),
				loc.toVector()
			);
		}
	}

	private static class PlayerData {
		private static final ConcurrentHashMap<Integer, EntityData> entityDataMap = new ConcurrentHashMap<>();

		public static void addEntityToPlayer(UUID playerUuid, int entityId) {
			entityDataMap.computeIfPresent(entityId, (key, value) -> {
				value.visibleToPlayers.add(playerUuid);
				return value;
			});
		}

		public static boolean removeEntityFromPlayer(UUID playerUuid, int entityId) {
			@Nullable
			EntityData entityData = entityDataMap.get(entityId);
			if (entityData == null) {
				return false;
			}
			return entityData.visibleToPlayers.remove(playerUuid);
		}

		public static void removeEntity(int entityId) {
			entityDataMap.remove(entityId);
		}

		public static boolean containsEntityForPlayer(UUID playerUuid, int entityId) {
			EntityData data = entityDataMap.get(entityId);
			return data != null && data.visibleToPlayers.contains(playerUuid);
		}

		public static @Nullable EntityData getEntityDataByUUID(UUID entityUuid) {
			for (EntityData e : entityDataMap.values()) {
				if (e.entityUuid.equals(entityUuid)) {
					return e;
				}
			}
			return null;
		}

		public static boolean containsPlayer(UUID playerUuid) {
			if (allowedPlayers.contains(playerUuid)) {
				return true;
			}
			for (Map.Entry<Integer, EntityData> entry : entityDataMap.entrySet()) {
				if (entry.getValue().visibleToPlayers.contains(playerUuid)) {
					return true;
				}
			}
			return false;
		}

		public static void removePlayer(UUID playerUuid) {
			entityDataMap.forEach((key, value) -> {
				value.visibleToPlayers.remove(playerUuid);
			});
		}

		@SuppressWarnings("unused") // I need kill switches
		public static EntityData getOrCreateEntityData(Entity entity) {
			return entityDataMap.computeIfAbsent(entity.getEntityId(), key -> generateEntityData(entity));
		}

		public static void updateEntityData(Entity entity) {
			entityDataMap.compute(entity.getEntityId(), (id, data) -> generateEntityData(entity));
		}

		private static @Nullable EntityData generateEntityData(Entity entity) {
			EntityData internal = null;
			if (ScoreboardUtils.checkTag(entity, FakePlayerBoss.identityTag)) {
				// we have to parse parameters here, because this technically gets called BEFORE entityAddToWorldEvent - who thought this was a good idea?
				final FakePlayerBoss.Parameters parameters = BossParameters.getParameters(entity, FakePlayerBoss.identityTag, new FakePlayerBoss.Parameters());
				final SkinData skinData = parameters.SKIN_NAME;
				NametagData nameTagData = null;
				if (entity.customName() != null) {
					if (entity.isCustomNameVisible()) {
						nameTagData = getNametagData(entity, entity.customName(), 64);
					} else {
						nameTagData = getNametagData(entity, entity.customName(), 4);
					}
				}
				internal = EntityData.of(entity.getUniqueId(), entity.getEntityId(), skinData, parameters, nameTagData);
			} else if (NPCS_ENABLED && WHITELISTED_ENTITIES.contains(entity.getType()) && entity.customName() != null) {
				// do something with villager names here
				// NPC code here
				NametagData nameTagData;
				if (entity.isCustomNameVisible()) {
					nameTagData = getNametagData(entity, entity.customName(), 64);
				} else {
					nameTagData = getNametagData(entity, entity.customName(), 4);
				}
				final SkinData skinData = PlayerSkinManager.fetchSkin(entity);
				internal = EntityData.of(entity.getUniqueId(), entity.getEntityId(), skinData, new FakePlayerBoss.Parameters(), nameTagData);
			}
			return internal;

		}

		private static @Nullable EntityData getEntityData(int entityId) {
			return entityDataMap.get(entityId);
		}
	}

	public static final Set<UUID> allowedPlayers = new HashSet<>();
	private static final WrappedDataValue ENABLE_SECOND_SKIN_LAYER = new WrappedDataValue(17, Registry.get(Byte.class), (byte) (0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40));
	public static final String PERMISSON_STRING = "monumenta.fakeplayers";

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		// we want to replace the entity type with a player
		// surely nothing will go wrong
		PacketContainer packet = event.getPacket();
		PacketType packetType = event.getPacketType();
		if (packetType.equals(PacketType.Play.Server.SPAWN_ENTITY)) {
			handleSpawnEntityPacket(event, packet, player);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_DESTROY)) {
			handleEntityDestroyPacket(event, packet, player);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_METADATA)) {
			rewriteFakePlayerMetadata(event, packet, player);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_EQUIPMENT)) {
			handleEntityEquipmentPacket(event, packet, player);
		} else if (packetType.equals(PacketType.Play.Server.ENTITY_HEAD_ROTATION)) {
			handleEntityRotationPacket(event, packet, player);
		} else if (packetType.equals(PacketType.Play.Server.SCOREBOARD_TEAM)) {
			handleTeamPacket(event, packet, player);
		}
	}

	public void handleSpawnEntityPacket(PacketEvent event, PacketContainer packet, Player player) {
		if (!allowedPlayers.contains(player.getUniqueId())) {
			return;
		}
		Entity entity = packet.getEntityModifier(event).readSafely(0);
		if (entity == null) {
			return;
		}
		@Nullable EntityData data = PlayerData.getOrCreateEntityData(entity);
		if (data == null) {
			return;
		}

		event.setCancelled(true);
		// associate entity id and uuid with this player
		PlayerData.addEntityToPlayer(player.getUniqueId(), entity.getEntityId());

		// this must be sent before the fake player spawns in
		sendPlayerInfoPacket(player, entity, data);
		sendSpawnPlayerPacket(player, entity);
		sendTeamPacket(player, entity, data);

		if (data.nametag != null) {
			sendSpawnNametagPacket(player, data.nametag);
			sendPassengerPacket(player, entity, data.nametag);
		}
	}

	private void sendPassengerPacket(Player player, Entity entity, NametagData data) {
		PacketContainer entityPacket = new PacketContainer(PacketType.Play.Server.MOUNT);
		entityPacket.getIntegers().writeSafely(0, entity.getEntityId());
		List<Entity> e = entity.getPassengers();
		int[] array = new int[e.size() + 1];
		for (int i = 0; i < e.size(); i++) {
			array[i] = e.get(i).getEntityId();
		}
		array[e.size()] = data.entityId;
		entityPacket.getIntegerArrays().writeSafely(0, array);
		sendPacketNoFilters(player, entityPacket);
	}

	private void handleEntityEquipmentPacket(PacketEvent event, PacketContainer packet, Player player) {
		if (!PlayerData.containsPlayer(player.getUniqueId())) {
			return;
		}
		Entity entity = packet.getEntityModifier(event).readSafely(0);
		if (entity == null || !PlayerData.containsEntityForPlayer(player.getUniqueId(), entity.getEntityId())) {
			return;
		}
		EntityData data = PlayerData.getEntityData(entity.getEntityId());
		if (data == null || data.params.SHOW_ARMOR) {
			return;
		}
		List<Pair<EnumWrappers.ItemSlot, ItemStack>> items = packet.getSlotStackPairLists().readSafely(0);
		if (items == null) {
			return;
		}
		for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : items) {
			EnumWrappers.ItemSlot itemSlot = pair.getFirst();
			// don't hide mainhand or offhand
			if (itemSlot.equals(EnumWrappers.ItemSlot.MAINHAND) || itemSlot.equals(EnumWrappers.ItemSlot.OFFHAND)) {
				continue;
			}
			pair.setSecond(new ItemStack(Material.AIR));
		}
		packet.getSlotStackPairLists().writeSafely(0, items);
	}

	private void sendSpawnPlayerPacket(Player recievingPlayer, Entity entity) {
		// create actual player
		// https://minecraft.wiki/w/Java_Edition_protocol/Packets?oldid=2773257#Spawn_Entity
		PacketContainer playerPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		playerPacket.getModifier().writeDefaults();
		PacketPlayOutSpawnEntityHandle playerHandle = PacketPlayOutSpawnEntityHandle.createHandle(playerPacket.getHandle());
		playerHandle.setEntityId(entity.getEntityId());
		playerHandle.setEntityUUID(entity.getUniqueId());
		Location loc = entity.getLocation();
		playerHandle.setPosX(loc.getX());
		playerHandle.setPosY(loc.getY());
		playerHandle.setPosZ(loc.getZ());
		playerHandle.setPitch(loc.getPitch());
		playerHandle.setYaw(loc.getYaw());
		playerHandle.setCommonEntityType(CommonEntityType.PLAYER);
		sendPacketNoFilters(recievingPlayer, playerPacket);
	}

	private static NametagData getNametagData(Entity entity, Component customName, @Nullable Integer distanceInBlocks) {
		TextDisplay nameTag = (TextDisplay) NmsUtils.getVersionAdapter().spawnWorldlessEntity(EntityType.TEXT_DISPLAY, entity.getWorld());
		nameTag.setAlignment(TextDisplay.TextAlignment.CENTER);
		nameTag.text(customName);
		nameTag.setInterpolationDuration(0);
		nameTag.setInterpolationDelay(-1);
		nameTag.setBillboard(Billboard.CENTER);
		nameTag.setBrightness(new Display.Brightness(15, 15));
		nameTag.setViewRange(distanceInBlocks != null ? ((float) distanceInBlocks / 64f) : 1f);
		Location loc = entity.getLocation().add(0, entity.getHeight() + nameTag.getDisplayHeight(), 0);
		float ridingHeight = nameTag.getDisplayHeight() + 0.25f;
		Transformation base = nameTag.getTransformation();
		nameTag.setTransformation(new Transformation(new Vector3f(0, ridingHeight, 0), base.getLeftRotation(), base.getScale(), base.getRightRotation()));
		return NametagData.of(nameTag, loc);
	}

	private void sendSpawnNametagPacket(Player player, NametagData data) {
		PacketContainer entityPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		entityPacket.getModifier().writeDefaults();
		PacketPlayOutSpawnEntityHandle entityHandle = PacketPlayOutSpawnEntityHandle.createHandle(entityPacket.getHandle());
		entityHandle.setEntityId(data.entityId);
		entityHandle.setEntityUUID(data.entityUuid);
		entityHandle.setPosX(data.worldPos.getX());
		entityHandle.setPosY(data.worldPos.getY());
		entityHandle.setPosZ(data.worldPos.getZ());
		entityHandle.setCommonEntityType(CommonEntityType.byEntityType(EntityType.TEXT_DISPLAY));
		sendPacketNoFilters(player, entityPacket);
		sendNametagMetadataPacket(player, data);
	}

	private void sendNametagMetadataPacket(Player player, NametagData data) {
		PacketPlayOutEntityMetadataHandle handle = PacketPlayOutEntityMetadataHandle.createNew(data.entityId, data.dataWatcher, true);
		sendPacketNoFilters(player, handle);
	}

	@SuppressWarnings("EnumOrdinal")
	private void rewriteFakePlayerMetadata(PacketEvent event, PacketContainer packet, Player player) {
		if (!PlayerData.containsPlayer(player.getUniqueId())) {
			return;
		}
		Entity entity = packet.getEntityModifier(event).readSafely(0);
		if (entity == null || !PlayerData.containsEntityForPlayer(player.getUniqueId(), entity.getEntityId())) {
			return;
		}

		// remove entities that conflict with https://minecraft.wiki/w/Java_Edition_protocol/Entity_metadata?oldid=2769478#Player
		StructureModifier<List<WrappedDataValue>> watchableAccessor = packet.getDataValueCollectionModifier();
		List<WrappedDataValue> metaItems = watchableAccessor.readSafely(0);
		if (metaItems == null || metaItems.isEmpty()) {
			return;
		}

		ListIterator<WrappedDataValue> metaIterator = metaItems.listIterator();
		while (metaIterator.hasNext()) {
			WrappedDataValue metadataItem = metaIterator.next();
			// hardcoded player exceptions
			int index = metadataItem.getIndex();
			Object value = metadataItem.getValue();
			// death pose
			if (index == 6 && value instanceof Enum<?> pose && pose.ordinal() == Pose.DYING.ordinal()) {
				metaIterator.remove();
				continue;
			}
			// Additional hearts
			if (index == 15 && !(value instanceof Float)) {
				metaIterator.remove();
				continue;
			}
			// Score
			if (index == 16 && !(value instanceof Integer)) {
				metaIterator.remove();
				continue;
			}
			// we override this value (skin data)
			if (index == 17) {
				metaIterator.remove();
				continue;
			}
			// mainhand/offhand
			if (index == 18 && !(value instanceof Byte)) {
				metaIterator.remove();
				continue;
			}
			// parrot shoulder nbt
			if ((index == 19 || index == 20) && !(value instanceof NbtCompound)) {
				metaIterator.remove();
				continue;
			}
			if (index > 20) {
				metaIterator.remove();
				continue;
			}
		}
		// set second skin layer to show
		metaIterator.add(ENABLE_SECOND_SKIN_LAYER);
		watchableAccessor.writeSafely(0, metaItems);
		event.setPacket(packet);
	}

	private void sendPlayerInfoPacket(Player recievingPlayer, Entity entity, EntityData entityData) {
		// create player info packet
		// https://minecraft.wiki/w/Java_Edition_protocol/Packets?oldid=2773257#Player_Info_Update
		PacketContainer playerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		playerInfoPacket.getModifier().writeDefaults();
		ClientboundPlayerInfoUpdatePacketHandle playerInfoHandle = ClientboundPlayerInfoUpdatePacketHandle.createHandle(playerInfoPacket.getHandle());
		playerInfoHandle.setAction(EnumPlayerInfoActionHandle.ADD_PLAYER);
		// setting the fake player's name to the recievingPlayer's name makes the nametag completely invisible
		// setting the fake player's name to an empty string will show a nametag but it is very subtle (Wynncraft)
		// but we can set this string to anything, as long as it is within the 16 character limit
		GameProfileHandle playerData = GameProfileHandle.createNew(entity.getUniqueId(), entityData.entityName);
		// use player's current skin
		// playerData.setAllProperties(GameProfileHandle.getForPlayer(recievingPlayer));
		SkinData textureData;
		if (entityData.entitySkin != null) {
			textureData = entityData.entitySkin;
		} else {
			textureData = PlayerSkinManager.fetchSkin(entity);
		}
		PropertyHandle propertyData = PropertyHandle.createNew("textures", textureData.texture(), textureData.signature());
		playerData.putProperty("textures", propertyData);
		playerInfoHandle.setPlayers(List.of(PlayerInfoDataHandle.createNew(playerInfoHandle, playerData, 0, GameMode.ADVENTURE, ChatText.fromMessage(""), false)));
		sendPacket(recievingPlayer, playerInfoPacket);
	}

	private void handleEntityDestroyPacket(PacketEvent event, PacketContainer packet, Player player) {
		if (!PlayerData.containsPlayer(player.getUniqueId())) {
			return;
		}
		// https://minecraft.wiki/w/Java_Edition_protocol/Packets?oldid=2773257#Remove_Entities
		PacketPlayOutEntityDestroyHandle entityHandle = PacketPlayOutEntityDestroyHandle.createHandle(packet.getHandle());
		int[] entityIds = entityHandle.getEntityIds();
		// we don't cancel this event, since we want these entities to also be removed on the client

		// Lookup to see if one of the entity ids has a uuid match
		List<UUID> uuids = new ArrayList<>(1);
		List<Integer> newIds = new ArrayList<>(Arrays.stream(entityIds).boxed().toList());
		for (int entityId : entityIds) {
			@Nullable
			EntityData data = PlayerData.getEntityData(entityId);
			if (data == null || data.entityUuid == null) {
				continue;
			}
			if (!PlayerData.removeEntityFromPlayer(player.getUniqueId(), entityId)) {
				continue;
			}
			uuids.add(data.entityUuid);
			if (data.nametag != null) {
				newIds.add(data.nametag.entityId);
			}
		}
		if (!uuids.isEmpty()) {
			// https://minecraft.wiki/w/Java_Edition_protocol/Packets?oldid=2773257#Player_Info_Remove
			for (UUID uuid : uuids) {
				Entity entity = Bukkit.getEntity(uuid);
				if (entity == null) {
					continue;
				}
				@Nullable
				Team entityTeam = ScoreboardUtils.getEntityTeam(entity);
				if (entityTeam != null) {
					entityTeam.removeEntry(uuid.toString());
				}
				sendDestroyNametagPacket(entity.getEntityId());
			}
			sendPlayerInfoRemovePacket(player, uuids);
		}
		if (entityIds.length != newIds.size()) {
			int[] newEntityIds = newIds.stream().mapToInt(i -> i).toArray();
			entityHandle.setMultipleEntityIds(newEntityIds);
		}
		event.setPacket(PacketContainer.fromPacket(entityHandle.getRaw()));
	}

	private void sendPlayerInfoRemovePacket(Player player, List<UUID> uuids) {
		PacketContainer playerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE);
		playerInfoPacket.getModifier().writeDefaults();
		// I tried to use BKCommonLib here but it failed to write a list of uuids for some reason
		playerInfoPacket.getLists(Converters.passthrough(UUID.class)).writeSafely(0, uuids);
		sendPacket(player, playerInfoPacket);
	}

	private static void sendDestroyNametagPacket(int entityId) {
		EntityData data = PlayerData.getEntityData(entityId);
		if (data == null || data.nametag == null) {
			return;
		}
		PacketPlayOutEntityDestroyHandle handle = PacketPlayOutEntityDestroyHandle.createNewSingle(data.nametag.entityId);
		for (UUID uuid : data.visibleToPlayers) {
			Player player = Bukkit.getPlayer(uuid);
			if (player == null) {
				continue;
			}
			sendPacket(player, handle);
		}
	}

	/**
	 * Hacky code to make sure the body rotates with the head
	 */
	private void handleEntityRotationPacket(PacketEvent event, PacketContainer packet, Player player) {
		Entity entity = packet.getEntityModifier(event).readSafely(0);
		if (entity == null || !PlayerData.containsEntityForPlayer(player.getUniqueId(), entity.getEntityId())) {
			return;
		}
		// this is the only solution I could find for force-updating the body to match the head's rotation for fake players
		// this technically shouldn't be noticable but is a really hacky fix
		Location loc = entity.getLocation();
		// get the entity headrotation
		float headRotation = NmsUtils.getVersionAdapter().getEntityHeadRotation(entity);
		if (headRotation == loc.getYaw()) {
			return;
		}
		entity.setRotation(loc.getYaw(), loc.getPitch());
	}

	/**
	 * Super hacky code for making colored glowing kind of work
	 * Has many limitations and is not reliable
	 * FIXME: Remove if this code doesn't work or breaks on a minecraft version update
	 */
	private void handleTeamPacket(PacketEvent event, PacketContainer packet, Player player) {
		if (!PlayerData.containsPlayer(player.getUniqueId())) {
			return;
		}
		PacketPlayOutScoreboardTeamHandle handle = PacketPlayOutScoreboardTeamHandle.createHandle(packet.getHandle());
		Collection<String> names = handle.getPlayers();
		List<String> copy = new ArrayList<>(names);
		ListIterator<String> namesIterator = copy.listIterator();
		boolean changed = false;
		while (namesIterator.hasNext()) {
			String name = namesIterator.next();
			// if found, change it
			UUID uuid;
			try {
				uuid = UUID.fromString(name);
			} catch (IllegalArgumentException e) {
				// not a uuid
				continue;
			}
			EntityData data = PlayerData.getEntityDataByUUID(uuid);
			if (data == null) {
				Entity entity = Bukkit.getEntity(uuid);
				if (entity == null) {
					continue;
				}

				if (!ScoreboardUtils.checkTag(entity, FakePlayerBoss.identityTag)) {
					continue;
				}

				data = PlayerData.getOrCreateEntityData(entity);
			}

			if (data != null) {
				changed = true;
				namesIterator.remove();
				String newName = data.entityName;
				MMLog.debug("Changing team name from " + name + " to " + newName);
				if (newName != null && !copy.contains(newName)) {
					namesIterator.add(newName);
				}
			}
		}
		if (changed && !copy.isEmpty()) {
			handle.setPlayers(copy);
			event.setPacket(PacketContainer.fromPacket(handle.getRaw()));
		} else if (copy.isEmpty()) {
			event.setCancelled(true);
		}
	}

	private static void sendTeamPacket(Player player, Entity entity, EntityData data) {
		if (GlowingManager.isCustomGlowingForPlayer(entity, player)) {
			return;
		}
		Team team = ScoreboardUtils.getEntityTeam(entity);
		if (team != null) {
			String teamName = team.getName();
			PacketPlayOutScoreboardTeamHandle leave = PacketPlayOutScoreboardTeamHandle.createNew();
			leave.setName(teamName);
			leave.setMethod(PacketPlayOutScoreboardTeamHandle.METHOD_LEAVE);
			leave.setPlayers(List.of(data.entityUuid.toString()));
			sendPacketNoFilters(player, leave);

			PacketPlayOutScoreboardTeamHandle join = PacketPlayOutScoreboardTeamHandle.createNew();
			join.setName(teamName);
			join.setMethod(PacketPlayOutScoreboardTeamHandle.METHOD_JOIN);
			join.setPlayers(List.of(data.entityName));
			sendPacketNoFilters(player, join);
		}
	}

	private static void sendPacketNoFilters(Player receiver, PacketContainer packet) {
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet, false);
		} catch (Exception e) {
			MMLog.severe("Caught exception when sending packet: ", e);
		}
	}

	private static void sendPacketNoFilters(Player receiver, PacketHandle packet) {
		sendPacketNoFilters(receiver, PacketContainer.fromPacket(packet.getRaw()));
	}

	private static void sendPacket(Player receiver, PacketContainer packet) {
		try {
			ProtocolLibrary.getProtocolManager().sendServerPacket(receiver, packet, true);
		} catch (Exception e) {
			MMLog.severe("Caught exception when sending packet: ", e);
		}
	}

	private static void sendPacket(Player receiver, PacketHandle packet) {
		sendPacket(receiver, PacketContainer.fromPacket(packet.getRaw()));
	}


	public static void joinPlayer(UUID uuid) {
		allowedPlayers.add(uuid);
	}

	// Public static methods //

	/**
	 * Remove cached Fake Player entity ids and uuids on logout
	 * To prevent the server for leaking memory
	 * @param uuid - UUID of the player to remove data for
	 */
	public static void leavePlayer(UUID uuid) {
		allowedPlayers.remove(uuid);
		PlayerData.removePlayer(uuid);
	}

	/**
	 * Remove cached fake player entity
	 * @param entityId - UUID of the player to remove data for
	 */
	public static void scheduleRemove(int entityId) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> PlayerData.removeEntity(entityId));
	}

	/**
	 * Updates the fake player for CustomName or Skin changes
	 * @param entity  the fake player entity
	 */
	public static void updateFakePlayer(LivingEntity entity) {
		PlayerData.updateEntityData(entity);
	}

}
