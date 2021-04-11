package eu.battleland.revoken.game.controllers.report;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AController;
import eu.battleland.revoken.providers.api.discord.DiscordWebhook;
import eu.battleland.revoken.statics.PktStatics;
import eu.battleland.revoken.statics.TimeStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2(topic = "BugReport Controller")
public class ReportController extends AController {

    // 2020 01/01
    public static long UNIX_BASE = 1577833200;

    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private @Nullable String webhookURL;
    private @NotNull DiscordWebhook webhook;

    public ReportController(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        reload();

        Bukkit.getCommandMap().register("revoken", new Command("bugreport", "Used to report a bug", "/bugreport <description>", Arrays.asList("reportbug", "bug", "reportproblem", "problem")) {
            @Override
            public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] args) {
                if(!commandSender.hasPermission("revoken.admin"))
                    return true;

                if(!(commandSender instanceof Player)) {
                    commandSender.sendMessage("§cSender must be a player.");
                    return true;
                }
                final Player player = (Player) commandSender;
                final var nmsPlayer = PktStatics.getNmsPlayer(player);

                final StringBuilder message = new StringBuilder();
                if(args.length > 0)
                    message.append(String.join(" ", args));
                else {
                    commandSender.sendMessage("§cNapis kratku spravu popisujucu problem.");
                    return true;
                }
                commandSender.sendMessage("§aNahlasujem bug...");

                final Location playerLocation = player.getLocation();
                final int playerPing = nmsPlayer.ping;
                final var rsStatus = player.getResourcePackStatus();
                final int lastTps    = (int) Bukkit.getServer().getTPS()[0];

                final long reportUnixTimestamp = System.currentTimeMillis() / 1000;
                final long idUnixTimestamp = reportUnixTimestamp - UNIX_BASE;

                threadPool.submit(() -> {
                    String xyzText = String.format("X: %d, Y: %d, Z: %d, World: %s", playerLocation.getBlockX(), playerLocation.getBlockY(), playerLocation.getBlockZ(), playerLocation.getWorld().getName());

                    var embed = new DiscordWebhook.EmbedObject().setColor(new Color(230, 95, 95))
                            .setAuthor(player.getName(),  "", "https://minotar.net/avatar/" + player.getName())
                            .setTitle(Bukkit.getServer().getMotd() + " - " + Long.toHexString(idUnixTimestamp))
                            .setDescription(message.toString())
                            .addField("Lokacia", xyzText, false)
                            .addField("TPS a Ping", lastTps + "tps, " + playerPing + "ms", true)
                            .addField("ResourcePack Status", (rsStatus == null ? "none" : rsStatus.name().toLowerCase()), true)
                            .addField("Client Brand", player.getClientBrandName(), true)
                            .addField("Client View", player.getClientViewDistance() + "c", true)
                            .setFooter("Nahlasene: " + Instant.ofEpochSecond(reportUnixTimestamp).atZone(ZoneId.systemDefault()).format(TimeStatics.prettyDateTimeFormat), "");

                    webhook.addEmbed(embed);
                    webhook.setContent(String.format("Hráč `%s` nahlásil bug na `%s`", player.getName(), Bukkit.getServer().getMotd()));
                    webhook.setUsername("Problem Report / " + player.getName());
                    webhook.setAvatarUrl("https://icons.iconarchive.com/icons/paomedia/small-n-flat/1024/sign-error-icon.png");
                    System.out.println("embed created");
                    try {
                        webhook.execute();
                        webhook.clearEmbeds();
                        player.sendMessage("§8# §7Zaslal som tvoj report. Id: §a" + Long.toHexString(idUnixTimestamp));
                    } catch (IOException e) {
                        log.error("Failed to send bug report from {}.", player.getName(), e);
                        player.sendMessage("§cNepodarilo sa mi poslať tvoj report. Skúsim to znova za chvíľu.");
                    }
                });
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
        getInstance().getGlobalConfig().ifPresent((config) -> {
            this.webhookURL = config.getData().getString("discord-webhook.bugreport-webhook", "");
        });
        if(webhookURL == null || webhookURL.isEmpty())
            log.warn("Webhook for Discord not provided from global config.");
        else {
            this.webhook = new DiscordWebhook(this.webhookURL);
        }
    }
}
