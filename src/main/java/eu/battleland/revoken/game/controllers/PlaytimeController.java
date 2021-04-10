package eu.battleland.revoken.game.controllers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AController;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Set;

public class PlaytimeController extends AController {

    public PlaytimeController(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getMessenger().registerIncomingPluginChannel(getInstance(), "BungeeCord", (@NotNull String channel, @NotNull Player receiver, byte[] data) -> {
            ByteArrayDataInput request = ByteStreams.newDataInput(data);

            final String subChannel = request.readUTF();
            if(subChannel.equals("revoken-playtime-inquiry")) {
                ByteArrayDataOutput response = ByteStreams.newDataOutput();
                response.writeUTF("revoken-playtime-response");
                response.writeLong(receiver.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);

                receiver.sendPluginMessage(getInstance(), "BungeeCord", response.toByteArray());
                return;
            }
            if(subChannel.equals("revoken-playtime-response")) {

            }


        });
        Bukkit.getMessenger().registerOutgoingPluginChannel(getInstance(), "BungeeCord");
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }


}
