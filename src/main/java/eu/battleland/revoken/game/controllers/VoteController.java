package eu.battleland.revoken.game.controllers;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AController;
import eu.battleland.revoken.providers.api.ApiConnector;
import eu.battleland.revoken.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.statics.PlaceholderStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.net.http.HttpClient;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Log4j2(topic = "Revoken - Vote Controller")
public class VoteController extends AController {

    /**
     * CC - CzechCraft
     * CL - CraftList
     */

    private @NotNull AStore config;

    private final DateTimeFormatter ccDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter clDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private @NotNull String rawCcReminder;
    private @NotNull String rawClReminder;

    private boolean enabled;
    private int taskId = -1;
    private int updateTick = 360;
    private int remindCount = 3;

    private ConcurrentHashMap<UUID, Long> nextVote = new ConcurrentHashMap<>();


    public VoteController(@NotNull RevokenPlugin plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        try {
            this.config = getInstance().getStorageProvider().provideYaml("resources", "configs/vote.yaml", true);
        } catch (Exception x) {
            log.error("Failed to provide config to VoteController", x);
        }
        loadSettings();
        setupTask();
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {
        try {
            this.config.prepare();
        } catch (Exception x) {
            log.error("Failed to provide config to VoteController", x);
        }
        loadSettings();

        if(this.enabled)
            setupTask();
    }

    private void setupTask() {
        if(this.taskId != -1)
            Bukkit.getScheduler().cancelTask(this.taskId);
        this.taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                var ccData = ApiConnector.getHttpApiResponse("https://czech-craft.eu/api/server/battleland-eu/player/" + player.getName() + "/next_vote/");
                if(ccData == null || !ccData.has("next_vote"))
                    return;

                var nextVoteStr = ccData.get("next_vote").getAsString();
                LocalDateTime nextVote = LocalDateTime.parse(nextVoteStr, this.ccDateFormat);
                if(nextVote.isBefore(LocalDateTime.now()))
                {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', PlaceholderStatics.askPapiForPlaceholders(this.rawCcReminder, player)));
                }

            });
        }, this.updateTick, this.updateTick);
    }

    private void loadSettings() {
        var data = this.config.getData();

        this.enabled = data.getBool("vote-reminder.enabled", false);
        this.rawCcReminder = String.join("\n", data.getStringList("vote-reminder.vote-again-czechcraft", new ArrayList<>()));
        this.rawClReminder = String.join("\n", data.getStringList("vote-reminder.vote-again-craftlist", new ArrayList<>()));

        this.updateTick = data.getInt("vote-reminder.update-time", 360) * 20;
        this.remindCount = data.getInt("vote-reminder.remind-count", 3);
    }
}
