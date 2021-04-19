package eu.battleland.revoken.serverside.game.mechanics.wearables;

import eu.battleland.common.Revoken;
import eu.battleland.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.wearables.model.Wearable;
import eu.battleland.revoken.serverside.statics.PktStatics;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Triple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2(topic = "Wearable Mechanic")
public class WearableMechanic extends AMechanic<RevokenPlugin> implements Listener {

    public final NamespacedKey SAVE_KEY;

    @Getter
    private final Map<UUID, Triple<EntityPlayer, Entity, Wearable>> wardrobe = new ConcurrentHashMap<>();

    public WearableMechanic(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);

        SAVE_KEY = new NamespacedKey("battleland", "wearing");
    }

    /**
     * Clothes the player in the finest silk.
     *
     * @param wearable Wearable Object
     * @param player   Player   Object
     * @return boolean result
     */
    public boolean clotheOneselfIn(@NotNull Wearable wearable, @NotNull Player player) {
        final EntityPlayer nmsPlayer = PktStatics.getNmsPlayer(player);
        final WorldServer worldServer = nmsPlayer.getWorldServer();

        final Entity wearableEntity = EntityTypes.ARMOR_STAND.createCreature(
                worldServer, null, null, null,
                PktStatics.getBlockLocation(player), EnumMobSpawn.COMMAND, false, false
        );
        if (wearableEntity == null) {
            log.error("Failed to instance class ARMOR_STAND for Wearable: " + wearable.getIdentifier() + ", Player: " + player.getName());
            return false;
        }
        hideEntities(wearableEntity);

        wearableEntity.setCustomName(IChatBaseComponent.ChatSerializer.b("wearable_" + wearable.getIdentifier()));
        wearableEntity.setSlot(EnumItemSlot.HEAD, wearable.getNativeItemStack());

        player.getPersistentDataContainer()
                .set(SAVE_KEY, PersistentDataType.STRING, wearable.getIdentifier());

        this.wardrobe.compute(player.getUniqueId(), (uuid, data) -> {
            if (data != null)
                clotheOneselfOff(player);

            wearableEntity.startRiding(nmsPlayer);
            worldServer.addEntity(wearableEntity);
            return Triple.of(nmsPlayer, wearableEntity, wearable);
        });

        return true;
    }

    /**
     * Rips and tears down the finest silk the player is surrounded with.
     *
     * @param player Player   Object
     * @return boolean result
     */
    public boolean clotheOneselfOff(@NotNull Player player) {
        final EntityPlayer nmsPlayer = PktStatics.getNmsPlayer(player);
        final WorldServer worldServer = nmsPlayer.getWorldServer();

        player.getPersistentDataContainer()
                .remove(SAVE_KEY);

        var data = this.wardrobe.remove(player.getUniqueId());
        data.getSecond().stopRiding();
        worldServer.removeEntity(data.getSecond());

        return false;
    }


    private void hideEntities(@NotNull Entity... entities) {
        for (Entity entity : entities) {
            entity.persistentInvisibility = false;
            entity.setInvulnerable(true);
            entity.setInvisible(true);

            if (entity instanceof EntityArmorStand) {
                ((EntityArmorStand) entity).setMarker(true);
            }
            entity.setSilent(true);
            entity.setBoundingBox(new AxisAlignedBB(0, 0, 0, 0, 0, 0));

            if (entity instanceof EntityLiving)
                ((EntityLiving) entity).collides = false;

        }
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getCommandMap().register("revoken", new Command("test") {
            @Override
            public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
                Wearable wearable = Wearable.builder()
                        .identifier("backpack")
                        .baseMaterial(Material.POPPY)
                        .modelData(1)
                        .build();

                if (clotheOneselfIn(wearable, (Player) sender))
                    sender.sendMessage("§a!");

                return true;
            }
        });
    }

    @Override
    public void terminate() {
    }

    @Override
    public void reload() {
    }


    @Override
    public void tick() {
        this.wardrobe.forEach((uuid, data) -> {
            var player = data.getFirst();
            var entity = data.getSecond();
            if (player.networkManager.isConnected())
                PktStatics.sendPacketToAll(new PacketPlayOutEntityHeadRotation(entity, (byte) ((int) (player.yaw * 256.0F / 360.0F))));
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // load wearable from registry from persistent player storage
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clotheOneselfOff(event.getPlayer());
    }
}
