package eu.battleland.revoken;

import eu.battleland.revoken.diagnostics.timings.Timer;
import eu.battleland.revoken.game.ControllerMngr;
import eu.battleland.revoken.game.MechanicMngr;
import eu.battleland.revoken.game.controllers.ChatController;
import eu.battleland.revoken.game.controllers.InterfaceController;
import eu.battleland.revoken.providers.storage.flatfile.StorageProvider;
import eu.battleland.revoken.providers.storage.flatfile.store.AStore;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2(topic = "Revoken")
public class RevokenPlugin extends JavaPlugin {

    @Getter @Setter
    private boolean debug = true;

    @Getter
    private StorageProvider storageProvider;

    @Getter
    private ControllerMngr controllerMngr;
    @Getter
    private MechanicMngr   mechanicMngr;

    @Override
    public void onLoad() {
        super.onLoad();

        Timer timer = Timer.timings().start();
        log.info("Constructing Revoken plugin...");
        {
            this.storageProvider = new StorageProvider(this);

            this.controllerMngr  = new ControllerMngr(this);
            this.mechanicMngr    = new MechanicMngr(this);
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

            Bukkit.getServer().getCommandMap().register("revoken", new Command("revoken") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if(!sender.hasPermission("revoken.admin"))
                        return true;

                    if(args.length == 0)
                        return true;
                    if(args[0].equalsIgnoreCase("reload")) {
                        controllerMngr.reload();
                        sender.sendMessage("§aReloaded.");
                    }
                    else if(args[0].equalsIgnoreCase("debug")) {
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
                    if(!sender.hasPermission("revoken.admin"))
                        return Collections.emptyList();

                    if(args.length == 1)
                        return Arrays.asList("reload", "debug");

                    return Collections.emptyList();
                }
            });

            Bukkit.getServer().getCommandMap().register("revoken", new Command("test") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    if(!sender.hasPermission("revoken.test"))
                        return true;

                    Player player = (Player) sender;
                    EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                    nmsPlayer.setMot(new Vec3D(Math.random() * 1.5, Math.random() * 1.5, Math.random() * 1.5));

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
}
