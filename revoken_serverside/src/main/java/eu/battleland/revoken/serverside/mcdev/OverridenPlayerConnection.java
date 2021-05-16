package eu.battleland.revoken.serverside.mcdev;

import eu.battleland.revoken.common.abstracted.AMechanic;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.WearablesAPI;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.WearablesMechanic;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.model.Wearable;
import eu.battleland.revoken.serverside.game.mechanics.gamechanger.wearables.nms.WearableEntity;
import net.minecraft.server.v1_16_R3.*;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Pair;

import java.util.Map;

public class OverridenPlayerConnection extends PlayerConnection {

    private final EntityPlayer player;
    private final RevokenPlugin revokenPlugin;


    public OverridenPlayerConnection(@NotNull RevokenPlugin plugin, MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);
        this.player = entityplayer;

        this.revokenPlugin = plugin;
    }

    @Override
    public void a(PacketPlayInFlying packet) {
        super.a(packet);

    }


    @Override
    public void sendPacket(Packet<?> packet) {
        super.sendPacket(packet);
    }

}
