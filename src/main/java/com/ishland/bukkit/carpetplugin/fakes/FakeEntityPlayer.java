package com.ishland.bukkit.carpetplugin.fakes;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.v1_16_R2.*;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;

public class FakeEntityPlayer extends EntityPlayer {

    public Runnable fixStartingPosition = () -> {
    };

    @Nullable
    public static FakeEntityPlayer createFake(String username,
                                              MinecraftServer server,
                                              double x,
                                              double y,
                                              double z,
                                              float yaw,
                                              float pitch,
                                              WorldServer world,
                                              EnumGamemode gameMode) {
        PlayerInteractManager interactManager = new PlayerInteractManager(world);
        GameProfile gameProfile = server.getUserCache().getProfile(username);
        if (gameProfile == null) return null;
        if (gameProfile.getProperties().containsKey("textures")) {
            try {
                gameProfile = TileEntitySkull.b(gameProfile, (gameProfile1) -> true, true).get(); // loadProperties
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        FakeEntityPlayer instance = new FakeEntityPlayer(server, world, gameProfile, interactManager);
        instance.fixStartingPosition = () -> instance.setLocation(x, y, z, yaw, pitch);
        server.getPlayerList().a(new FakeNetworkManager(EnumProtocolDirection.SERVERBOUND), instance); // onPlayerConnect
        instance.a(world, x, y, z, yaw, pitch); // teleport
        instance.setHealth(20.0F);
        instance.shouldBeRemoved = false; // removed
        instance.G = 0.6F; // stepHeight
        interactManager.setGameMode(gameMode);
        server.getPlayerList().a( // sendToDimension
                new PacketPlayOutEntityHeadRotation(instance, (byte) (instance.yaw * 256 / 360)),
                world.getDimensionKey()
        );
        server.getPlayerList().a( // sendToDimension
                new PacketPlayOutEntityTeleport(instance), world.getDimensionKey()
        );
        instance.getWorldServer().chunkProvider.movePlayer(instance);
        instance.datawatcher.set(bi /* PLAYER_MODEL_PARTS */, (byte) 0x7f);
        return instance;
    }

    public FakeEntityPlayer(MinecraftServer minecraftserver, WorldServer worldserver, GameProfile gameprofile, PlayerInteractManager playerinteractmanager) {
        super(minecraftserver, worldserver, gameprofile, playerinteractmanager);
    }

    @Override
    public void killEntity() {
        this.server.a(new TickTask(this.server.ah(), () -> {
            this.networkManager
                    .j() // getNetworkHandler
                    .a(new ChatMessage("Killed")); // onDisconnected
        }));
    }

    @Override
    public void tick() {
        if (this.server.ah()/* getTicks */ % 10 == 0) {
            ((PlayerConnection) this.networkManager.j()/* getNetworkHandler */).syncPosition();
            this.getWorldServer().chunkProvider.movePlayer(this);
        }
        super.tick();
        this.playerTick();
    }

    @Override
    public void die(DamageSource damagesource) {
        super.die(damagesource);
        setHealth(20.0F);
        this.foodData = new FoodMetaData();
        killEntity();
    }
}
