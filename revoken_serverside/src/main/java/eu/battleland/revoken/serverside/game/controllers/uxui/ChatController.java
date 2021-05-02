package eu.battleland.revoken.serverside.game.controllers.uxui;

import eu.battleland.revoken.common.Revoken;
import eu.battleland.revoken.common.abstracted.AController;
import eu.battleland.revoken.common.providers.storage.flatfile.store.AStore;
import eu.battleland.revoken.serverside.RevokenPlugin;
import eu.battleland.revoken.serverside.statics.PermissionStatics;
import lombok.extern.log4j.Log4j2;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import xyz.rgnt.mth.tuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import static org.bukkit.Bukkit.getOnlinePlayers;


@Log4j2(topic = "Revoken - ChatController")
public class ChatController extends AController<RevokenPlugin> implements Listener {


    private final Map<String, Pair<String, Permission>> emotes = new HashMap<>();
    private @NotNull AStore chatConfig;
    private boolean disableBlanks = true;
    // horrible settings for emoticons
    private boolean emotesEnabled = true;
    private Permission emotesPermission = null;
    // horrible settings for mention
    private boolean mentionEnabled = true;
    private String mentionIndicator = "@";
    private String mentionColor = "&e";
    private Sound mentionSound = Sound.BLOCK_BELL_USE;
    private float mentionVolume = 1f;
    private float mentionPitch = 1f;
    private Permission mentionPermission = null;

    private boolean hasChatManager = false;


    /**
     * Colors mapped by Permissions
     */
    private Map<Permission, String> permissionColors = new HashMap<>();

    public ChatController(@NotNull Revoken<RevokenPlugin> plugin) {
        super(plugin);
    }

    @Override
    public void initialize() throws Exception {
        try {
            this.chatConfig = getPlugin().instance().getStorageProvider().provideYaml("resources", "configs/controllers/chat/chat.yaml", true);
        } catch (Exception x) {
            log.error("Failed to provide config to ChatController", x);
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
            this.chatConfig.prepare();
        } catch (Exception x) {
            log.error("Failed to re-load config for ChatController", x);
        }

        loadSettings();
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();

        String format = event.getFormat();
        String message = "";

        // chat manager applies group color only to format
        // in order for us to get group color, we have to extract the message from format
        int chatSeparatorIndex = format.indexOf("►");
        if (this.hasChatManager) {
            message = format.substring(chatSeparatorIndex);
        } else {
            message = event.getMessage();
        }


        // PERMISSION COLOR
        StringBuffer colorMod = new StringBuffer();
        this.permissionColors.forEach((permission, color) -> {
            if (sender.hasPermission(permission)) {
                colorMod.append(color);
                System.out.println("Using: " + permission);
            }
        });
        message = ChatColor.translateAlternateColorCodes('&', colorMod.toString()) + message;

        String lastColors = ChatColor.getLastColors(message);

        // MENTION
        if (mentionEnabled)
            if (mentionPermission == null || sender.hasPermission(mentionPermission))
                for (Player player : getOnlinePlayers()) {
                    if (!sender.canSee((player)))
                        return;

                    if (message.matches("(?i).*" + mentionIndicator + player.getName() + "\\b.*")) {
                        message = message.replaceAll
                                ("(?i)" + mentionIndicator + player.getName() + "\\b",
                                        ChatColor.translateAlternateColorCodes('&', mentionColor + mentionIndicator + player.getName() + lastColors)
                                );

                        player.playSound(player.getLocation(), mentionSound, SoundCategory.PLAYERS, mentionVolume, mentionPitch);
                    }
                }

        // EMOTES
        if (emotesEnabled)
            // check global emote permission
            if (emotesPermission == null || sender.hasPermission(emotesPermission))
                for (String key : this.emotes.keySet()) {
                    if (message.contains(key)) {
                        var data = this.emotes.get(key);

                        // check emote permission
                        if (data.getSecond() == null || sender.hasPermission(data.getSecond())) {
                            message = message.replaceAll(Matcher.quoteReplacement(key), data.getFirst());
                        }
                    }
                }


        var stripped = ChatColor.stripColor(message).trim();
        if (disableBlanks && (stripped.isBlank() || stripped.isEmpty()))
            event.setCancelled(true);

        if (hasChatManager)
            event.setFormat(format.substring(0, chatSeparatorIndex) + message);
        else
            event.setMessage(message);
    }

    public void loadSettings() {
        var config = this.chatConfig.getData();

        this.hasChatManager = Bukkit.getPluginManager().getPlugin("ChatManager") != null;
        if (this.hasChatManager) {
            log.info("Detected ChatManager.");
        }

        this.disableBlanks = config.getBool("chat.disable-blank-messages", true);

        // emotes
        this.emotesEnabled = config.getBool("chat.emoticons.enabled", false);
        if (this.emotesEnabled) {
            this.emotesPermission = PermissionStatics.permissionFromString(config.getString("chat.emoticons.permission"));
            if (this.emotesPermission != null)
                log.debug("Permission for emoticons: " + this.emotesPermission.getName());

            String defaultEmojiIndicator = config.getString("chat.emoticons.indicator", "\\");
            for (String configName : config.getKeys("chat.emoticons.emotes")) {
                var sector = config.getSector("chat.emoticons.emotes." + configName);
                if (sector == null)
                    continue;

                String key = sector.getString("key");
                String emote = sector.getString("emote");

                // permission
                Permission perm = PermissionStatics.permissionFromString(sector.getString("permission"));

                // override indicator if set
                String indicator = defaultEmojiIndicator;
                if (sector.isSet("indicator"))
                    indicator = sector.getString("indicator");

                if (key != null && emote != null)
                    this.emotes.put(indicator + key, Pair.of(emote, perm));
            }

            log.debug("Loaded {} emotes.", this.emotes.size());
        } else
            log.info("Emoticons are disabled.");

        // mention
        this.mentionEnabled = config.getBool("chat.mention.enabled", false);
        if (this.mentionEnabled) {
            this.mentionPermission = PermissionStatics.permissionFromString(config.getString("chat.mention.permission"));
            if (this.mentionPermission != null)
                log.debug("Permission for mentions: " + this.mentionPermission.getName());

            this.mentionIndicator = config.getString("chat.mention.indicator", "@");
            if (!config.getBool("chat.mention.color-based-on-chatcolor", false))
                this.mentionColor = config.getString("chat.mention.color", "&a");
            else
                this.mentionColor = "~+";

            var mentionSoundStr = config.getString("chat.mention.sound.name", "BLOCK_BELL_USE").toUpperCase();

            try {
                this.mentionSound = Sound.valueOf(mentionSoundStr);
            } catch (Exception x) {
                log.error("Can't parse mention sound called {}, it does not match anything I know about.", mentionSoundStr);
            }
            this.mentionVolume = config.getFloat("chat.mention.sound.volume", 1f);
            this.mentionPitch = config.getFloat("chat.mention.sound.pitch", 1f);

        } else
            log.info("Mentions are disabled.");

        this.permissionColors.clear();
        // permission color
        {
            var root = config.getSector("chat.permission-color");
            if (root != null) {
                Set<String> entries = root.getKeys();
                for (String entry : entries) {
                    /*String defaultColor = IridiumColorAPI.process(root.getString(entry + ".default-color", ""));
                    this.permissionColors.put(PermissionStatics.permissionFromString("revoken.permission-color." + entry, PermissionDefault.FALSE), defaultColor);*/
                    // todo replace with adventure api
                }
            }
        }

    }
}
