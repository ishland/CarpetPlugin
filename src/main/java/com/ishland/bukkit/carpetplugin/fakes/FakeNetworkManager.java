package com.ishland.bukkit.carpetplugin.fakes;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.server.v1_16_R2.EnumProtocolDirection;
import net.minecraft.server.v1_16_R2.NetworkManager;
import net.minecraft.server.v1_16_R2.Packet;

import java.net.SocketAddress;

public class FakeNetworkManager extends NetworkManager {
    public FakeNetworkManager(EnumProtocolDirection enumprotocoldirection) {
        super(enumprotocoldirection);
    }

    @Override
    public SocketAddress getSocketAddress() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "local";
            }
        };
    }

    @Override
    public void sendPacket(Packet<?> packet) {
    }

    @Override
    public void sendPacket(Packet<?> packet, GenericFutureListener<? extends Future<? super Void>> genericFutureListener) {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void stopReading() {
    }
}
