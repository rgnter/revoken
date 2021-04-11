package eu.battleland.revoken.serverside;

import eu.battleland.common.Revoken;
import eu.battleland.common.diagnostics.timings.Timer;
import eu.battleland.revoken.serverside.game.ControllerMngr;
import eu.battleland.revoken.serverside.game.MechanicMngr;
import eu.battleland.revoken.serverside.game.special.ThirdPerson;
import eu.battleland.common.providers.storage.flatfile.StorageProvider;
import eu.battleland.common.providers.storage.flatfile.store.AStore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2(topic = "Revoken")
public class RevokenPlugin extends JavaPlugin implements Revoken<RevokenPlugin> {

    @Getter
    @Setter
    private boolean debug = true;

    @Getter
    private @NotNull Optional<AStore> globalConfig = Optional.empty();

    @Getter
    private StorageProvider storageProvider;

    @Getter
    private ControllerMngr controllerMngr;
    @Getter
    private MechanicMngr mechanicMngr;

    @Override
    public void onLoad() {
        super.onLoad();

        Timer timer = Timer.timings().start();
        log.info("Constructing Revoken plugin...");
        {
            this.storageProvider = new StorageProvider(this);
            loadConfiguration();

            this.controllerMngr = new ControllerMngr(this);
            this.mechanicMngr = new MechanicMngr(this);
        }
        log.info("Constructed Revoken plugin in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }

    @Override
    public void onEnable() {
        super.onEnable();

        Timer timer = Timer.timings().start();
        log.info("Initializing Revoken plugin...");
        {
            this.controllerMngr.initialize();
            this.mechanicMngr.initialize();

            Bukkit.getServer().getCommandMap().register("eu/battleland/revoken", new Command("eu/battleland/revoken") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if (!sender.hasPermission("revoken.admin"))
                        return true;

                    if (args.length == 0)
                        return true;
                    if (args[0].equalsIgnoreCase("reload")) {
                        globalConfig.ifPresentOrElse((config) -> {
                            try {
                                config.prepare();
                            } catch (Exception e) {
                                sender.sendMessage("§cFailed to reload global configuration.");
                                log.error("Failed to reload global configuration", e);
                            }
                        }, () -> {
                            loadConfiguration();
                        });
                        controllerMngr.reload();
                        sender.sendMessage("§aReloaded.");
                    } else if (args[0].equalsIgnoreCase("debug")) {
                        debug = !debug;
                        sender.sendMessage(debug ? "§aDebug is now enabled" : "§cDebug is now disabled");
                    } else {
                        sender.sendMessage("§cI have no idea what you are up to, but I can't judge. rtrd.");
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

            Bukkit.getServer().getCommandMap().register("eu/battleland/revoken", new Command("test") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if (!sender.hasPermission("revoken.test"))
                        return true;

                    Player player = (Player) sender;
                    ThirdPerson thirdPerson = new ThirdPerson();
                    thirdPerson.spectateLocation(player, player.getLocation(), player.getLocation());


                    sender.sendMessage("§atest");
                    return true;
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

    private void loadConfiguration() {
        try {
            if(this.globalConfig.isEmpty())
                this.globalConfig = Optional.of(this.getStorageProvider().provideYaml("resources", "configs/_global.yaml", true));
        } catch (Exception x) {
            log.error("Failed to provide default config", x);
        }
    }

    @Override
    public RevokenPlugin instance() {
        return this;
    }
}
