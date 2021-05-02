package eu.battleland.revoken.serverside.game.controllers.security;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.api.ApiConnector;
import eu.battleland.revoken.common.providers.api.discord.DiscordWebhook;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.statics.PktStatics;
import eu.battleland.revoken.serverside.statics.TimeStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.rgnt.mth.tuples.Pair;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Log4j2(topic = "BugReport Controller")
public class BattleRepController extends AController<RevokenPlugin> {

    // 2020 01/01
    public static long UNIX_BASE = 1577833200;
    private final @NotNull DiscordWebhook webhook = new DiscordWebhook("");
    // uuid of reporter, pair of uuid of reportee and report id
    private final Map<UUID, List<Pair<UUID, Long>>> playerReportedPlayers = new HashMap<>();
    // uuid of reporter, pair of uuid of timestamp and report id
    private final Map<UUID, Pair<Long, Long>> playerLastReport = new HashMap<>();
    private ExecutorService threadPool;
    private @Nullable String webhookURL;


    public BattleRepController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        Bukkit.getPluginManager().addPermission(new Permission("revoken.report", PermissionDefault.TRUE));

        this.threadPool = Executors.newCachedThreadPool();
        reload();

        Bukkit.getCommandMap().register("revoken", new Command("bugreport", "Used to report a bug", "/bugreport <description>", Arrays.asList("reportbug", "bug", "reportproblem", "problemreport", "problem")) {
            @Override
            public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] args) {
                if (!commandSender.hasPermission("revoken.report"))
                    return true;

                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage("§cSender must be a player.");
                    return true;
                }
                final Player player = (Player) commandSender;
                final StringBuilder message = new StringBuilder();
                if (args.length > 0)
                    message.append(String.join(" ", args));
                else {
                    commandSender.sendMessage("§8# §fSpecifikuj kratku spravu popisujucu problem.");
                    return true;
                }
                commandSender.sendMessage("§8# §7Nahlasujem bug...");

                final long reportUnixTimestamp = System.currentTimeMillis() / 1000;
                final long idUnixTimestamp = reportUnixTimestamp - UNIX_BASE;

