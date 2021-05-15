package eu.battleland.revoken.serverside.providers.statics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TextStatics {

    public static @NotNull Component adventureMarkdown(@NotNull String text, @Nullable Map<String, String> placeholders) {
        return placeholders == null ? MiniMessage.markdown().parse(ChatColor.translateAlternateColorCodes('&', text))
                : MiniMessage.markdown().parse(ChatColor.translateAlternateColorCodes('&', text), placeholders);
    }

}
