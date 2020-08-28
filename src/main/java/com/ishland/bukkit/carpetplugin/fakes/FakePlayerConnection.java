package com.ishland.bukkit.carpetplugin.fakes;

import com.google.common.base.Suppliers;
import net.minecraft.server.v1_16_R2.*;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public class FakePlayerConnection extends PlayerConnection {

    private static final Supplier<Field> playerConnection$playerJoinReady = Suppliers.memoize(() ->
    {
        try {
            final Field playerJoinReady = PlayerConnection.class.getDeclaredField("playerJoinReady");
            playerJoinReady.setAccessible(true);
            return playerJoinReady;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    });

    private final PlayerConnection parent;

    public FakePlayerConnection(PlayerConnection parent, MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayer entityplayer) {
        super(minecraftserver, networkmanager, entityplayer);
        this.parent = parent;
    }

    @Override
    public void tick() {
        try {
            final Runnable playerJoinReady = (Runnable) playerConnection$playerJoinReady.get().get(parent);
            if(playerJoinReady != null){
                playerJoinReady.run();
                playerConnection$playerJoinReady.get().set(parent, null);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        super.tick();
    }

    @Override
    public void sendPacket(Packet<?> packet) {
    }

    @Override
    public void disconnect(String s) {
        player.killEntity();
    }
}
