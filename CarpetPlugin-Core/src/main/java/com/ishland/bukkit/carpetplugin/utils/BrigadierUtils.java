package com.ishland.bukkit.carpetplugin.utils;

import com.google.common.collect.Sets;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.ChatMessage;
import net.minecraft.server.CommandListenerWrapper;
import net.minecraft.server.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class BrigadierUtils {

    public static LiteralArgumentBuilder<CommandListenerWrapper> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<CommandListenerWrapper, T> argument(String name,
                                                                                  ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static CompletableFuture<Suggestions> suggestMatching(Iterable<String> iterable,
                                                                 SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);

        for (String entry : iterable) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(entry);
            }
        }

        return builder.buildFuture();
    }

    public static Set<String> getPlayers() {
        Set<String> players = Sets.newHashSet("Steve", "Alex");
        for (EntityPlayer player : ((CraftServer) Bukkit.getServer()).getServer().getPlayerList().getPlayers())
            if (player instanceof FakeEntityPlayer)
                players.add(player.getName());
        return Collections.unmodifiableSet(players);
    }

    public static Predicate<CommandListenerWrapper> hasPermission(String s) {
        return (player) -> player.getBukkitSender().hasPermission(s);
    }

    public static void sendMessage(CommandContext<CommandListenerWrapper> ctx, BaseComponent... components) {
        ctx.getSource().sendMessage(new ChatMessage(new TextComponent(components).toLegacyText()), false);
    }
}
