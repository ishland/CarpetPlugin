package com.ishland.bukkit.carpetplugin.utils;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.google.common.collect.Sets;
import com.ishland.bukkit.carpetplugin.fakes.FakeEntityPlayer;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public class BrigadierUtils {

    public static LiteralArgumentBuilder<BukkitBrigadierCommandSource> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    public static <T> RequiredArgumentBuilder<BukkitBrigadierCommandSource, T> argument(String name,
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
        for (Player player : Bukkit.getOnlinePlayers())
            if (((CraftPlayer) player).getHandle() instanceof FakeEntityPlayer)
                players.add(player.getName());
        return Collections.unmodifiableSet(players);
    }

    public static Predicate<BukkitBrigadierCommandSource> hasPermission(String s) {
        return (player) -> player.getBukkitSender().hasPermission(s);
    }
}
