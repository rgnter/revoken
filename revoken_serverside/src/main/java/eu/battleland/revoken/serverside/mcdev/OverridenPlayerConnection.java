package eu.battleland.revoken.serverside.mcdev;

import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.game.controllers.security.AdminController;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.game.PacketPlayInFlying;
import net.minecraft.network.protocol.game.PacketPlayInTabComplete;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.network.PlayerConnection;
import org.jetbrains.annotations.NotNull;

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
        if (!AdminController.getInstance().getFrozenPlayers().contains(this.player.getUniqueID()))
            super.a(packet);
        else
            teleport(getCraftPlayer().getLocation());
    }
}
