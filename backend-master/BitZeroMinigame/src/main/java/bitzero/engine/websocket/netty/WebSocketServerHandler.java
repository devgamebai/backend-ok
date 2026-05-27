/*
 * Decompiled with CFR 0_116.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang.exception.ExceptionUtils
 *  org.jboss.netty.buffer.ChannelBuffer
 *  org.jboss.netty.buffer.ChannelBuffers
 *  org.jboss.netty.channel.Channel
 *  org.jboss.netty.channel.ChannelFuture
 *  org.jboss.netty.channel.ChannelFutureListener
 *  org.jboss.netty.channel.ChannelHandlerContext
 *  org.jboss.netty.channel.ChannelStateEvent
 *  org.jboss.netty.channel.ExceptionEvent
 *  org.jboss.netty.channel.MessageEvent
 *  org.jboss.netty.channel.SimpleChannelUpstreamHandler
 *  org.jboss.netty.handler.codec.http.DefaultHttpResponse
 *  org.jboss.netty.handler.codec.http.HttpHeaders
 *  org.jboss.netty.handler.codec.http.HttpMessage
 *  org.jboss.netty.handler.codec.http.HttpMethod
 *  org.jboss.netty.handler.codec.http.HttpRequest
 *  org.jboss.netty.handler.codec.http.HttpResponse
 *  org.jboss.netty.handler.codec.http.HttpResponseStatus
 *  org.jboss.netty.handler.codec.http.HttpVersion
 *  org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame
 *  org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
 *  org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
 *  org.jboss.netty.util.CharsetUtil
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package bitzero.engine.websocket.netty;

import bitzero.engine.config.EngineConfiguration;
import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.core.IEngineAcceptor;
import bitzero.engine.core.security.IConnectionFilter;
import bitzero.engine.data.IPacket;
import bitzero.engine.data.Packet;
import bitzero.engine.data.TransportType;
import bitzero.engine.exceptions.RefusedAddressException;
import bitzero.engine.io.IProtocolCodec;
import bitzero.engine.io.protocols.ProtocolType;
import bitzero.engine.service.IService;
import bitzero.engine.sessions.ISession;
import bitzero.engine.sessions.ISessionManager;
import bitzero.engine.websocket.IWebSocketChannel;
import bitzero.engine.websocket.WebSocketService;
import bitzero.engine.websocket.WebSocketStats;
import bitzero.server.BitZeroServer;
import bitzero.server.entities.User;
import bitzero.server.entities.managers.IUserManager;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketServerHandler
extends SimpleChannelUpstreamHandler
implements IWebSocketChannel {
    private static final ChannelBuffer HTTP_INDEX_BUFFER = ChannelBuffers.copiedBuffer((CharSequence)"<html><header><title>SFS2X Websocket</title></header><body><h3>{ SFS2X Websocket Index Page\u00c2\u00a0}</h3></body></html>", (Charset)CharsetUtil.UTF_8);
    public static final String WEBSOCKET_PATH = "/websocket";
    private final BitZeroEngine engine;
    private final BitZeroServer sfs;
    private final ISessionManager sessionManager;
    private final IProtocolCodec codec;
    private final Logger logger;
    private final WebSocketService webSocketService;
    private WebSocketServerHandshaker handshaker;
    private final WebSocketStats webSocketStats;
    private final int MAX_REQ_SIZE;
    private final IConnectionFilter connFilter;
    private final boolean isSSL;
    private final boolean isAutoBahnTest;
    private final Executor systemThreadPool;
    private ISession sfsSession;
    private Channel wsChannel;

    public WebSocketServerHandler(IProtocolCodec codec, boolean isSSL, boolean isAutoBahnTest) {
        this.codec = codec;
        this.isSSL = isSSL;
        this.isAutoBahnTest = isAutoBahnTest;
        this.engine = BitZeroEngine.getInstance();
        this.sfs = BitZeroServer.getInstance();
        this.connFilter = this.engine.getEngineAcceptor().getConnectionFilter();
        this.systemThreadPool = this.sfs.getSystemThreadPool();
        this.sessionManager = this.engine.getSessionManager();
        this.MAX_REQ_SIZE = this.engine.getConfiguration().getMaxIncomingRequestSize();
        this.logger = LoggerFactory.getLogger((String)"WebSocketServerHandler");
        this.webSocketService = (WebSocketService)this.engine.getServiceByName("webSocketEngine");
        this.webSocketStats = this.webSocketService.getWebSocketStats();
    }

    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        try {
            this.wsChannel = ctx.getChannel();
            String address = this.wsChannel.getRemoteAddress().toString();
            this.connFilter.validateAndAddAddress(address.substring(1, address.indexOf(58)));
            this.sfsSession = this.sessionManager.createWebSocketSession(this);
            this.sessionManager.addSession(this.sfsSession);
        }
        catch (RefusedAddressException error) {
            this.logger.warn("Refused connection. " + error.getMessage());
            ctx.getChannel().close();
            this.logger.warn(ExceptionUtils.getStackTrace((Throwable)error));
        }
        catch (Exception ex) {
            this.logger.warn("Refused connection. " + ex.getMessage());
            ctx.getChannel().close();
            this.logger.warn(ExceptionUtils.getStackTrace((Throwable)ex));
        }
    }

    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (this.sfsSession == null) {
            return;
        }
        this.sfsSession.setConnected(false);
        this.sfs.getSessionManager().onSocketDisconnected(this.sfsSession);
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            this.handleHttpRequest(ctx, (HttpRequest)msg);
        } else if (msg instanceof WebSocketFrame) {
            this.handleWebSocketFrame(ctx, (WebSocketFrame)msg);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
        if (req.getMethod() != HttpMethod.GET) {
            this.sendHttpResponse(ctx, req, (HttpResponse)new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }
        if ("/".equals(req.getUri())) {
            DefaultHttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            res.headers().set("Content-Type", (Object)"text/html; charset=UTF-8");
            HttpHeaders.setContentLength((HttpMessage)res, (long)HTTP_INDEX_BUFFER.readableBytes());
            res.setContent(HTTP_INDEX_BUFFER);
            this.sendHttpResponse(ctx, req, (HttpResponse)res);
            return;
        }
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(this.getWebSocketLocation(req), "default-protocol", false);
        this.handshaker = wsFactory.newHandshaker(req);
        if (this.handshaker == null) {
            this.logger.info("Websocket Unsupport Version");
            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
        } else {
            this.logger.info("Websocket Handshake");
            this.handshaker.handshake(ctx.getChannel(), req).addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
        }
    }

    private void handleAutoBahnTest(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof TextWebSocketFrame) {
            String textRequest = ((TextWebSocketFrame)frame).getText();
            ctx.getChannel().write((Object)new TextWebSocketFrame(textRequest));
            return;
        }
        if (frame instanceof BinaryWebSocketFrame) {
            ctx.getChannel().write((Object)new BinaryWebSocketFrame(frame.getBinaryData()));
            return;
        }
        if (frame instanceof ContinuationWebSocketFrame) {
            this.logger.info("Should not receive continuation frame here");
            return;
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        this.logger.debug(String.format("Channel %s received %s", ctx.getChannel().getId(), frame.getClass().getSimpleName()));
        if (frame instanceof CloseWebSocketFrame) {
            this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame)frame);
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write((Object)new PongWebSocketFrame(frame.getBinaryData()));
            return;
        }
        if (this.isAutoBahnTest) {
            this.handleAutoBahnTest(ctx, frame);
        } else {
            if (frame instanceof TextWebSocketFrame) {
                this.webSocketStats.addDroppedInFrame();
                return;
            }
            if (frame instanceof ContinuationWebSocketFrame) {
                this.logger.info("Should not receive continuation frame here");
                return;
            }
            if (frame instanceof BinaryWebSocketFrame) {
                ChannelBuffer rawRequest = ((BinaryWebSocketFrame)frame).getBinaryData();
                ByteBuffer bf = rawRequest.toByteBuffer();
                int reqLen = bf.capacity();
                if (reqLen > this.MAX_REQ_SIZE) {
                    User uu = this.sfs.getUserManager().getUserBySession(this.sfsSession);
                    Object[] arrobject = new Object[2];
                    arrobject[0] = reqLen > 10240 ? "" + reqLen / 1024 + "KB" : Integer.valueOf(reqLen);
                    arrobject[1] = uu != null ? uu : this.sfsSession;
                    this.logger.warn(String.format("Refused WebSocket request. Too large: %s, From: %s ", arrobject));
                    this.webSocketStats.addDroppedInPacket();
                    return;
                }
                this.webSocketStats.addReadBytes(reqLen);
                this.webSocketStats.addReadPackets(1);
                Packet packet = new Packet();
                packet.setData(bf);
                packet.setSender(this.sfsSession);
                packet.setOriginalSize(bf.capacity());
                packet.setTransportType(TransportType.TCP);
                packet.setAttribute("type", (Object)ProtocolType.BINARY);
                this.systemThreadPool.execute(new WSIOExecutor(packet));
            } else {
                this.webSocketStats.addDroppedInFrame();
                throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
            }
        }
    }

    private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req, HttpResponse res) {
        if (res.getStatus().getCode() != 200) {
            res.setContent(ChannelBuffers.copiedBuffer((CharSequence)res.getStatus().toString(), (Charset)CharsetUtil.UTF_8));
            HttpHeaders.setContentLength((HttpMessage)res, (long)res.getContent().readableBytes());
        }
        ChannelFuture f = ctx.getChannel().write((Object)res);
        if (!HttpHeaders.isKeepAlive((HttpMessage)req) || res.getStatus().getCode() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ClosedChannelException) {
            return;
        }
        this.logger.warn(e.getCause().toString());
        if (this.wsChannel.isOpen()) {
            this.wsChannel.close();
        }
    }

    private String getWebSocketLocation(HttpRequest req) {
        return (this.isSSL ? "wss://" : "ws://") + req.headers().get("Host") + "/websocket";
    }

    @Override
    public void close() {
        this.wsChannel.close();
    }

    @Override
    public void write(ChannelBuffer message) {
        this.logger.info("Should Not Reach This Method While Testing with AutoBahn");
        this.wsChannel.write((Object)new BinaryWebSocketFrame(message));
    }

    @Override
    public SocketAddress getLocalAddress() {
        return this.wsChannel.getLocalAddress();
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return this.wsChannel.getRemoteAddress();
    }

    private final class WSIOExecutor
    implements Runnable {
        private final IPacket packet;

        public WSIOExecutor(IPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            WebSocketServerHandler.this.codec.onPacketRead(this.packet);
        }
    }

}

