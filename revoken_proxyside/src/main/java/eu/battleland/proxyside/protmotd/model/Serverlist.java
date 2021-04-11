package eu.battleland.proxyside.protmotd.model;

import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class Serverlist {


    @Setter
    private List<@NotNull String> motdLines = Collections.emptyList();
    @Setter
    private List<@NotNull String> sampleLines = Collections.emptyList();


    public void processServerPing(@NotNull ServerPing ping) {
        if (this.motdLines.size() > 0) {
            String processedMotd = this.motdLines.stream().map(this::processPlaceholders).collect(Collectors.joining("\n"));
            TextComponent motd = new TextComponent(
                    ComponentSerializer.parse(
                            GsonComponentSerializer.gson().serialize(
                                    MiniMessage.markdown().parse(processedMotd))));
            ping.setDescriptionComponent(motd);
        }
        if (this.sampleLines.size() > 0) {
            ServerPing.Players players = ping.getPlayers();
            ServerPing.PlayerInfo[] playerInfo = new ServerPing.PlayerInfo[sampleLines.size()];
            for (int i = 0; i < sampleLines.size(); i++) {
                playerInfo[i] = new ServerPing.PlayerInfo(processPlaceholders(sampleLines.get(i)), UUID.randomUUID());
            }
            players.setSample(playerInfo);
        }
    }


    private @NotNull String processPlaceholders(@NotNull String message) {
        message = message.replaceAll("(?i)%online%", ProxyServer.getInstance().getOnlineCount() + "");
        message = message.replaceAll("(?i)%max%", ProxyServer.getInstance().getConfig().getPlayerLimit() + "");
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }
}
