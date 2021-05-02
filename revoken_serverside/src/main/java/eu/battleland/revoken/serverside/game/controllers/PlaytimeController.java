package eu.battleland.revoken.serverside.game.controllers;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.serverside.RevokenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaytimeController extends AController<RevokenPlugin> {

    public PlaytimeController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getMessenger().registerIncomingPluginChannel(getPlugin().instance(), "BungeeCord", (@NotNull String channel, @NotNull Player receiver, byte[] data) -> {
            ByteArrayDataInput request = ByteStreams.newDataInput(data);

            final String subChannel = request.readUTF();
            if (subChannel.equals("revoken-playtime-inquiry")) {
                ByteArrayDataOutput response = ByteStreams.newDataOutput();
                response.writeUTF("revoken-playtime-response");
                response.writeLong(receiver.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);

                receiver.sendPluginMessage(getPlugin().instance(), "BungeeCord", response.toByteArray());
                return;
            }
            if (subChannel.equals("revoken-playtime-response")) {

            }


        });
        Bukkit.getMessenger().registerOutgoingPluginChannel(getPlugin().instance(), "BungeeCord");
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {

    }


}
