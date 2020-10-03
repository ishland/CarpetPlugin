package com.ishland.bukkit.carpetplugin.lib.fakeplayer.base;

import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;

import java.net.SocketAddress;

public class FakeChannel extends AbstractChannel {

    private boolean isOpen = true;

    protected FakeChannel() {
        super(null);
    }

    @Override
    protected AbstractChannel.AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress socketAddress, SocketAddress socketAddress1, ChannelPromise channelPromise) {
            }
        };
    }

    @Override
    protected boolean isCompatible(EventLoop eventLoop) {
        return false;
    }

    @Override
    protected SocketAddress localAddress0() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "CarpetPlugin";
            }
        };
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "CarpetPlugin";
            }
        };
    }

    @Override
    protected void doBind(SocketAddress socketAddress) {

    }

    @Override
    protected void doDisconnect() {
        isOpen = false;
    }

    @Override
    protected void doClose() {
        isOpen = false;
    }

    @Override
    protected void doBeginRead() {

    }

    @Override
    protected void doWrite(ChannelOutboundBuffer channelOutboundBuffer) {

    }

    @Override
    public ChannelConfig config() {
        return new DefaultChannelConfig(this);
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isActive() {
        return isOpen;
    }

    @Override
    public ChannelMetadata metadata() {
        return new ChannelMetadata(false);
    }

    @Override
    public ChannelFuture close() {
        doDisconnect();
        return null;
    }
}