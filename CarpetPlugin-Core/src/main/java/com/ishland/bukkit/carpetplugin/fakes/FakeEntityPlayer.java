package com.ishland.bukkit.carpetplugin.fakes;

import com.google.common.base.Suppliers;
import com.google.common.base.Throwables;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class FakeEntityPlayer extends EntityPlayer {

    @SuppressWarnings("unchecked")
    private static final Supplier<Queue<NetworkManager>> serverConnection$pending = Suppliers.memoize(() -> {
        try {
            final ServerConnection serverConnection =
                    ((CraftServer) Bukkit.getServer()).getHandle().getServer().getServerConnection();
            assert serverConnection != null;
            final Field pendingField = serverConnection.getClass().getDeclaredField("pending");
            pendingField.setAccessible(true);
            return (Queue<NetworkManager>) pendingField.get(serverConnection);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    });

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
        try {
            PlayerInteractManager interactManager = new PlayerInteractManager(world);
            GameProfile gameProfile = server.getUserCache().getProfileIfCached(username);
            if(gameMode == null) gameProfile = server.getUserCache().getProfile(username);
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
            final FakeNetworkManager networkManager = new FakeNetworkManager(EnumProtocolDirection.SERVERBOUND);

            // Additional things to get networkManager ticking
            networkManager.channel = new FakeChannel();

            serverConnection$pending.get().add(networkManager);

            server.getPlayerList().a(networkManager, instance); // onPlayerConnect
            final FakePlayerConnection fakePlayerConnection = new FakePlayerConnection(
                    instance.playerConnection,
                    server,
                    instance.networkManager,
                    instance);
            instance.networkManager.setPacketListener(fakePlayerConnection);
            instance.playerConnection = fakePlayerConnection;

            instance.playerConnection.tick();
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
            instance.datawatcher.set(EntityHuman.bi /* PLAYER_MODEL_PARTS */, (byte) 0x7f);
            return instance;
        } catch (Throwable t) {
            Throwables.throwIfUnchecked(t);
            CrashReport crashReport = new CrashReport("Spawning fake player", t);
            crashReport.a("Player being spawned: ")
                    .a("Player Name: ", username)
                    .a("Player World: ", world);
            throw new ReportedException(crashReport);
        }
    }

    public FakeEntityPlayer(MinecraftServer minecraftserver, WorldServer worldserver, GameProfile gameprofile, PlayerInteractManager playerinteractmanager) {
        super(minecraftserver, worldserver, gameprofile, playerinteractmanager);
    }

    @Override
    public void killEntity() {
        this.server.a(new TickTask(this.server.ah(), () -> {
            this.networkManager.channel.close();
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
        this.foodData = new FoodMetaData(this);
        killEntity();
    }

    @Override
    public void load(NBTTagCompound nbttagcompound) {
        super.load(nbttagcompound);
        this.server.execute(fixStartingPosition);
    }

}
