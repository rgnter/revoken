package eu.battleland.revoken.serverside.game;

import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.abstracted.AMngr;
import eu.battleland.revoken.common.diagnostics.timings.Timer;
import eu.battleland.revoken.common.providers.storage.flatfile.data.AuxData;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.AuxCodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.impl.ex.CodecException;
import eu.battleland.revoken.common.providers.storage.flatfile.data.codec.meta.CodecKey;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.SittingMechanic;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.items.ItemsMechanic;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.WearablesMechanic;
import eu.battleland.revoken.serverside.game.mechanics.silkspawner.SilkSpawnersMechanic;
import eu.battleland.revoken.serverside.statics.PktStatics;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.PacketPlayOutGameStateChange;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2(topic = "Revoken - MechanicMngr")
public class MechanicMngr extends AMngr<RevokenPlugin, AMechanic<RevokenPlugin>> implements Listener {

    private final AtomicInteger lastTickableId = new AtomicInteger(0);
    private final ConcurrentHashMap<Integer, Runnable> syncTickables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Runnable> asyncTickables = new ConcurrentHashMap<>();
    private final ExecutorService tickThreadPool;


    @Getter
    private @NotNull Optional<AStore> configuration = Optional.empty();
    @Getter
    private final @NotNull Settings settings = new Settings();

