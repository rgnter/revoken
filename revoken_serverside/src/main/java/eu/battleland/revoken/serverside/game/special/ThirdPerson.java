package eu.battleland.revoken.serverside.game.special;

import eu.battleland.revoken.serverside.statics.PktStatics;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Pair;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ThirdPerson {

    public ConcurrentHashMap<UUID, Pair<Location, Location>> players = new ConcurrentHashMap<>();

    public void spectateLocation(@NotNull Player player, @NotNull Location bodyLocation, @NotNull Location cameraLocation) {
        EntityPlayer original = PktStatics.getNmsPlayer(player);
        EntityPlayer copy = PktStatics.createEntityPlayerCopy(original);
        copy.setLocation(bodyLocation.getX(), bodyLocation.getY(), bodyLocation.getZ(), bodyLocation.getYaw(), bodyLocation.getPitch());
        PacketPlayOutSpawnEntityLiving pkt = new PacketPlayOutSpawnEntityLiving(copy);
        original.playerConnection.sendPacket(pkt);
    }

}
