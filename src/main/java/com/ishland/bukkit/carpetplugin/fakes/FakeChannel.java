package com.ishland.bukkit.carpetplugin.fakes;

import io.netty.channel.*;

import java.net.SocketAddress;

public class FakeChannel extends AbstractChannel {
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
                return "local";
            }
        };
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return new SocketAddress() {
            @Override
            public String toString() {
                return "local";
            }
        };
    }

    @Override
    protected void doBind(SocketAddress socketAddress) {

    }

    @Override
    protected void doDisconnect() {

    }

    @Override
    protected void doClose() {

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
        return true;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public ChannelMetadata metadata() {
        return new ChannelMetadata(false);
    }
}