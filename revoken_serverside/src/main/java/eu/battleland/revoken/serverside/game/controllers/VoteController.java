package eu.battleland.revoken.serverside.game.controllers;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.api.ApiConnector;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.statics.PlaceholderStatics;
import eu.battleland.revoken.serverside.providers.statics.TextStatics;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Log4j2(topic = "Revoken - Vote Controller")
public class VoteController extends AController<RevokenPlugin> {

    private final DateTimeFormatter ccDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter clDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * CC - CzechCraft
     * CL - CraftList
     */

    private @NotNull AStore config;
    private @NotNull String rawCcReminder;
    private @NotNull String rawClReminder;

    private boolean enabled;
    private int taskId = -1;
    private int updateTick = 360;
    private int remindCount = 3;

    private ConcurrentHashMap<UUID, Long> nextVote = new ConcurrentHashMap<>();


    public VoteController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        try {
            this.config = getPlugin().instance().getStorageProvider().provideYaml("resources", "configs/controllers/vote/vote.yaml", true);
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

        if (this.enabled)
            setupTask();
    }

    private void setupTask() {
        if (this.taskId != -1)
            Bukkit.getScheduler().cancelTask(this.taskId);
        this.taskId = Bukkit.getScheduler().scheduleAsyncRepeatingTask(getPlugin().instance(), () -> {

            String statusStr = Bukkit.getOnlinePlayers().stream().map(player -> {
                var ccData = ApiConnector.getHttpApiResponse("https://czech-craft.eu/api/server/battleland-eu/player/" + player.getName() + "/next_vote/");
                if (ccData == null || !ccData.has("next_vote"))
                    return "§7" + player.getName();

                var nextVoteStr = ccData.get("next_vote").getAsString();
                LocalDateTime nextVote = LocalDateTime.parse(nextVoteStr, this.ccDateFormat);
                boolean hasVoted = nextVote.isBefore(LocalDateTime.now());

                if (hasVoted) {
                    player.sendMessage(
                            ComponentSerializer.parse(
                                    GsonComponentSerializer.gson().serialize(
                                            TextStatics.adventureMarkdown(PlaceholderStatics.askPapiForPlaceholders(this.rawCcReminder, player), null)
                                    )
                            )
                    );
                    return "§a" + player.getName();
                } else
                    return "§c" + player.getName();

            }).collect(Collectors.joining(", "));

            if (!statusStr.isEmpty())
                log.info("Vote status: {}", statusStr);
            else
                log.info("Vote status: §7Nobody online");
        }, 80, this.updateTick);

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
