package eu.battleland.revoken.game.controllers;

import eu.battleland.revoken.RevokenPlugin;
import eu.battleland.revoken.abstracted.AController;
import eu.battleland.revoken.providers.api.ApiConnector;
import eu.battleland.revoken.providers.storage.flatfile.store.AStore;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
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

        Bukkit.getScheduler().scheduleAsyncRepeatingTask(getInstance(), () -> {
            Bukkit.getOnlinePlayers().forEach(player -> {
                System.out.println("task");
                var ccData = ApiConnector.getHttpApiResponse("http://czech-craft.eu/api/server/battleland-eu/player/" + player.getName() + "/next_vote/");
                if(ccData == null || !ccData.has("next_vote"))
                    return;
                System.out.println("wData");
                var nextVoteStr = ccData.get("next_vote").getAsString();
                LocalDateTime nextVote = LocalDateTime.parse(nextVoteStr, this.ccDateFormat);
                if(nextVote.isAfter(LocalDateTime.now()))
                {
                    player.sendMessage(this.rawCcReminder);
                    System.out.println("message");
                }
            });
        }, 20, 20*240);
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reloadResources() {
        try {
            this.config.prepare();
        } catch (Exception x) {
            log.error("Failed to provide config to VoteController", x);
        }
    }

    private void loadSettings() {
        var data = this.config.getData();

        this.rawCcReminder = String.join("\n", data.getStringList("vote-reminder.vote-again-czechcraft", new ArrayList<>()));
        this.rawClReminder = String.join("\n", data.getStringList("vote-reminder.vote-again-craftlist", new ArrayList<>()));
    }
}
