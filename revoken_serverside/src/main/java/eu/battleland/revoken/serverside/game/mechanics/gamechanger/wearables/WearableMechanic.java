package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables;


import com.google.common.io.ByteStreams;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.mcdev.BackpackEntity;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.model.Wearable;
import eu.battleland.revoken.serverside.statics.PktStatics;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.mth.tuples.Triple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2(topic = "Wearable Mechanic")
public class WearableMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public final NamespacedKey WEARABLES_DATA_KEY = new NamespacedKey("battleland", "wearables");

    private final Map<UUID, Map<@NotNull String,  @NotNull Triple<EntityPlayer, Entity, Wearable>>> wardrobe = new ConcurrentHashMap<>();

    @Getter
    private @NotNull Optional<AStore> configuration = Optional.empty();

    /**
     * Default constructor
     *
     * @param plugin Revoken plugin
     */
    public WearableMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
        super.setTickable(true);
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
        final BlockPosition position = PktStatics.getBlockLocation(player);

        final Entity wearableEntity =
                new BackpackEntity(worldServer, position.getX(), position.getY(), position.getZ());
        PktStatics.makeEntitiesDummmies(wearableEntity);

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

            createWearableInWorld(newData);
            return origin;
        });

        saveWearablesToStorage(player);
        return true;
    }

    /**
     * Adds all wearables to player
     *
     * @param player    Player
     * @param wearables Wearable variadic args
     */
    public void addAllWearables(@NotNull Player player, @NotNull Wearable... wearables) {
        for (Wearable wearable : wearables) {
            addWearable(player, wearable);
        }
    }

    /**
     * Removes wearable from player.
     *
     * @param player Player
     * @return boolean result
     */
    public boolean remWearable(@NotNull Player player, @NotNull Wearable wearable) {
        this.wardrobe.computeIfPresent(player.getUniqueId(), (playerUuid, wearables) -> {
            deleteWearableFromWorld(wearables.remove(wearable.getIdentifier())); // delete removed wearable from world
            return wearables;
        });

        saveWearablesToStorage(player);
        return true;
    }

    /**
     * Removes all wearables from player.
     *
     * @param player Player
     * @return boolean result
     */
    public boolean remAllWearables(@NotNull Player player) {
        this.wardrobe.remove(player.getUniqueId()).forEach((identifier, wearableData) -> {
            deleteWearableFromWorld(wearableData); // delete each wearable from world
        });

        wipeWearablesInStorage(player);
        return true;
    }

    /**
     * Creates wearable in world
     * @param wearableData Wearable data
     * @return boolean
     */
    private boolean createWearableInWorld(@NotNull Triple<EntityPlayer, Entity, Wearable> wearableData) {
        var entity = wearableData.getSecond();
        var nmsPlayer = wearableData.getFirst();
        WorldServer worldServer = entity.getWorld().getMinecraftWorld();

        worldServer.addEntity(entity);
        entity.passengerTick();
        return entity.a(nmsPlayer, true); // ride player, force true
    }


    /**
     * Deletes wearable from world
     * @param wearableData Wearable data
     * @return boolean
     */
    private boolean deleteWearableFromWorld(@Nullable Triple<EntityPlayer, Entity, Wearable> wearableData) {
        if(wearableData == null)
            return false;

        var entity = wearableData.getSecond();
        WorldServer worldServer = entity.getWorld().getMinecraftWorld();

        wearableData.getSecond().stopRiding(true);

        try {
            worldServer.removeEntity(wearableData.getSecond());
            return true;
        } catch (Exception exception) {
            var location = wearableData.getSecond().getPositionVector();
            log.error("Failed to remove entity of wearable '{}'(id: {}, location: {},{},{},{}) from world '{}'",
                    wearableData.getThird().getIdentifier(),
                    wearableData.getSecond().getId(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    wearableData.getSecond().getWorld().getWorld().getName(),
                    worldServer.getWorld().getName(),
                    exception
            );
            return false;
        }
    }

    /**
     * Creates all wearables of player in world
     * @param owner Player
     */
    private void createAllWearablesInWorld(@NotNull Player owner) {
        var wearables = this.wardrobe.get(owner.getUniqueId());
        if (wearables != null) {
            wearables.forEach((wearableId, wearableData) -> {
                createWearableInWorld(wearableData);
            });
        }
    }

    /**
     * Deletes all wearables of player from world
     * @param player Player
     */
    private void deleteAllWearablesFromWorld(@NotNull Player player) {
        var wearables = this.wardrobe.get(player.getUniqueId());
        if (wearables != null) {
            wearables.forEach((wearableId, wearableData) -> {
                deleteWearableFromWorld(wearableData);
            });
        }
    }

    private @NotNull Set<String> loadWearablesFromStorage(@NotNull Player player) {
        final Set<String> wearables = new HashSet<>();
        final var dataContainer = player.getPersistentDataContainer();
        if (!dataContainer.has(WEARABLES_DATA_KEY, PersistentDataType.BYTE_ARRAY))
            return wearables;

        final var data = dataContainer.get(WEARABLES_DATA_KEY, PersistentDataType.BYTE_ARRAY);
        if (data == null || data.length == 0)
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
        if (wearableData == null) {
            log.warn("Tried to save player '{}' with no wearables.", player.getName());
            return;
        }
        var wearables = wearableData.keySet();

        final var dataContainer = player.getPersistentDataContainer();
        final var writerStream = ByteStreams.newDataOutput();

        wearables.forEach(writerStream::writeUTF);
        dataContainer.set(WEARABLES_DATA_KEY, PersistentDataType.BYTE_ARRAY, writerStream.toByteArray());
    }

    private void wipeWearablesInStorage(@NotNull Player player) {
        player.getPersistentDataContainer().remove(WEARABLES_DATA_KEY);
    }


    @Override
    public void initialize() throws Exception {
        setupConfig();
        setupRepo();

        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
        Bukkit.getCommandMap().register("revoken", new Command("backpacktest") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                var player = (Player) sender;
                Wearable wearable0 = Wearable.builder()
                        .identifier("backpack")
                        .baseMaterial(Material.POPPY)
                        .modelData(1)
                        .build();

                addAllWearables(player, wearable0);
                sender.sendMessage("§aYou are now wearing a backpack");
                ((Player) sender).setResourcePack(
                        "http://battleland.eu/file/battleland.zip",
                        "48c9f5f6ab0416d8b05403cac7f12ff1bf767b59"
                );
                sender.sendMessage("§7Accept the resource pack prompt to see the backpack");
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
        setupConfig();
        setupRepo();
    }

    @Override
    public void tick() {
        this.wardrobe.forEach((uuid, wearables) ->
        {
            if (wearables == null)
                return;

            // todo: maybe rather update rotation when player sends packet?
            wearables.forEach((wearableId, wearableData) -> {
                var player = wearableData.getFirst();
                var entity = wearableData.getSecond();
                var wearable = wearableData.getThird();

                if (player.networkManager.isConnected()) {
                    PktStatics.sendPacketToAll(new PacketPlayOutEntityHeadRotation(entity, (byte) ((int) (player.yaw * 256.0F / 360.0F))));
                }
            });
        });
    }


    private void setupConfig() {
        try {
            if (configuration.isEmpty())
                configuration = Optional.of(getPlugin().instance().getStorageProvider()
                        .provideYaml("resources", "configs/mechanics/gamechanger/wearables.yaml", true));
        } catch (Exception x) {
            log.error("Failed to provide mechanic config", x);
        }
    }

    private void setupRepo() {
        configuration.ifPresent(config -> {
            var data = config.getData();
            data.getKeys("wearables").forEach(wearableId -> {


                try {
                    var wearableData = data.getSector("wearables." + wearableId);
                    if(wearableData == null) {
                        throw new Exception("failed to access wearable data");
                    }

                    var wearableBuilder = Wearable
                            .builder()
                            .identifier(wearableId);
                    var impl = wearableData.getStringOpt("impl");
                    if(impl.isEmpty())
                        impl = Optional.of("DummyWearable");

                    var modelData = wearableData.getIntOpt("model_data");
                    if(modelData.isEmpty())
                        throw new Exception("missing model data");
                    wearableBuilder.modelData(modelData.get());

                    var baseMaterialName = wearableData.getStringOpt("base_material");
                    if(baseMaterialName.isEmpty())
                        throw new Exception("missing base material name");
                    Material baseMaterial = Material.getMaterial(baseMaterialName.get().toUpperCase());
                    if(baseMaterial == null)
                        throw new Exception("invalid base material");
                    wearableBuilder.baseMaterial(baseMaterial);

                    WearablesRepository.registerWearable(wearableBuilder.build());
                    log.debug("Registering wearable '{}'", wearableId);
                } catch (Exception x) {
                    log.error("Failed to register wearable '{}': {}", wearableId, x.getMessage());
                }
            });
        });
    }

    @EventHandler
    public void handleGamemodeSwitchEvent(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            deleteAllWearablesFromWorld(event.getPlayer());
        } else if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            createAllWearablesInWorld(event.getPlayer());
        }
    }

    @EventHandler
    public void handlePlayerDeathEvent(PlayerDeathEvent event) {
        deleteAllWearablesFromWorld(event.getEntity());
    }

    @EventHandler
    public void handlePlayerRespawnEvent(PlayerRespawnEvent event) {
        createAllWearablesInWorld(event.getPlayer());
    }

    @EventHandler
    public void handleOnJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        Set<String> wearables = loadWearablesFromStorage(player);
        if (!wearables.isEmpty()) {
            for (String wearableId : wearables) {
                final var wearable = WearablesRepository.getWearables().get(wearableId);
                if (wearable != null)
                    this.addWearable(
                            player,
                            wearable
                    );
                else
                    log.warn("Unregistered wearable id '{}' loaded for player '{}'", wearableId, player.getName());
            }
        }
    }

    @EventHandler
    public void handleOnQuit(PlayerQuitEvent event) {
        saveWearablesToStorage(event.getPlayer());
        deleteAllWearablesFromWorld(event.getPlayer());

        this.wardrobe.remove(event.getPlayer().getUniqueId());
    }

}
