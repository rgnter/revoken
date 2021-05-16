package eu.battleland.revoken.serverside;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.diagnostics.timings.Timer;
import eu.battleland.revoken.common.providers.storage.flatfile.StorageProvider;
import eu.battleland.revoken.common.providers.storage.data.codec.ICodec;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.game.ControllerMngr;
import eu.battleland.revoken.serverside.game.MechanicMngr;
import eu.battleland.revoken.serverside.mcdev.OverridenPlayerConnection;
import eu.battleland.revoken.serverside.providers.statics.PktStatics;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2(topic = "Revoken")
public class RevokenPlugin extends JavaPlugin implements Revoken<RevokenPlugin>, Listener {

    @Getter
    public static RevokenPlugin instance;

   // @Getter
  //  public static Optional<PaperCommandManager<CommandSender>> commandManager = Optional.empty();

    /**
     * Providers
     */
    @Getter
    private StorageProvider storageProvider;

    /**
     * Managers
     */
    @Getter
    private ControllerMngr controllerMngr;
    @Getter
    private MechanicMngr mechanicMngr;


    @Getter
    @Setter
    private boolean debug = false;

    @Getter
    private @NotNull Settings settings = new Settings();
    @Getter
    private @NotNull Optional<AStore> pluginConfig = Optional.empty();

    public RevokenPlugin() {

    }

    @Override
    public void onLoad() {
        super.onLoad();
        instance = this;

        Timer timer = Timer.timings().start();
        log.info("Constructing Revoken plugin...");
        {
            this.storageProvider = new StorageProvider(this);
            this.settings.setup();

            this.controllerMngr = new ControllerMngr(this);
            this.mechanicMngr = new MechanicMngr(this);
        }
        log.info("Constructed Revoken plugin in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
        /*try {
            commandManager = Optional.of(new PaperCommandManager<>(
                    instance,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    Function.identity(),
                    Function.identity()
            ));
        } catch (Exception e) {
            log.error("Failed to make CommandManager", e);
        }
        commandManager.ifPresent((manager) -> {
            if (manager.queryCapability(CloudBukkitCapabilities.BRIGADIER))
                manager.registerBrigadier();
            if (manager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION))
                manager.registerAsynchronousCompletions();
        });*/

        Timer timer = Timer.timings().start();
        log.info("Initializing Revoken plugin...");
        {
            this.controllerMngr.initialize();
            this.mechanicMngr.initialize();

            Bukkit.getServer().getCommandMap().register("revoken", new Command("revoken") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if (!sender.hasPermission("revoken.admin"))
                        return true;

                    if (args.length == 0)
                        return true;
                    if (args[0].equalsIgnoreCase("reload")) {
                        settings.setup();
                        controllerMngr.reload();
                        mechanicMngr.reload();
                        sender.sendMessage("§aReloaded plugin/controllers/mechanics.");
                    } else if (args[0].equalsIgnoreCase("debug")) {
                        sender.sendMessage((debug = !debug) ? "§aDebug is now enabled" : "§cDebug is now disabled");
                    } else {
                        sender.sendMessage("§c?");
                    }
                    return true;
                }

                @NotNull
                @Override
                public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                    if (!sender.hasPermission("revoken.admin"))
                        return Collections.emptyList();

                    if (args.length == 1)
                        return Arrays.asList("reload", "debug");

                    return Collections.emptyList();
                }
            });
        }
        log.info("Initialized Revoken plugin in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }

    @Override
    public void onDisable() {
        super.onDisable();


        Timer timer = Timer.timings().start();
        log.info("Terminating Revoken plugin...");
        {
            this.mechanicMngr.terminate();
            this.controllerMngr.terminate();
        }
        log.info("Terminating Revoken plugin in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }


    class Settings implements ICodec {

        private void setup() {
            RevokenPlugin.this.pluginConfig.or(() -> {
                try {
                    RevokenPlugin.this.pluginConfig = Optional.of(RevokenPlugin.this.getStorageProvider().provideYaml("resources", "configs/plugin_config.yaml", true));
                    return RevokenPlugin.this.pluginConfig;
                } catch (Exception e) {
                    log.error("Couldn't provide config", e);
                }
                return Optional.empty();
            }).ifPresent((config) -> {
                try {
                    config.prepare();
                } catch (Exception e) {
                    log.error("Couldn't prepare config", e);
                }

                final var data = config.getData();
                log.info("Loaded settings");
            });
        }
    }


    @Override
    public RevokenPlugin instance() {
        return this;
    }
}
