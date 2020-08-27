package com.ishland.bukkit.carpetplugin.fakes;

import net.minecraft.server.v1_16_R2.EnumProtocolDirection;
import net.minecraft.server.v1_16_R2.NetworkManager;

public class FakeNetworkManager extends NetworkManager {
    public FakeNetworkManager(EnumProtocolDirection enumprotocoldirection) {
        super(enumprotocoldirection);
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void stopReading() {
    }
}