    {
        this.tickThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "Revoken - Tickable Thread");
            }
        });
    }


    /**
     * Default constructor
     *
     * @param plugin Plugin instance
     */
    public MechanicMngr(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    public static void lagClient(Optional<CommandSender> requester, Player player) {
        var nmsPlayer = PktStatics.getNmsPlayer(player);
        Bukkit.getScheduler().runTaskAsynchronously(RevokenPlugin.getInstance(), () -> {
            for (int i = 0; i < 10000; i++) {

                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }

                if (player.isOnline())
                    nmsPlayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.h, i));
                else {
                    requester.ifPresentOrElse((sender) -> {
                        sender.sendMessage("§aBroke them.");
                    }, () -> {
                        log.info("Lagged client {}", player.getName());
                    });
                    break;
                }

            }
        });
    }

    @Override
    public void initialize() {
        settings.setup();

        if (!settings.noMechanics) {
            Timer timer = Timer.timings().start();
            registerComponents(
                    new SittingMechanic(getPlugin()),

                    // game changers
                    new WearablesMechanic(getPlugin()),
                    new ItemsMechanic(getPlugin()),

                    new SilkSpawnersMechanic(getPlugin())
            );

            log.info("Constructing and Initializing Mechanics");
            callForComponents((clazz, instance) -> {
                try {
                    instance.initialize();

                    if (instance.isTickable())
                        registerSyncTickable(instance::tick);
                    if (instance.isTickableAsync())
                        registerAsyncTickable(instance::asyncTick);

                    log.debug("Initialized mechanic '§e{}§r'", clazz.getSimpleName());
                } catch (Exception x) {
                    log.error("Failed to initialize mechanic '{}'", clazz.getSimpleName(), x);
                }
            });
            log.info("Constructed and Initialized Mechanics in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
        }

        ((CraftServer) getPlugin().instance().getServer()).getServer().b(() -> {
            // async tickables
            asyncTickables.forEach((id, tickable) -> {
                this.tickThreadPool.submit(() -> {
                    try {
                        tickable.run();
                    } catch (Exception x) {
                        if (!this.getPlugin().instance().isDebug())
                            log.error("Caught exception while ticking async task #{}({})", id, tickable.getClass().getName(), x);
                    }
                });
            });

            // sync tickables
            syncTickables.forEach((id, tickable) -> {
                var tickTimer = Timer.timings().start();
                try {
                    tickable.run();
                    double tookMs = tickTimer.stop().resultMilli();
                    if (tookMs > 100)
                        log.warn("Ticking sync task #{}({}) took {}ms", id, tickable.getClass().getName(), tookMs);

                } catch (Exception x) {
                    if (!this.getPlugin().instance().isDebug())
                        log.error("Caught exception while ticking sync task #{}({})", id, tickable.getClass().getName(), x);
                }
            });

        });


        {
            Bukkit.getCommandMap().register("revoken", new Command("ride") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                    if (!sender.hasPermission("revoken.ride"))
                        return true;

                    Player player = (Player) sender;
                    var entity = Optional.ofNullable(player.getTargetEntity(10));
                    entity.ifPresentOrElse((target) -> {
                        target.addPassenger(player);

                        player.sendMessage("§aride on!");
                    }, () -> {
                        player.sendMessage("§cfuck off");
                    });

                    return true;
                }
            });

            Bukkit.getCommandMap().register("revoken", new Command("resourcepack") {
                @Override
                public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
                    if (commandSender instanceof Player) {
                        if (((Player) commandSender).hasResourcePack()) {
                            commandSender.sendMessage("§aYou already have resourcepack enabled!");
                            return true;
                        }
                        commandSender.sendMessage("§aAccept resourcepack prompt");
                        ((Player) commandSender).setResourcePack(
                                "http://battleland.eu/file/battleland.zip",
                                "48c9f5f6ab0416d8b05403cac7f12ff1bf767b59"
                        );
                    } else {
                        commandSender.sendMessage("§7http://battleland.eu/file/battleland.zip (48c9f5f6ab0416d8b05403cac7f12ff1bf767b59)");
                        StringBuilder rpStatus = new StringBuilder();
                        Bukkit.getOnlinePlayers().stream().filter(Player::hasResourcePack).map(Player::getName).forEach(playerName -> rpStatus.append(playerName).append(" "));
                        commandSender.sendMessage(rpStatus.toString());
                    }

                    return true;
                }
            });

            Bukkit.getCommandMap().register("revoken", new Command("lagclient") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                    if (!sender.hasPermission("revoken.admin"))
                        return true;

                    if (args.length == 0) {
                        sender.sendMessage("§cMissing player argument");
                        return true;
                    }
                    String playerName = args[0];
                    final Player bukkitPlayer = Bukkit.getPlayer(playerName);
                    if (bukkitPlayer == null) {
                        sender.sendMessage("§cInvalid player reference");
                        return true;
                    }
                    lagClient(Optional.of(sender), bukkitPlayer);

                    return true;
                }
            });
        }
    }

    @Override
    public void terminate() {
        if (!settings.noMechanics) {
            Timer timer = Timer.timings().start();
            log.info("Terminating Mechanics");
            callForComponents((clazz, instance) -> {
                try {
                    instance.terminate();
                    log.debug("Terminated mechanic '§e{}§r'", clazz.getSimpleName());
                } catch (Exception x) {
                    log.error("Failed to terminate mechanic '{}'", clazz.getSimpleName(), x);
                }
            });
            log.info("Constructed and Initialized Mechanics in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
        }
        this.tickThreadPool.shutdown();
    }

    @Override
    public void reload() {
        Timer timer = Timer.timings().start();
        settings.setup();

        if (!settings.noMechanics) {
            log.info("Reloading Mechanics");

            callForComponents((clazz, instance) -> {
                try {
                    instance.reload();
                } catch (Exception x) {
                    log.error("Failed to reload mechanic '§e{}§r'", clazz.getSimpleName(), x);
                }
            });
            log.info("Reloaded Mechanics in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
        }
    }


    /**
     * Registers sync tickable
     *
     * @param runnable Task
     * @return Tickable ID
     */
    public int registerSyncTickable(@NotNull Runnable runnable) {
        this.syncTickables.put(lastTickableId.addAndGet(1), runnable);
        return lastTickableId.get();
    }

    /**
     * Registers sync tickable
     *
     * @param runnable Task
     * @return Tickable ID
     */
    public int registerAsyncTickable(@NotNull Runnable runnable) {
        this.asyncTickables.put(lastTickableId.addAndGet(1), runnable);
        return lastTickableId.get();
    }

    /**
     * Unregisters any tickable with this id
     *
     * @param tickableId Tickable ID
     */
    public void unregisterAnyTickable(int tickableId) {
        this.syncTickables.remove(tickableId);
        this.asyncTickables.remove(tickableId);
    }

    private class Settings implements ICodec {
        @CodecKey("resourcepack.force")
        private Boolean forceResourcepack = false;
        @CodecKey("resourcepack.url")
        private String resourcePackUrl = "";
        @CodecKey("resourcepack.hash")
        private String resourcePackHash = "";

        @CodecKey("mechanics.no-mechanics")
        private boolean noMechanics;

        protected void setup() {
            configuration.or(() -> {
                try {
                    configuration = Optional.of(getPlugin().instance().getStorageProvider()
                            .provideYaml("resources", "configs/mechanics/mechanics_config.yaml", true));
                } catch (Exception e) {
                    log.error("Failed to provide configuration", e);
                }
                return configuration;
            }).ifPresent((config) -> {
                try {
                    config.prepare();
                } catch (Exception e) {
                    log.error("Failed to prepare configuration", e);
                }

                try {
                    config.getData().decode(this);
                } catch (Exception e) {
                    log.error("Failed to decode settings: {}", e.getMessage());
                }

            });
        }

        @Override
        public Class<?> type() {
            return Settings.class;
        }

        @Override
        public ICodec instance() {
            return new Settings();
        }
    }
}
