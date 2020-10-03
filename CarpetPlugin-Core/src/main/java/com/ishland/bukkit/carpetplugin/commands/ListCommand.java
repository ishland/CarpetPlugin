package com.ishland.bukkit.carpetplugin.commands;

import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.minecraft.server.ChatMessage;
import net.minecraft.server.CommandListenerWrapper;
import net.minecraft.server.DedicatedPlayerList;
import net.minecraft.server.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftChatMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.literal;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.sendMessage;

public class ListCommand {

    public void register() {
        final CommandDispatcher<CommandListenerWrapper> commandDispatcher =
                ((CraftServer) Bukkit.getServer()).getHandle().getServer()
                        .getCommandDispatcher().a();
        commandDispatcher.register(literal("list")
                .then(literal("carpet")
                        .then(literal("removeOrphan")
                                .executes(ListCommand::removeOrphan)
                        )
                        .executes(ListCommand::listCarpet)
                )
        );
    }

    private static int removeOrphan(CommandContext<CommandListenerWrapper> ctx) {
        DedicatedPlayerList playerList = ((CraftServer) Bukkit.getServer()).getServer().getPlayerList();
        List<EntityPlayer> players =
                new CopyOnWriteArrayList<>(playerList.players);
        for (EntityPlayer player : players)
            if (playerList.getPlayer(player.getName()) != player) {
                ctx.getSource().getBukkitSender().sendMessage(
                        "Trying to get rid of " + player.getName()
                                + " (" + player.getClass().getName() + "), " +
                                "(UUID of " + player.getUniqueID() + ")");
                if (player.networkManager != null) try {
                    player.networkManager.j().a(new ChatMessage("Remove orphan"));
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                try {
                    String disconnectMessage = playerList.disconnect(player);
                    if(disconnectMessage != null && disconnectMessage.length() > 0)
                        playerList.sendMessage(CraftChatMessage.fromString(disconnectMessage));
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
                playerList.players.remove(player);
            }
        return 1;
    }

    private static int listCarpet(CommandContext<CommandListenerWrapper> ctx) {
        DedicatedPlayerList playerList = ((CraftServer) Bukkit.getServer()).getServer().getPlayerList();
        List<EntityPlayer> players =
                new CopyOnWriteArrayList<>(playerList.players);
        sendMessage(ctx, new ComponentBuilder()
                .append("There are ").append(String.valueOf(players.size())).append(" of a max of ")
                .append(String.valueOf(playerList.getMaxPlayers())).append(" players online: ")
                .create());
        for (EntityPlayer player : players) {
            boolean isPossibleOrphan = playerList.getPlayer(player.getName()) != player;
            sendMessage(ctx, new ComponentBuilder()
                    .append(
                            isPossibleOrphan
                                    ? "[Possible Orphan player] "
                                    : ""
                    ).color(ChatColor.RED)
                    .append(
                            player instanceof FakeEntityPlayer
                                    ? "CarpetPlugin player "
                                    : "Others (" + player.getClass().getName() + ") "
                    )
                    .append(player.getName())
                    .append("(UUID of " + player.getUniqueID().toString() + ")")
                    .create()
            );
        }
        return 1;
    }

}
