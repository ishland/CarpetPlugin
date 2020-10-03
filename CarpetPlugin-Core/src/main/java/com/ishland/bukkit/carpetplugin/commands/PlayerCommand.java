package com.ishland.bukkit.carpetplugin.commands;

import com.google.common.base.Preconditions;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.action.FakeEntityPlayerActionPack;
import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.server.ArgumentAnchor;
import net.minecraft.server.ArgumentRotation;
import net.minecraft.server.ArgumentVec3;
import net.minecraft.server.CommandListenerWrapper;
import net.minecraft.server.DedicatedPlayerList;
import net.minecraft.server.DedicatedServer;
import net.minecraft.server.EnumGamemode;
import net.minecraft.server.Vec2F;
import net.minecraft.server.Vec3D;
import net.minecraft.server.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.argument;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.getPlayers;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.hasPermission;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.literal;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.suggestMatching;

public class PlayerCommand {

    private static final ThreadPoolExecutor executor =
            new ThreadPoolExecutor(
                    1,
                    Runtime.getRuntime().availableProcessors() * 16,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>());

    public void register() {
        final CommandDispatcher<CommandListenerWrapper> commandDispatcher =
                ((CraftServer) Bukkit.getServer()).getHandle().getServer()
                        .getCommandDispatcher().a();
        commandDispatcher.register(literal("player")
                .requires(hasPermission("carpet.player"))
                .then(argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> suggestMatching(getPlayers(), builder))
                        .then(literal("spawn")
                                .requires(hasPermission("carpet.player.spawn"))
                                .executes(PlayerCommand::spawnAsync)
                                .then(literal("sync")
                                        .requires(hasPermission("carpet.player.spawn.sync"))
                                        .executes(PlayerCommand::spawn)
                                )
                        )
                        .then(literal("kill")
                                .requires(hasPermission("carpet.player.kill"))
                                .executes(PlayerCommand::kill)
                        )
                        .then(literal("stop")
                                .requires(hasPermission("carpet.player.stop"))
                                .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::stopAll))
                        )
                        .then(literal("actions")
                                .requires(hasPermission("carpet.player.actions"))
                                .executes(PlayerCommand::actions)
                        )
                        .then(literal("sneak")
                                .requires(hasPermission("carpet.player.sneak"))
                                .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::doSneak))
                        )
                        .then(literal("unsneak")
                                .requires(hasPermission("carpet.player.sneak"))
                                .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::unSneak))
                        )
                        .then(literal("sprint")
                                .requires(hasPermission("carpet.player.sprint"))
                                .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::doSprint))
                        )
                        .then(literal("unsprint")
                                .requires(hasPermission("carpet.player.sprint"))
                                .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::unSprint))
                        )
                        .then(literal("look")
                                .requires(hasPermission("carpet.player.look"))
                                .then(literal("at")
                                        .then(literal("block").then(argument("blockpos", ArgumentVec3.a())
                                                .executes(PlayerCommand::lookAtBlock)
                                        ))
                                        .then(literal("direction").then(argument("direction", ArgumentRotation.a())
                                                .executes(PlayerCommand::lookAtDirection)
                                        ))
                                )
                        )
                        .then(literal("use")
                                .requires(hasPermission("carpet.player.use"))
                                .then(literal("once")
                                        .executes((ctx) -> manipulate(ctx, FakeEntityPlayerActionPack::doUse))
                                )
                                .then(literal("continuous")
                                        .then(argument("interval", IntegerArgumentType.integer(1))
                                                .then(argument("repeats", IntegerArgumentType.integer(1))
                                                        .executes((ctx) -> manipulate(ctx, ap -> ap.doUse(
                                                                IntegerArgumentType.getInteger(ctx, "interval"),
                                                                IntegerArgumentType.getInteger(ctx, "repeats")
                                                                ))
                                                        )
                                                )
                                        )
                                        .executes((ctx) -> manipulate(ctx, ap -> ap.doUse(1, 1)))
                                )
                        )
                )
        );
    }

    private static int lookAtDirection(CommandContext<CommandListenerWrapper> ctx) {
        return manipulate(ctx, ap -> {
            Vec2F rotation = ArgumentRotation.a(ctx, "direction").b(ctx.getSource());
            ap.fakeEntityPlayer.yaw = rotation.j;
            ap.fakeEntityPlayer.pitch = rotation.i;
        });
    }

    private static int lookAtBlock(CommandContext<CommandListenerWrapper> ctx) {
        return manipulate(ctx, ap -> {
            try {
                ap.fakeEntityPlayer.a/* lookAt */(ArgumentAnchor.Anchor.EYES, ArgumentVec3.a(ctx, "blockpos"));
            } catch (CommandSyntaxException ignored) {
            }
        });
    }

    private static int actions(CommandContext<CommandListenerWrapper> ctx) {
        return manipulate(ctx, ap -> {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Activated actions: ").color(ChatColor.GOLD)
                    .create()
            );
            for (FakeEntityPlayerActionPack.Action action : ap.getActivatedActions())
                ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                        .append(action.toString()).color(ChatColor.AQUA)
                        .create()
                );
        });
    }

    private static int manipulate(CommandContext<CommandListenerWrapper> ctx, Consumer<FakeEntityPlayerActionPack> consumer) {
        if (!isFakePlayer(ctx)) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Only fake players can manipulate").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        FakeEntityPlayer player = getFakeEntityPlayer(ctx);
        consumer.accept(player.actionPack);
        return 1;
    }

    private static FakeEntityPlayer getFakeEntityPlayer(CommandContext<CommandListenerWrapper> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        final CraftPlayer bukkitPlayer = (CraftPlayer) Bukkit.getPlayerExact(playerName);
        assert bukkitPlayer != null;
        return (FakeEntityPlayer) bukkitPlayer.getHandle();
    }

    public void shutdown() {
        executor.shutdownNow();
        for (String playerName : getPlayers()) {
            final Player playerExact = Bukkit.getPlayerExact(playerName);
            if (playerExact != null)
                playerExact.kickPlayer("Shutdown");
        }
    }

    private static int kill(CommandContext<CommandListenerWrapper> ctx) {
        if (!isFakePlayer(ctx)) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Only fake players can be killed").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        FakeEntityPlayer player = getFakeEntityPlayer(ctx);
        player.killEntity();
        return 1;
    }

    private static int spawnAsync(CommandContext<CommandListenerWrapper> ctx) {
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
        executor.execute(() -> {
            if (server.getUserCache().getProfileIfCached(playerName) != null
                    || server.getUserCache().getProfile(playerName) != null)
                server.execute(() -> spawn(ctx));
            else
                ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                        .append("Player not found").color(ChatColor.RED).bold(true).append("").reset()
                        .create()
                );
        });
        ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                .append("Command queued, please wait... ").color(ChatColor.GREEN).bold(true).append("").reset()
                .create()
        );
        return 1;
    }

    private static int spawn(CommandContext<CommandListenerWrapper> ctx) {
        if (!canSpawn(ctx)) return 0;
        String playerName = StringArgumentType.getString(ctx, "player");
        Preconditions.checkNotNull(playerName);
        Preconditions.checkArgument(!playerName.isEmpty());
        if (playerName.length() > 16) {
            ctx.getSource().getBukkitSender().sendMessage(new ComponentBuilder()
                    .append("Player name ").color(ChatColor.RED).append("").reset()
                    .append(playerName).color(ChatColor.RED).bold(true).append("").reset()
                    .append(" is longer than 16 characters").color(ChatColor.RED).append("").reset()
                    .create()
            );
            return 0;
        }
        final DedicatedServer server = ((CraftServer) Bukkit.getServer()).getHandle().getServer();
        final WorldServer worldServer =
                ((CraftWorld) Objects.requireNonNull(ctx.getSource().getBukkitLocation()).getWorld()).getHandle();
        EnumGamemode gameMode = EnumGamemode.SURVIVAL;
        Vec3D location = ctx.getSource().getPosition();
        Vec2F rotation = ctx.getSource().i();
        assert location != null;
        FakeEntityPlayer entityPlayer = FakeEntityPlayer.createFake(
                playerName,
                server,
                location.getX(),
                location.getY(),
                location.getZ(),
                rotation.j,
                rotation.i,
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

    private static boolean canSpawn(CommandContext<CommandListenerWrapper> ctx) {
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
        GameProfile profile = server.getUserCache().getProfileIfCached(playerName);
        if (profile == null) profile = server.getUserCache().getProfile(playerName);
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

    private static boolean isFakePlayer(CommandContext<CommandListenerWrapper> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return false;
        return ((CraftPlayer) player).getHandle() instanceof FakeEntityPlayer;
    }

}
