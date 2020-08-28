package com.ishland.bukkit.carpetplugin.fakes;

import com.google.common.base.Suppliers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.v1_16_R2.*;

import java.lang.reflect.Field;
import java.net.SocketAddress;
import java.util.function.Supplier;

public class FakeNetworkManager extends NetworkManager {

    private static final Supplier<Field> networkManager$packetListener = Suppliers.memoize(() -> {
        try {
            final Field packetListener = NetworkManager.class.getDeclaredField("packetListener");
            packetListener.setAccessible(true);
            return packetListener;
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new RuntimeException(noSuchFieldException);
        }
    });

    public FakeNetworkManager(EnumProtocolDirection enumprotocoldirection) {
        super(enumprotocoldirection);
    }

    @Override
    public SocketAddress getSocketAddress() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "CarpetPlugin";
            }
        };
    }

    @Override
    public void sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener) {
        try {
            if (packet instanceof PacketPlayOutKeepAlive) {
                PacketPlayOutKeepAlive packetPlayOutKeepAlive = (PacketPlayOutKeepAlive) packet;
                ByteBuf writeBuffer = Unpooled.buffer(4);
                PacketDataSerializer serializer = new PacketDataSerializer(writeBuffer);
                packetPlayOutKeepAlive.b(serializer);
                final ByteBuf readBuffer = writeBuffer.retain();
                final PacketPlayInKeepAlive packetPlayInKeepAlive = new PacketPlayInKeepAlive();
                packetPlayInKeepAlive.a(new PacketDataSerializer(readBuffer));
                readBuffer.release();
                writeBuffer.release();
                getFakePlayerConnection().a(packetPlayInKeepAlive);
            }
        } catch (Throwable t) {
            CrashReport crashReport = new CrashReport("Handling fake player packet", t);
            crashReport.a("Packet being handled: ")
                    .a("Packet: ", () -> String.valueOf(packet));
            throw new ReportedException(crashReport);
        }
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void stopReading() {
    }

    private FakePlayerConnection getFakePlayerConnection() {
        try {
            return (FakePlayerConnection) networkManager$packetListener.get().get(this);
        } catch (IllegalAccessException illegalAccessException) {
            throw new RuntimeException(illegalAccessException);
        }
    }

}
