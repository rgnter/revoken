package eu.battleland.revoken.serverside.game.controllers;

import com.destroystokyo.paper.Title;
import com.iridium.iridiumcolorapi.IridiumColorAPI;
import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.providers.statics.PlaceholderStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

@Log4j2(topic = "Revoken - Interface Controller")
public class InterfaceController extends AController<RevokenPlugin> implements Listener {

    private @NotNull AStore config;

    private boolean firstJoinTitleEnabled;
    private String firstJoinTitleText;
    private String firstJoinSubtitleText;
    private Title.Builder firstJoinTitle;

    private boolean joinTitleEnabled;
    private String joinTitleText;
    private String joinSubtitleText;
    private Title.Builder joinTitle;

    public InterfaceController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        try {
            this.config = getPlugin().instance().getStorageProvider().provideYaml("resources", "configs/controllers/interface/interface.yaml", true);
        } catch (Exception x) {
            log.error("Failed to provide config to InterfaceController", x);
        }
        loadSettings();

        Bukkit.getPluginManager().registerEvents(this, getPlugin().instance());
    }

    @Override
    public void terminate() {

    }

    @Override
    public void reload() {
        try {
            this.config.prepare();
        } catch (Exception e) {
            log.error("Couldn't reload interface config for InterfaceManager");
        }
        loadSettings();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            if (!firstJoinTitleEnabled)
                return;
            this.firstJoinTitle
                    .title(PlaceholderStatics.askPapiForPlaceholders(this.firstJoinTitleText, player))
                    .subtitle(PlaceholderStatics.askPapiForPlaceholders(this.firstJoinSubtitleText, player))
                    .build().send(player);
        } else {
            if (!joinTitleEnabled)
                return;
            this.joinTitle
                    .title(PlaceholderStatics.askPapiForPlaceholders(this.joinTitleText, player))
                    .subtitle(PlaceholderStatics.askPapiForPlaceholders(this.joinSubtitleText, player))
                    .build().send(player);
        }
    }

    public void loadSettings() {
        var configData = this.config.getData();

        if (configData.isSet("first-join-title.enabled")) {
            var root = configData.getSector("first-join-title");
            if (root != null) {
                this.firstJoinTitleEnabled = root.getBool("enabled", false);
                // todo replace with adventure
                this.firstJoinTitleText = ChatColor.translateAlternateColorCodes('&', IridiumColorAPI.process(root.getString("title", "")));
                this.firstJoinSubtitleText = ChatColor.translateAlternateColorCodes('&', IridiumColorAPI.process(root.getString("subtitle", "")));
                this.firstJoinTitle = new Title.Builder()
                        .fadeIn(root.getInt("times.fade-in", 0))
                        .stay(root.getInt("times.stay", 0))
                        .fadeOut(root.getInt("times.fade-out", 0));
            }
        }
        if (configData.isSet("join-title.enabled")) {
            var root = configData.getSector("join-title");
            if (root != null) {
                this.joinTitleEnabled = root.getBool("enabled", false);
                this.joinTitleText = IridiumColorAPI.process(root.getString("title", ""));
                this.joinSubtitleText = IridiumColorAPI.process(root.getString("subtitle", ""));
                this.joinTitle = new Title.Builder()
                        .fadeIn(root.getInt("times.fade-in", 0))
                        .stay(root.getInt("times.stay", 0))
                        .fadeOut(root.getInt("times.fade-out", 0));
            }
        }

    }

}
