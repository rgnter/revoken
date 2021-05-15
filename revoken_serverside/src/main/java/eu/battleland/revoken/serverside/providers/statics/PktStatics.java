package eu.battleland.revoken.serverside.providers.statics;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.CraftServer;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class PktStatics {

    /**
     * Get nms player from Bukkit player
     *
     * @param player Bukkit player
     * @return Nms Player
     */
    public static @NotNull EntityPlayer getNmsPlayer(@NotNull Player player) {
        return ((CraftPlayer) player).getHandle();
    }

    /**
     * Get nms player from Bukkit player
     *
     * @param player UUID of player
     * @return Nms Player
     */
    public static @Nullable EntityPlayer getNmsPlayer(@NotNull UUID player) {
        return getNmsServer().getPlayerList().getPlayer(player);
    }

    /**
     * Converts Bukkit Location to NMS Location
     *
     * @param location Location
     * @return Block location
     */
    public static @NotNull BlockPosition toNmsBlockPosition(@NotNull Location location) {
        return new BlockPosition(CraftVector.toNMS(location.toBlockLocation().toVector()));
    }

    /**
     * Sends packet to all players
     *
     * @param pkt Packet to send
     */
    public static void sendPacketToAll(@NotNull Packet<PacketListenerPlayOut> pkt) {
        sendPacketToAll(pkt, Collections.emptyList());
    }

    /**
     * Sends packet to all players with filter
     *
     * @param pkt    Packet to send
     * @param except Vararg players to filter
     */
    public static void sendPacketToAll(@NotNull Packet<PacketListenerPlayOut> pkt, @NotNull UUID... except) {
        sendPacketToAll(pkt, Arrays.asList(except));
    }

    /**
     * Sends packet to all players with filter
     *
     * @param pkt    Packet to send
     * @param except List of players to filter
     */
    public static void sendPacketToAll(@NotNull Packet<PacketListenerPlayOut> pkt, @NotNull List<UUID> except) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> !except.contains(player.getUniqueId()))
                .map(PktStatics::getNmsPlayer)
                .forEach(player -> {
                    player.playerConnection.sendPacket(pkt);
                });
    }

    /**
     * Creates particle packet
     *
     * @param particle        Particle type from net.minecraft.server.v1_16_R1.Particles
     * @param overrideLimiter Override limiter
     * @param x               World pos X
     * @param y               World pos Y
     * @param z               World pos Z
     * @param offsetX         Offset of X
     * @param offsetY         Offset of Y
     * @param offsetZ         Offset of Z
     * @param speed           Speed of particle
     * @param count           Count of particles
     * @return packet
     */
    public static PacketPlayOutWorldParticles makeParticlePacket(@NotNull ParticleType particle, boolean overrideLimiter, double x, double y, double z, float offsetX, float offsetY, float offsetZ, float speed, int count) {
        return new PacketPlayOutWorldParticles(particle, overrideLimiter, x, y, z, offsetX, offsetY, offsetZ, speed, count);
    }

    /**
     * Creates particle packet
     *
     * @param particle        Particle type from net.minecraft.server.v1_16_R1.Particles
     * @param overrideLimiter Override limiter
     * @param loc             World pos vector
     * @param offset          Offset vector
     * @param speed           Speed of particle
     * @param count           Count of particles
     * @return packet
     */
    public static PacketPlayOutWorldParticles makeParticlePacket(@NotNull ParticleType particle, boolean overrideLimiter, Vector loc, @NotNull Vector offset, float speed, int count) {
        return makeParticlePacket(particle, overrideLimiter, loc.getX(), loc.getY(), loc.getZ(),
                (float) offset.getX(), (float) offset.getY(), (float) offset.getZ(), speed, count);
    }


    /**
     * @param world Bukkit World
     * @return NMS World
     */
    public static @NotNull WorldServer getNmsWorldServer(@NotNull org.bukkit.World world) {
        return ((CraftWorld) world).getHandle();
    }

    /**
     * @param player Bukkit player
     * @return NMS World
     */
    public static @NotNull WorldServer getNmsWorldServer(@NotNull Player player) {
        return getNmsWorldServer(player.getWorld());
    }

    /**
     * @param player Bukkit player
     * @return NMS World
     */
    public static @NotNull BlockPosition getBlockLocation(@NotNull Player player) {
        return new BlockPosition(CraftVector.toNMS(player.getLocation().toVector()));
    }

    /**
     * @return Server
     */
    public static MinecraftServer getNmsServer() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static @NotNull PlayerChunkMap getPlayerChunkMap(@NotNull World world) {
        return ((WorldServer) world).getChunkProvider().playerChunkMap;
    }

    /**
     * @param player       Player
     * @param playerToHide Player that will be hidden to player.
     */
    public static void untrackPlayerFor(@NotNull EntityPlayer player, @NotNull EntityPlayer playerToHide) {
        PlayerChunkMap playerChunkMap = ((WorldServer) player.world).getChunkProvider().playerChunkMap;
        PlayerChunkMap.EntityTracker tracker = playerChunkMap.trackedEntities.get(player.getId());
        if (tracker != null)
            tracker.clear(playerToHide);
    }

    /**
     * @param player       Player
     * @param playerToShow Player that will be shown to player.
     */
    public static void trackPlayerFor(@NotNull EntityPlayer player, @NotNull EntityPlayer playerToShow) {
        PlayerChunkMap playerChunkMap = ((WorldServer) player.world).getChunkProvider().playerChunkMap;
        PlayerChunkMap.EntityTracker tracker = playerChunkMap.trackedEntities.get(player.getId());
        if (tracker != null)
            tracker.updatePlayer(playerToShow);
    }

    /**
     * @param player Player
     * @param entity Entity that will share tracker with player
     */
    public static void synchronizeTracker(@NotNull EntityPlayer player, @NotNull Entity entity) {
        PlayerChunkMap playerChunkMap = ((WorldServer) player.world).getChunkProvider().playerChunkMap;
        PlayerChunkMap.EntityTracker tracker = playerChunkMap.trackedEntities.get(player.getId());
        playerChunkMap.trackedEntities.put(entity.getId(), tracker);

    }

    public static @NotNull EntityPlayer createEntityPlayerCopy(@NotNull EntityPlayer original) {
        EntityPlayer cpy = new EntityPlayer(getNmsServer(), original.getWorldServer(), original.getProfile(), new PlayerInteractManager(original.getWorldServer()));
        cpy.copyFrom(original, false);
        cpy.e(original.getId());
        cpy.a(original.getMainHand());

        return cpy;
    }

    public static PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook relMoveLook(int id, @NotNull EntityPlayer player) {
        return new PacketPlayOutEntity.PacketPlayOutRelEntityMoveLook(id,
                (short) ((player.locX() * 32 - player.lastX * 32) * 128),
                (short) ((player.locY() * 32 - player.lastY * 32) * 128),
                (short) ((player.locZ() * 32 - player.lastZ * 32) * 128),
                (byte) ((int) (player.yaw * 256.0F / 360.0F)), (byte) 0, player.isOnGround());
    }


    public static void makeEntitiesDummmies(@NotNull Entity... entities) {
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
}
