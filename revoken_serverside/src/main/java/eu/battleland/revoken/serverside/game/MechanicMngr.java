package eu.battleland.revoken.serverside.game;

import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.common.abstracted.AMngr;
import eu.battleland.revoken.common.diagnostics.timings.Timer;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.SittingMechanic;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.items.ItemsMechanic;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.WearableMechanic;
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
    private final @NotNull MechanicMngr.Settings settings;
    @Getter
    private @NotNull Optional<AStore> configuration = Optional.empty();
    {
        this.tickThreadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "Revoken - Tickable Thread");
            }
        });
        this.settings   = new Settings();
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
        settings.setupConfig();

        if(!settings.noMechanics) {
            Timer timer = Timer.timings().start();
            registerComponents(
                    new SittingMechanic(getPlugin()),

                    // game changers
                    new WearableMechanic(getPlugin()),
                    new ItemsMechanic(getPlugin())
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
        if(!settings.noMechanics){
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

        if(!settings.noMechanics) {
            log.info("Reloading Mechanics");
            settings.setupConfig();
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

    private class Settings {
        private boolean noMechanics;

        protected void setupConfig() {
            try {
                if (configuration.isEmpty())
                    configuration = Optional.of(getPlugin().instance().getStorageProvider()
                            .provideYaml("resources", "configs/plugin_config.yaml", true));
            } catch (Exception x) {
                log.error("Failed to provide mechanic config", x);
            }
            configuration.ifPresent(config -> {
                var data = config.getData();
                this.noMechanics = data.getBool("no-mechanics", false);
            });
        }
    }
}
