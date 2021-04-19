package eu.battleland.revoken.serverside.mcdev;

import net.minecraft.server.v1_16_R3.*;

import java.util.Collections;

public class OverridenPlayerConnection extends PlayerConnection {

    public EntityPlayer other;

    public OverridenPlayerConnection(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);
    }

    @Override
    public void a(PacketPlayInFlying packet) {
        super.a(packet);

        if(other != null) {
            other.yaw   = player.yaw;
            other.pitch = player.pitch;

        }
    }


    @Override
    public void sendPacket(Packet<?> packet) {
        super.sendPacket(packet);
        System.out.println(packet.getClass().getSimpleName());
    }

}
