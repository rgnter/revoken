package eu.battleland.revoken.serverside.game.mechanics.sexyborder;

import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class SexyBorder extends AMechanic<RevokenPlugin> implements Listener {

    public SexyBorder(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize() {
        Bukkit.getPluginManager().registerEvents(this, this.getPlugin().instance());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().teleport(new Location(event.getPlayer().getWorld(), -468, 80, 486));
        for (int i = 0; i < 360; i++) {
            Vector pos = new Vector(40 * Math.cos(Math.toRadians(i)), 80, 40 * Math.sin(Math.toRadians(i))).add(new Vector(-468, 80, 486));
            event.getPlayer().getWorld().getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ()).setType(Material.RED_CONCRETE);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Vector centerVec = new Vector(-468, 80, 486);
        Vector playerVec = event.getTo().toVector();

        // distance on X and Z
        if (Math.sqrt(NumberConversions.square(centerVec.getX() - playerVec.getX())
                + NumberConversions.square(centerVec.getZ() - playerVec.getZ())) > 40) {
            System.out.println("Outside!");


            Vector direction = centerVec.subtract(playerVec.midpoint(centerVec)).normalize(); //není to stejne jako u me?
            System.out.println(direction);
            event.getPlayer().setVelocity(direction.multiply(2).setY(0.1));
        }
    }
}