                try {
                    threadPool.submit(() -> {
                        synchronized (webhook) {
                            var embed = addInfoToEmbed(
                                    commonEmbed(player, message.toString(), idUnixTimestamp, reportUnixTimestamp),
                                    player
                            );

                            webhook.addEmbed(embed);
                            webhook.setContent(String.format("Hráč `%s` nahlásil problém na `%s`", player.getName(), Bukkit.getServer().getMotd()));
                            webhook.setUsername("Problem Report / " + player.getName());
                            webhook.setAvatarUrl("https://www.battleland.eu/uploads/Gallery/Img/330f68cc45ff.png");

                            try {
                                webhook.execute();
                                webhook.clearEmbeds();
                                player.sendMessage("§8# §7Zaslal som tvoj report. Id: §a" + Long.toHexString(idUnixTimestamp));
                            } catch (IOException e) {
                                log.error("Failed to send bug report from {}.", player.getName(), e);
                                player.sendMessage("§cNepodarilo sa mi poslať tvoj report. Skúsim to znova za chvíľu.");
                            }
                        }
                    });
                } catch (Exception x) {
                    player.sendMessage("§cNepodarilo sa mi poslať tvoj report. Skúsim to znova za chvíľu.");
                }
                return true;
            }
        });

        Bukkit.getCommandMap().register("eu/battleland/revoken", new Command("playerreport", "Used to report a player", "/playerreport <player name> <description>", Arrays.asList("reportplayer", "reportuser", "hacker")) {
            @Override
            public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] args) {
                if (!commandSender.hasPermission("revoken.report"))
                    return true;

                if (!(commandSender instanceof Player)) {
                    commandSender.sendMessage("§cSender must be a player.");
                    return true;
                }
                String targetName = "";
                if (args.length > 0) {
                    targetName = args[0];
                } else {
                    commandSender.sendMessage("§c# §7Špecifikuj meno hráča");
                    return true;
                }

                final Player player = (Player) commandSender;
                final Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    commandSender.sendMessage("§c# §7Hráč nie je online.");
                    return true;
                }

                final StringBuilder message = new StringBuilder();
                if (args.length > 1)
                    message.append(String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                else {
                    commandSender.sendMessage("§8# §fSpecifikuj strucny dovod preco reportujes tohto hraca. ");
                    return true;
                }
                commandSender.sendMessage("§8# §7Nahlasujem hraca...");

                final long reportUnixTimestamp = System.currentTimeMillis() / 1000;
                final long idUnixTimestamp = reportUnixTimestamp - UNIX_BASE;

                try {
                    threadPool.submit(() -> {
                        var header = commonEmbed(player, message.toString(), idUnixTimestamp, reportUnixTimestamp);

                        var playerInfo = new DiscordWebhook.EmbedObject();
                        addInfoToEmbed(playerInfo, player);

                        var targetInfo = new DiscordWebhook.EmbedObject();
                        addInfoToEmbed(targetInfo, target);

                        webhook.addEmbed(header, playerInfo, targetInfo);
                        webhook.setContent(String.format("Hráč `%s` nahlásil hráča `%s` na `%s`", player.getName(), target.getName(), Bukkit.getServer().getMotd()));
                        webhook.setUsername("Player Report / " + player.getName());
                        webhook.setAvatarUrl("https://www.battleland.eu/uploads/Gallery/Img/c882ce0529a0.png");

                        try {
                            webhook.execute();
                            webhook.clearEmbeds();
                            player.sendMessage("§8# §7Zaslal som tvoj report. Id: §a" + Long.toHexString(idUnixTimestamp));
                        } catch (IOException e) {
                            log.error("Failed to send problem report from {}.", player.getName(), e);
                            player.sendMessage("§cNepodarilo sa mi poslať tvoj report. Skúsim to znova za chvíľu.");
                        }
                    });
                } catch (Exception x) {
                    player.sendMessage("§cNepodarilo sa mi poslať tvoj report. Skúsim to znova za chvíľu.");
                }
                return true;
            }
        });
    }

    @Override
    public void terminate() {
        this.threadPool.shutdown();
    }

    @Override
    public void reload() {
        getPlugin().instance().getPluginConfig().ifPresent((config) -> {
            this.webhookURL = config.getData().getString("discord-webhook.webhook", "");
        });
        if (webhookURL == null || webhookURL.isEmpty())
            log.warn("Webhook for Discord not provided from global config.");
        else {
            this.webhook.setUrl(this.webhookURL);
        }
    }


    private @NotNull DiscordWebhook.EmbedObject commonEmbed(@NotNull Player player, @NotNull String message, long id, long reportUnixTimestamp) {
        return new DiscordWebhook.EmbedObject().setColor(new Color(230, 95, 95))
                .setAuthor(player.getName(), "", ApiConnector.getSkinHeadUrl(player.getName()))
                .setTitle(Bukkit.getServer().getMotd() + " - " + Long.toHexString(id))
                .setDescription(message)
                .setFooter("Nahlasene: " + Instant.ofEpochSecond(reportUnixTimestamp).atZone(ZoneId.systemDefault()).format(TimeStatics.prettyDateTimeFormat), "");
    }

    private @NotNull DiscordWebhook.EmbedObject addInfoToEmbed(@NotNull DiscordWebhook.EmbedObject embed, @NotNull Player player) {
        final var nmsPlayer = PktStatics.getNmsPlayer(player);

        final Location playerLocation = player.getLocation();
        final int playerPing = nmsPlayer.ping;
        final var rsStatus = player.getResourcePackStatus();
        final int lastTps = (int) Bukkit.getServer().getTPS()[0];


        embed.addField("Lokacia", String.format("X: %d, Y: %d, Z: %d, World: %s", playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ(), playerLocation.getWorld().getName()), false)
                .setAuthor(player.getName(), "", ApiConnector.getSkinHeadUrl(player.getName()))
                .addField("TPS a Ping", lastTps + "tps, " + playerPing + "ms", true)
                .addField("ResourcePack Status", (rsStatus == null ? "none" : rsStatus.name().toLowerCase()), true)
                .addField("Client Brand", player.getClientBrandName(), true)
                .addField("Client View", player.getClientViewDistance() + "chunks", true);
        return embed;
    }
}
