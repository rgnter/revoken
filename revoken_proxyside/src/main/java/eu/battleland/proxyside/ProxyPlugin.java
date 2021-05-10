package eu.battleland.proxyside;


import eu.battleland.proxyside.protmotd.ProtocoledServerlist;
import eu.battleland.proxyside.telemetry.DataCollector;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.providers.storage.flatfile.StorageProvider;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;

public class ProxyPlugin extends Plugin implements Revoken<ProxyPlugin>, Listener {

    @Getter
    private @NotNull StorageProvider storageProvider;
    @Getter
    private @NotNull DataCollector dataCollector;

    @Getter
    private @NotNull ProtocoledServerlist serverlist;


    @Override
    public void onLoad() {
        super.onLoad();
        getLogger().info("Constructing proxy");

        {
            this.storageProvider = new StorageProvider(this);
            this.dataCollector = new DataCollector();

            this.serverlist = new ProtocoledServerlist(this);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        getLogger().info("Initializing proxy");

        {
            getProxy().getPluginManager().registerListener(this, this);
            getProxy().getPluginManager().registerCommand(this, new Command("telemetry") {
                @Override
                public void execute(CommandSender sender, String[] args) {
                    if (!sender.hasPermission("battleproxy.admin"))
                        return;

                    if (args.length == 0) {
                        sender.sendMessage(new TextComponent("§cMissing player argument"));
                        return;
                    }

                    String playerName = args[0];
                    ProxiedPlayer player = getProxy().getPlayer(playerName);
                    if (player == null) {
                        sender.sendMessage(new TextComponent("§cMissing player argument"));
                        return;
                    }

                    Executors.newSingleThreadExecutor().submit(() -> {
                        try {
                            DataCollector.Result result = dataCollector.getPlayerData(player);
                            sender.sendMessage(new TextComponent("§7Player '" + player.getName() + "' has joined from " + result.getCountryName() + "(" + result.getCountryCode() + "), through ISP " + result.getIsp() + "(" + result.getIspOrg() + "; " + result.getAs() + ")"));
                        } catch (IOException x) {
                            x.printStackTrace();
                            sender.sendMessage(new TextComponent("§c" + x.toString()));
                        }
                    });
                }

            });

            this.serverlist.initialize();
        }

        getProxy().getPluginManager().registerCommand(this, new Command("proxybroadcast", "", "proxybc") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission("battleproxy.admin"))
                    return;

                if (args.length == 0) {
                    sender.sendMessage("§cMissing message argument");
                    return;
                }

                String rawMessage = String.join(" ", args);
                TextComponent message = new TextComponent(
                        ComponentSerializer.parse(
                                GsonComponentSerializer.gson().serialize(
                                        MiniMessage.markdown().parse(rawMessage))));
                getProxy().getPlayers().forEach(player -> {
                    player.sendMessage(ChatMessageType.CHAT, message);
                });
                getProxy().getConsole().sendMessage(new TextComponent(new TextComponent("§fProxyBroadcast from " + sender.getName() + " §9:: "), message));
            }

        });

        getProxy().getPluginManager().registerCommand(this, new Command("serverbroadcast", "", "serverbc") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission("battleproxy.admin"))
                    return;

                Collection<ProxiedPlayer> serverPlayers = null;
                String rawMessage;
                if (args.length == 0) {
                    sender.sendMessage("§cMissing server name argument");
                    return;
                }

                String serverName = args[0];

                // if self reference
                if (serverName.matches("^~")) {
                    if (sender instanceof ProxiedPlayer)
                        serverPlayers = ((ProxiedPlayer) sender).getServer().getInfo().getPlayers();
                    else {
                        sender.sendMessage("§cSelf server reference not possible");
                        return;
                    }
                }
                // if target reference
                if (serverName.matches("~\\b.*\\b")) {
                    String playerName = serverName.substring(1);
                    ProxiedPlayer target = getProxy().getPlayer(playerName);
                    if (target == null) {
                        sender.sendMessage("§cTarget player invalid");
                        return;
                    }

                    serverPlayers = target.getServer().getInfo().getPlayers();
                } else {
                    var server = getProxy().getServerInfo(serverName);
                    if (server == null) {
                        sender.sendMessage("§cTarget server invalid");
                        return;
                    }

                    serverPlayers = server.getPlayers();
                }

                if (serverPlayers == null)
                    return;

                if (args.length == 1) {
                    sender.sendMessage("§cMissing message argument (Supports markdown)");
                    return;
                }
                rawMessage = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                TextComponent message = new TextComponent(
                        ComponentSerializer.parse(
                                GsonComponentSerializer.gson().serialize(
                                        MiniMessage.markdown().parse(rawMessage))));

                serverPlayers.forEach(player -> {
                    player.sendMessage(ChatMessageType.CHAT, message);
                });
                getProxy().getConsole().sendMessage(new TextComponent(new TextComponent("§fServerBroadcast from " + sender.getName() + " on " + serverName + " §9:: "), message));
            }
        });

        this.serverlist.initialize();

        getProxy().getPluginManager().registerCommand(this, new Command("battleproxy") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                if (!sender.hasPermission("battleproxy.admin"))
                    return;

                if (args.length == 0) {
                    sender.sendMessage("§cMissing argument");
                    return;
                }

                String arg = args[0];
                if (arg.equalsIgnoreCase("reload")) {
                    serverlist.reloadConfig();
                    sender.sendMessage("§aok.");
                }
            }
        });

        this.serverlist.initialize();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        getLogger().info("Terminating proxy");
        this.serverlist.terminate();
    }

    @EventHandler
    public void onPlayerJoin(PostLoginEvent event) {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                var player = event.getPlayer();
                DataCollector.Result result = dataCollector.getPlayerData(player);
                getLogger().info("§7Player '" + player.getName() + "' has joined from " + result.getCountryName() + "(" + result.getCountryCode() + "), through ISP " + result.getIsp() + "(" + result.getIspOrg() + "; " + result.getAs() + ")");

            } catch (Exception ignored) {
                getLogger().warning("Failed to collect data from player '" + event.getPlayer().getName() + "'");
            }
        });
    }

    @Override
    public ProxyPlugin instance() {
        return this;
    }

    @Override
    public @NotNull InputStream getResource(@NotNull String resourcePath) {
        return super.getResourceAsStream(resourcePath);
    }
}
