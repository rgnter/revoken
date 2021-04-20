package eu.battleland.revoken.serverside.game.mechanics.wearables;


import eu.battleland.common.Revoken;
import eu.battleland.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.wearables.model.Wearable;
import eu.battleland.revoken.serverside.statics.PktStatics;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import com.google.common.io.ByteStreams;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.GameMode;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import xyz.rgnt.mth.tuples.Triple;
import org.jetbrains.annotations.NotNull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2(topic = "Wearable Mechanic")
public class WearableMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public final NamespacedKey SAVE_KEY;

    @Getter
    private final Map<UUID, Map<String, Triple<EntityPlayer, Entity, Wearable>>> wardrobe = new ConcurrentHashMap<>();

    public WearableMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
        super.setTickableAsync(true);

        SAVE_KEY = new NamespacedKey("battleland", "wearables");
    }


    /**
     * Adds wearable to player.
     *
     * @param wearable Wearable Object
     * @param player   Player   Object
     * @return boolean result
     */
    public boolean addWearable(@NotNull Player player, @NotNull Wearable wearable) {
        final EntityPlayer nmsPlayer = PktStatics.getNmsPlayer(player);
        final WorldServer worldServer = nmsPlayer.getWorldServer();

        final Entity wearableEntity = EntityTypes.ARMOR_STAND.createCreature(
                worldServer, null, null, null,
                PktStatics.getBlockLocation(player), EnumMobSpawn.COMMAND, false, false
        );
        if (wearableEntity == null) {
            log.error("Failed to instance class ARMOR_STAND for Wearable: " + wearable.getIdentifier() + ", Player: " + player.getName());
            return false;
        }
        hideEntities(wearableEntity);

        wearableEntity.setCustomName(IChatBaseComponent.ChatSerializer.b("wearable_" + wearable.getIdentifier()));
        wearableEntity.setSlot(EnumItemSlot.HEAD, wearable.getNativeItemStack());
        wearableEntity.persist = false;


        this.wardrobe.compute(player.getUniqueId(), (uuid, data) -> {
            Map<String, Triple<EntityPlayer, Entity, Wearable>> origin = (
                    data != null ? data : new ConcurrentHashMap<>()
            );

            var newData = origin.compute(wearable.getIdentifier(), (wearableId, wearableData) -> {
                if (wearableData != null) {
                    deleteWearableFromWorld(wearableData);
                    log.warn("Overriding already existing wearable '{}' on player '{}'!", wearableId, player.getName());
                }
                return Triple.of(nmsPlayer, wearableEntity, wearable);
            });

            createWearableInWorld(newData, player);
            return origin;
        });

        saveWearablesToStorage(player);
        return true;
    }

    public void addAllWearables(@NotNull Player player, @NotNull Wearable ... wearables) {
        for (Wearable wearable : wearables) {
            addWearable(player, wearable);
        }
    }

    /**
     * Removes all wearables from player.
     *
     * @param player Player   Object
     * @return boolean result
     */
    public boolean remWearable( @NotNull Player player, @NotNull Wearable wearable) {
        this.wardrobe.computeIfPresent(player.getUniqueId(), (playerUuid, wearables) -> {
            wearables.computeIfPresent(wearable.getIdentifier(), (wearableId, wearableData) -> {
                deleteWearableFromWorld(wearableData);
                return null;
            });
            wearables.remove(wearable.getIdentifier());
            return wearables;
        });

        saveWearablesToStorage(player);
        return true;
    }

    /**
     * Removes all wearables from player.
     *
     * @param player Player   Object
     * @return boolean result
     */
    public boolean remAllWearables(@NotNull Player player) {
        this.wardrobe.computeIfPresent(player.getUniqueId(), (playerUuid, wearables) -> {
            wearables.forEach((wearableId, wearableData) -> {
                deleteWearableFromWorld(wearableData);
            });
            return null;
        });
        this.wardrobe.remove(player.getUniqueId());

        clearWearablesInStorage(player);
        return true;
    }

    private void createWearableInWorld(@NotNull Triple<EntityPlayer, Entity, Wearable> wearableData, @NotNull Player owner) {
        var entity = wearableData.getSecond();
        var nmsPlayer = PktStatics.getNmsPlayer(owner);
        WorldServer worldServer = entity.getWorld().getMinecraftWorld();

        worldServer.addEntity(entity);
        entity.a(nmsPlayer, true);
    }

    private void createAllWearablesInWorld(@NotNull Player owner) {
        System.out.println("call");
        var wearables = this.wardrobe.get(owner.getUniqueId());
        if(wearables != null) {
            wearables.forEach((wearableId, wearableData) ->{
                System.out.println(wearableId);
                createWearableInWorld(wearableData, owner);
            });
        }
    }

    /**
     * Destroys wearable data from world
     *
     * @param wearableData Wearable data
     */
    private boolean deleteWearableFromWorld(@NotNull Triple<EntityPlayer, Entity, Wearable> wearableData) {
        var entity = wearableData.getSecond();
        WorldServer worldServer = entity.getWorld().getMinecraftWorld();

        wearableData.getSecond().stopRiding(true);

        try {
            worldServer.removeEntity(wearableData.getSecond());
            return true;
        } catch (Exception exception) {
            var location = wearableData.getSecond().getPositionVector();
            log.error("Failed to remove entity of wearable '{}'(id: {}, location: {},{},{},{})from World",
                    wearableData.getThird().getIdentifier(),
                    wearableData.getSecond().getId(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    wearableData.getSecond().getWorld().getWorld().getName(),
                    exception
            );
            return false;
        }
    }

    private void deleteAllWearablesFromWorld(@NotNull Player player) {
        var wearables = this.wardrobe.get(player.getUniqueId());
        if(wearables != null) {
            wearables.forEach((wearableId, wearableData) ->{
                deleteWearableFromWorld(wearableData);
            });
        }
    }


    private @NotNull Set<String> loadWearablesFromStorage(@NotNull Player player) {
        final Set<String> wearables = new HashSet<>();
        final var dataContainer = player.getPersistentDataContainer();
        if(!dataContainer.has(SAVE_KEY, PersistentDataType.BYTE_ARRAY))
            return wearables;

        final var data = dataContainer.get(SAVE_KEY, PersistentDataType.BYTE_ARRAY);
        if(data == null || data.length == 0)
            return wearables;
        final var readerStream = ByteStreams.newDataInput(data);

        try {
            while (true) {
                wearables.add(readerStream.readUTF());
            }
        } catch (IllegalStateException ignored) {
            return wearables;
        }

    }

    private void saveWearablesToStorage(@NotNull Player player) {
        var wearableData = this.wardrobe.get(player.getUniqueId());
        if(wearableData == null) {
            log.warn("Tried to save player '{}' with no wearables.", player.getName());
            return;
        }
        var wearables = wearableData.keySet();

        final var dataContainer = player.getPersistentDataContainer();
        final var writerStream = ByteStreams.newDataOutput();

        wearables.forEach(writerStream::writeUTF);
        dataContainer.set(SAVE_KEY, PersistentDataType.BYTE_ARRAY, writerStream.toByteArray());
    }

    private void clearWearablesInStorage(@NotNull Player player) {
        player.getPersistentDataContainer().remove(SAVE_KEY);
    }


    private void hideEntities(@NotNull Entity... entities) {
        for (Entity entity : entities) {
            entity.persistentInvisibility = false;
            entity.setInvulnerable(true);
            entity.setInvisible(true);

            if (entity instanceof EntityArmorStand) {
                ((EntityArmorStand) entity).setMarker(true);
            }
            entity.setSilent(true);
            entity.setBoundingBox(new AxisAlignedBB(0, 0, 0, 0, 0, 0));

            if (entity instanceof EntityLiving)
                ((EntityLiving) entity).collides = false;

        }
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
        Bukkit.getCommandMap().register("revoken", new Command("test") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                var player = (Player) sender;
                Wearable wearable0 = Wearable.builder()
                        .identifier("backpack")
                        .baseMaterial(Material.POPPY)
                        .modelData(1)
                        .build();
                Wearable wearable1 = Wearable.builder()
                        .identifier("backpack2")
                        .baseMaterial(Material.POPPY)
                        .modelData(2)
                        .build();

                addAllWearables(player, wearable0, wearable1);
                sender.sendMessage("§a!");

                return true;
            }
        });
    }

    @Override
    public void terminate() {
        Bukkit.getOnlinePlayers().stream()
                .peek(this::saveWearablesToStorage)
                .forEach(this::deleteAllWearablesFromWorld);
    }

    @Override
    public void reload() {
    }


    @Override
    public void asyncTick() {
        this.wardrobe.forEach((uuid, wearables) ->
        {
            if(wearables == null)
                return;

            wearables.forEach((wearableId, wearableData) -> {
                if(wearableData == null)
                    return;

                var player = wearableData.getFirst();
                var entity = wearableData.getSecond();
                if(player == null)
                    return;
                if (player.networkManager.isConnected())
                    PktStatics.sendPacketToAll(new PacketPlayOutEntityHeadRotation(entity, (byte) ((int) (player.yaw * 256.0F / 360.0F))));
            });
        });
    }

    @EventHandler
    public void gamemodeSwitchEvent(PlayerGameModeChangeEvent event) {
        if(event.getNewGameMode() == GameMode.SPECTATOR) {
            deleteAllWearablesFromWorld(event.getPlayer());
        }
        else if(event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            createAllWearablesInWorld(event.getPlayer());
        }
    }

    @EventHandler
    public void playerDeathEvent(PlayerDeathEvent event) {
        deleteAllWearablesFromWorld(event.getEntity());
    }

    @EventHandler
    public void playerRespawnEvent(PlayerRespawnEvent event) {
        createAllWearablesInWorld(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        System.out.println(loadWearablesFromStorage(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveWearablesToStorage(event.getPlayer());
        deleteAllWearablesFromWorld(event.getPlayer());
        this.wardrobe.remove(event.getPlayer().getUniqueId());

    }

}
