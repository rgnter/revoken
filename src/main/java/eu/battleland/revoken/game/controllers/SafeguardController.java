package eu.battleland.revoken.game.controllers;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AController;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;

public class SafeguardController extends AController {

    public SafeguardController(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().addPermission(new Permission("revoken.safeguard", "Provides protection to player from other players", PermissionDefault.FALSE));
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        final Player damager, damagee;
        if(!(event.getDamager() instanceof Player) && !(event.getEntity() instanceof Player))
            return;

        damager = (Player) event.getDamager();
        damagee = (Player) event.getEntity();

        if(damagee.hasPermission("revoken.safeguard") || damager.hasPermission("revoken.safeguard")) {
            event.setCancelled(true);
        }
    }

}
