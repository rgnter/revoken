package eu.battleland.revoken.serverside.providers.statics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.network.chat.IChatBaseComponent;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TextStatics {

    public static @NotNull Component adventureMarkdown(@NotNull String text, @Nullable Map<String, String> placeholders) {
        return placeholders == null ? MiniMessage.markdown().parse(ChatColor.translateAlternateColorCodes('&', text))
                : MiniMessage.markdown().parse(ChatColor.translateAlternateColorCodes('&', text), placeholders);
    }

    public static @NotNull TextComponent adventureMarkdownBukkit(@NotNull String text, @Nullable Map<String, String> placeholders) {
        final var json = GsonComponentSerializer.gson().serialize(TextStatics.adventureMarkdown(text, placeholders));
        return new TextComponent(ComponentSerializer.parse(json));
    }

//    public static @Nullable IChatBaseComponent adventureMarkdownNative(@NotNull String text, @Nullable Map<String, String> placeholders) {
//        final var json = GsonComponentSerializer.gson().serialize(TextStatics.adventureMarkdown(text, placeholders));
//        System.out.println(json);
//        return IChatBaseComponent.ChatSerializer.jsonToComponent(json);
//    }
}
