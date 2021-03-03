package eu.battleland.revoken;

import eu.battleland.revoken.diagnostics.timings.Timer;
import eu.battleland.revoken.game.ControllerMngr;
import eu.battleland.revoken.game.controllers.ChatController;
import eu.battleland.revoken.game.controllers.InterfaceController;
import eu.battleland.revoken.providers.storage.flatfile.StorageProvider;
import eu.battleland.revoken.providers.storage.flatfile.store.AStore;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IBlockAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Log4j2(topic = "Revoken")
public class RevokenPlugin extends JavaPlugin {


    @Getter
    private StorageProvider storageProvider;

    @Getter
    private ControllerMngr controllerMngr;

    @Override
    public void onLoad() {
        super.onLoad();

        Timer timer = Timer.timings().start();
        log.info("Constructing Revoken plugin...");
        {
            this.storageProvider = new StorageProvider(this);
            this.controllerMngr = new ControllerMngr(this);

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
                    return true;
                }

                @NotNull
                @Override
                public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
                    if(!sender.hasPermission("revoken.admin"))
                        return Collections.emptyList();

                    if(args.length == 1) {
                        return Arrays.asList("reload", "maintenance", "special");
                    }
                    return Collections.emptyList();
                }
            });

            Bukkit.getServer().getCommandMap().register("revoken", new Command("test") {
                @Override
                public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
                    Player player = (Player) sender;
                    EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();


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
            this.controllerMngr.terminate();

        }
        log.info("Terminating Revoken plugin in §e{}§rms", String.format("%.3f", timer.stop().resultMilli()));
    }
}
