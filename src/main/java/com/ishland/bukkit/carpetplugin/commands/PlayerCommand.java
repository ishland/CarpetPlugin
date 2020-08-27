package com.ishland.bukkit.carpetplugin.commands;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent;
import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.fakes.FakeEntityPlayer;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.server.v1_16_R2.DedicatedPlayerList;
import net.minecraft.server.v1_16_R2.DedicatedServer;
import net.minecraft.server.v1_16_R2.EnumGamemode;
import net.minecraft.server.v1_16_R2.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Objects;

import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.*;

public class PlayerCommand implements Listener {

    @EventHandler
    public void register(
            @SuppressWarnings("deprecation") CommandRegisteredEvent<BukkitBrigadierCommandSource> event) {
        if (event.getCommandLabel().equals("player"))
            event.setLiteral(
                    literal("player")
                            .requires(hasPermission("carpet.player"))
                            .then(argument("player", StringArgumentType.word())
                                    .suggests((ctx, builder) -> suggestMatching(getPlayers(), builder))
                                    .then(literal("spawn")
                                            .requires(hasPermission("carpet.player.spawn"))
                                            .executes(PlayerCommand::spawn)
                                    )
                                    .then(literal("kill")
                                            .requires(hasPermission("carpet.player.kill"))
                                            .executes(PlayerCommand::kill)
                                    )
                            )
                            .build()
            );
    }

    private static int kill(CommandContext<BukkitBrigadierCommandSource> ctx) {
        if (!isFakePlayer(ctx)) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Only fake players can be killed").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        String playerName = StringArgumentType.getString(ctx, "player");
        final CraftPlayer bukkitPlayer = (CraftPlayer) Bukkit.getPlayerExact(playerName);
        assert bukkitPlayer != null;
        FakeEntityPlayer player = ((FakeEntityPlayer) bukkitPlayer.getHandle());
        player.killEntity();
        return 1;
    }

    private static int spawn(CommandContext<BukkitBrigadierCommandSource> ctx) {
        if (!canSpawn(ctx)) return 0;
        String playerName = StringArgumentType.getString(ctx, "player");
        Preconditions.checkNotNull(playerName);
        Preconditions.checkArgument(!playerName.isEmpty());
        if (playerName.length() > 40) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player name ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" is longer than 40 characters").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        final DedicatedServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        final WorldServer worldServer =
                ((CraftWorld) Objects.requireNonNull(ctx.getSource().getBukkitLocation()).getWorld()).getHandle();
        EnumGamemode gameMode = EnumGamemode.SURVIVAL;
        Location location = ctx.getSource().getBukkitLocation();
        assert location != null;
        FakeEntityPlayer entityPlayer = FakeEntityPlayer.createFake(
                playerName,
                server,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch(),
                worldServer,
                gameMode);
        if (entityPlayer == null) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" cannot get spawned").color(ChatColor.RED).append("").reset()
                    .create()
            );
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Possible problems: ").color(ChatColor.RED).append("").reset()
                    .create()
            );
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("1. Player doesn't exists and cannot spawn in online mode. ").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        return 1;
    }

    private static boolean canSpawn(CommandContext<BukkitBrigadierCommandSource> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        final Location location = ctx.getSource().getBukkitLocation();
        if (location == null) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player can only spawned with location").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return false;
        }
        final Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" is already logged on").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return false;
        }

        final DedicatedPlayerList playerList = ((CraftServer) Bukkit.getServer()).getHandle();
        final DedicatedServer server = playerList.getServer();
        GameProfile profile = server.getUserCache().getProfile(playerName);
        if (playerList.getProfileBans().isBanned(profile)) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" is banned").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return false;
        }

        if (playerList.getHasWhitelist() && profile != null && playerList.isWhitelisted(profile)
                && !ctx.getSource().getBukkitSender().hasPermission("carpet.player.spawn.whitelist")) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" is whitelisted and insufficient permission").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return false;
        }

        return true;
    }

    private static boolean isFakePlayer(CommandContext<BukkitBrigadierCommandSource> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return false;
        return ((CraftPlayer) player).getHandle() instanceof FakeEntityPlayer;
    }

}
