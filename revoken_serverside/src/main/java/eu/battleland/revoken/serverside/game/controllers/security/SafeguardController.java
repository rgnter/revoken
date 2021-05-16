package eu.battleland.revoken.serverside.game.controllers.security;

import com.mojang.brigadier.arguments.StringArgumentType;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.serverside.RevokenPlugin;
import net.minecraft.server.v1_16_R3.CommandDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SafeguardController extends AController<RevokenPlugin> {

    /**
     * Player UUID as Key
     * Long as a Value. Long is UNIX timestamp representing safeguard expiry
     */
    private final Map<UUID, Long> safeguarded = new ConcurrentHashMap<>();


    public SafeguardController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().addPermission(
                new Permission("revoken.safeguard", "Provides protection to player from other players", PermissionDefault.FALSE)
        );

        final var commandDispatch =
                ((CraftServer) Bukkit.getServer()).getServer().getCommandDispatcher().a();

        commandDispatch.register(CommandDispatcher.a("test")
                .then(CommandDispatcher.a("test", StringArgumentType.greedyString()).executes(command -> {
            String arg = command.getArgument("test", String.class);
            command.getSource().getBukkitEntity().sendMessage(arg);
            return 1;
        })));

    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) && !(event.getEntity() instanceof Player))
            return;
        final Player damager = (Player) event.getDamager();
        final Player damagee = (Player) event.getEntity();
        if (damager.isOp())
            return;


        if (damagee.hasPermission("revoken.safeguard") || damager.hasPermission("revoken.safeguard")) {
            event.setCancelled(true);
        }
    }


}
