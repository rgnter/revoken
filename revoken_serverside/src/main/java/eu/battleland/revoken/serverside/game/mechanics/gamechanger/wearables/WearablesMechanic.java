package eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables;


import com.google.common.io.ByteStreams;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.model.Wearable;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.nms.WearableEntity;
import eu.battleland.revoken.serverside.providers.statics.PktStatics;
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
import xyz.rgnt.mth.tuples.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides in-game implementation for Wearables API
 */
@Log4j2(topic = "Wearable Mechanic")
public class WearablesMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public final NamespacedKey WEARABLES_DATA_KEY = new NamespacedKey("battleland", "wearables");

    @Getter
    private final WearablesAPI api;
    protected final Map<UUID, @Nullable Map<String, Pair<WearableEntity, Wearable>>> wardrobe = new ConcurrentHashMap<>();

    @Getter
    private @NotNull Optional<AStore> configuration = Optional.empty();


    /**
     * Default constructor
     *
     * @param plugin Revoken plugin
     */
    public WearablesMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
        this.api = new WearablesAPI(this);
    }

    protected @NotNull WearableEntity createWearableEntity(@NotNull Player player, @NotNull Wearable wearable) {
        final EntityPlayer nmsPlayer = PktStatics.getNmsPlayer(player);
        final WorldServer worldServer = nmsPlayer.getWorldServer();
        final BlockPosition position = PktStatics.getBlockLocation(player);

        final WearableEntity wearableEntity =
                new WearableEntity(nmsPlayer, worldServer, position.getX(), position.getY(), position.getZ());
        PktStatics.makeEntitiesDummmies(wearableEntity);

        wearableEntity.setCustomName(IChatBaseComponent.ChatSerializer.b("wearable_" + wearable.getIdentifier()));
        wearableEntity.setSlot(EnumItemSlot.HEAD, wearable.getNativeItemStack());
        wearableEntity.persist = false;

        return wearableEntity;
    }

    /**
     * Creates wearable in world
     * @param wearableData Wearable data
     * @return Returns true if entity has been successfully added to the world, otherwise returns false
     */
    protected boolean createWearableInWorld(@NotNull Pair<WearableEntity, Wearable> wearableData) {
        final var wearableEntity = wearableData.getFirst();
        final var worldServer = wearableEntity.getWorld().getMinecraftWorld();

        if(!worldServer.addEntity(wearableEntity)) {
            log.error("Failed to add entity of wearable '{}'(entity id: {}) to world '{}' worn by '{}'",
                    wearableData.getSecond().getIdentifier(),
                    wearableEntity.getId(),
                    worldServer.getWorld().getName(),
                    wearableEntity.getOwner().getName()
            );
            return false;
        }

        // ride player, force true
        if(!wearableEntity.a((Entity) wearableEntity.getOwner(), true)) {
            log.error("Entity of wearable '{}' failed to ride owner '{}'(entity id: {}) to world '{}'",
                    wearableData.getSecond().getIdentifier(),
                    wearableEntity.getOwner().getName(),
                    wearableEntity.getId(),
                    worldServer.getWorld().getName()
            );
            return false;
        }
        return true;
    }


    /**
     * Deletes wearable from world
     * @param wearableData Wearable data
     * @return Returns false only when: wearable data is null, or entity couldn't be removed from world.
     */
    protected boolean deleteWearableFromWorld(@Nullable Pair<WearableEntity, Wearable> wearableData) {
        if(wearableData == null)
            return false;

        final var entity = wearableData.getFirst();
        final var wearable = wearableData.getSecond();
        final var worldServer = entity.getWorld().getMinecraftWorld();
        entity.stopRiding(true);

        try {
            worldServer.removeEntity(entity);
            return true;
        } catch (Exception exception) {
            final var location = entity.getPositionVector();
            log.error("Failed to remove entity of wearable '{}'(entity id: {}, entity location: {} {} {}, {}) from world '{}' worn by '{}'",
                    wearable.getIdentifier(),
                    entity.getId(),
                    location.getX(),
                    location.getY(),
                    location.getZ(),
                    entity.getWorld().getWorld().getName(),
                    worldServer.getWorld().getName(),
                    entity.getOwner().getName(),
                    exception
            );
            return false;
        }
    }

    /**
     * Creates all wearables of player in world
     * @param owner Player
     */
    protected void createAllWearablesInWorld(@NotNull Player owner) {
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
    protected void deleteAllWearablesFromWorld(@NotNull Player player) {
        var wearables = this.wardrobe.get(player.getUniqueId());
        if (wearables != null)
            wearables.forEach((wearableId, wearableData) ->
                    deleteWearableFromWorld(wearableData));

    }

    protected @NotNull Set<String> loadWearablesFromStorage(@NotNull Player player) {
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

    protected void saveWearablesToStorage(@NotNull Player player) {
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

    protected void wipeWearablesInStorage(@NotNull Player player) {
        player.getPersistentDataContainer().remove(WEARABLES_DATA_KEY);
    }


    @Override
    public void initialize() throws Exception {
        setupConfig();
        setupRepo();

        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
        Bukkit.getCommandMap().register("revoken", new Command("wearable") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
                if(!sender.hasPermission("revoken.admin")) {
                    return true;
                }

                final String action;
                if(args.length < 1) {
                    sender.sendMessage("§cSpecify action: add/remove/set/info");
                    return true;
                }
                action = args[0];

                if(action.equalsIgnoreCase("info")) {
                    if(args.length < 2) {
                        sender.sendMessage("§cSpecify wearable id");
                        return true;
                    }

                    final String wearableId = args[1];
                    final Wearable wearable = WearablesRepository.getWearable(wearableId);
                    if(wearable == null) {
                        sender.sendMessage("§cInvalid wearable identifier");
                        return true;
                    }

                    sender.sendMessage("Wearable identifier: " + wearable.getIdentifier());
                    sender.sendMessage("Wearable base material: " + wearable.getBaseMaterial().name());
                    sender.sendMessage("Wearable model data: " + wearable.getModelData());


                    return true;
                } else {
                    if (args.length < 2) {
                        sender.sendMessage("§cSpecify player name");
                        return true;
                    }
                    final String playerName = args[1];
                    final Player player = Bukkit.getPlayer(playerName);
                    if (args.length < 3) {
                        sender.sendMessage("§cSpecify wearable identifier");
                        return true;
                    }
                    if (player == null) {
                        sender.sendMessage("§cInvalid player");
                        return true;
                    }

                    final String wearableId = args[2];
                    final Wearable wearable = WearablesRepository.getWearable(wearableId);
                    if (wearable == null) {
                        sender.sendMessage("§cInvalid wearable identifier");
                        return true;
                    }

                    if (action.equalsIgnoreCase("add")) {
                        api.addWearable(player, wearable);
                        sender.sendMessage("§aAdded wearable " + wearableId + " to player " + playerName);
                        return true;
                    } else if (action.equalsIgnoreCase("rem") || action.equalsIgnoreCase("remove")) {
                        api.remWearable(player, wearable);
                        sender.sendMessage("§aRemoved wearable " + wearableId + " from player " + playerName);
                        return true;
                    } else if (action.equalsIgnoreCase("set")) {
                        api.remAllWearables(player);
                        api.addWearable(player, wearable);
                        sender.sendMessage("§aSet wearable " + wearableId + " to player " + playerName);
                        return true;
                    }
                }
                return true;
            }

            @Override
            public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                if(args.length > 1) {
                    if(args[0].equalsIgnoreCase("info") || args.length == 3)
                        return new ArrayList<>(WearablesRepository.getWearables().keySet());
                    if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove") ||
                            args[0].equalsIgnoreCase("rem") || args[0].equalsIgnoreCase("set"))
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    return Collections.emptyList();
                }

                return Arrays.asList("add", "rem", "remove", "set");
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

    /**
     * Sets up configuration for this mechanic
     */
    private void setupConfig() {
        try {
            if (configuration.isEmpty())
                configuration = Optional.of(getPlugin().instance().getStorageProvider()
                        .provideYaml("resources", "configs/mechanics/gamechanger/wearables.yaml", true));
        } catch (Exception x) {
            log.error("Failed to provide mechanic config", x);
        }
    }

    /**
     * Sets up wearables repository
     */
    private void setupRepo() {
        configuration.ifPresentOrElse(config -> {
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
        }, () -> {
            log.error("Werables configuration is not present! Can't load wearables repository!");
        });
    }

    @EventHandler
    public void handleGamemodeSwitchEvent(PlayerGameModeChangeEvent event) {
        if(!this.api.hasWearable(event.getPlayer()))
            return;

        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            deleteAllWearablesFromWorld(event.getPlayer());
        } else if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            createAllWearablesInWorld(event.getPlayer());
        }
    }

    @EventHandler
    public void handlePlayerDeathEvent(PlayerDeathEvent event) {
        if(this.api.hasWearable(event.getEntity()))
            this.deleteAllWearablesFromWorld(event.getEntity());
    }

    @EventHandler
    public void handlePlayerRespawnEvent(PlayerRespawnEvent event) {
        if(this.api.hasWearable(event.getPlayer()))
            this.createAllWearablesInWorld(event.getPlayer());
    }

    @EventHandler
    public void handleOnJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final Set<String> wearables = loadWearablesFromStorage(player);
        if (wearables.isEmpty())
            return;

        for (String wearableId : wearables) {
            final var wearable = WearablesRepository.getWearables().get(wearableId);
            if (wearable != null) {
                if(!this.api.addWearable(player, wearable))
                    log.warn("Couldn't add stored stored wearable with id '{}' to player '{}'", wearableId, player.getName());
            }
            else
                log.warn("Unregistered wearable id '{}' found for player '{}'", wearableId, player.getName());
        }

    }

    @EventHandler
    public void handleOnQuit(PlayerQuitEvent event) {
        if(this.api.hasWearable(event.getPlayer())) {
            saveWearablesToStorage(event.getPlayer());
            deleteAllWearablesFromWorld(event.getPlayer());

            this.wardrobe.remove(event.getPlayer().getUniqueId());
        }
    }

}
