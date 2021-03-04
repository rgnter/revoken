package eu.battleland.revoken.game.special;

import eu.battleland.revoken.RevokenPlugin;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.entity.EntityDismountEvent;
import xyz.rgnt.mth.Mth;
import xyz.rgnt.mth.tuples.Pair;

import java.util.*;
import java.util.concurrent.Executors;

public class Sitting implements Listener {

    private RevokenPlugin plugin;
    @Getter
    private TreeMap<UUID, Pair<UUID, Location>> entites = new TreeMap<>();

    public Sitting(RevokenPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b != null)
            if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.AIR) && e.getPlayer().getInventory().getItemInOffHand().getType().equals(Material.AIR))
                    if (b.getType().name().contains("_STAIRS"))
                        sitOnStairs(b, e.getPlayer());
    }

    @EventHandler
    public void dismount(EntityDismountEvent e) {
        if (e.getEntity() instanceof Player)
            if (entites.containsKey(e.getDismounted().getUniqueId())) {
                entites.remove(e.getDismounted().getUniqueId());
                e.getDismounted().remove();

                e.getEntity().setVelocity(new Vector(0, .5, 0));
            }

    }

    @EventHandler
    public void blockBreak(BlockBreakEvent e) {
        final Location location = e.getBlock().getLocation().add(new Vector(0, -1, 0));
        // perfection
        Executors.newSingleThreadExecutor().submit(() -> {
            this.entites.forEach((entityUuid, sitData) -> {
                System.out.println(sitData.getSecond());
                if(sitData.getSecond().equals(location))
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        var entity = Bukkit.getEntity(entityUuid);
                        if(entity != null)
                            entity.remove();
                    });
            });
        });

    }
    public void sitOnLocation(@NotNull Location location, @NotNull Player player) {
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(
                location, EntityType.ARMOR_STAND);

        armorStand.setCustomName("revoken_chair");
        armorStand.setCanTick(false);
        armorStand.setSmall(true);
        armorStand.setGravity(false);
        armorStand.addPassenger(player);
        armorStand.setVisible(false);

        var loc = location.toBlockLocation();
        loc.setYaw(0);

        this.entites.put(armorStand.getUniqueId(), Pair.of(player.getUniqueId(), loc));
    }

    public void sitOnStairs(@NotNull Block block, @NotNull Player player) {
        Stairs stairs = (Stairs) block.getBlockData();
        if (stairs.getHalf() == Bisected.Half.BOTTOM) {
            int yawBase = 0;

            BlockFace face = ((Stairs) block.getBlockData()).getFacing();
            Vector direction = face.getDirection();

            Stairs.Shape shape = stairs.getShape();
            boolean isInnerLeft = shape.equals(Stairs.Shape.INNER_LEFT);
            boolean isInnerRight = shape.equals(Stairs.Shape.INNER_RIGHT);
            boolean isInner = isInnerLeft || isInnerRight;

            if (!isInner) {
                // get block faced by the stairs and if the area is not empty; disallow sit
                Block frontBlock = block.getWorld().getBlockAt(block.getLocation().add(direction.multiply(new Vector(-1, -1, -1))));
                if (!frontBlock.getType().equals(Material.AIR))
                    return;
            }

            // get the top block, and if the area isn't empty... cancel
            Block topBlock = block.getWorld().getBlockAt(block.getLocation().add(new Vector(0, 1, 0)));
            if (!topBlock.getType().equals(Material.AIR))
                return;

            // north
            if (direction.getZ() == -1) {
                yawBase = -180;
                if(isInnerRight)
                    yawBase=45;
                if(isInnerLeft)
                    yawBase=-45;
            }
            // south
            else if (direction.getZ() == 1) {
                yawBase = 0;

                if(isInnerRight)
                    yawBase=-135;

                if(isInnerLeft)
                    yawBase=135;
            }
            // east
            else if (direction.getX() == 1) {
                yawBase = -90;
                if(isInnerRight)
                    yawBase=135;
                if(isInnerLeft)
                    yawBase=135;
            }
            //west
            else if (direction.getX() == -1) {
                yawBase = 90;

                if(isInnerLeft)
                    yawBase=-135;
                if(isInnerRight)
                    yawBase=135;
            }
            else
                return;

            Vector center = block.getBoundingBox().getCenter();
            center.setY(center.getY() - 0.95D);

            Location loc = center.toLocation(player.getWorld());
            loc.setYaw(yawBase);
            loc.setPitch(0);

            sitOnLocation(loc, player);
        }

    }

}
