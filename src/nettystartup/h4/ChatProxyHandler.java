package nettystartup.h4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import nettystartup.h3.ChatMessage;
import nettystartup.h3.ChatMessageCodec;

// 도전과제: 시간이 더 있다면, 세번째 시간에 개발한 채팅 서버에 TCP 연결로 프록시 처리를 하는 웹소켓 채팅서비스를 만들어봅시다.
// 이 클래스는 양방향으로 재사용된다 — "내가 붙어있는 채널로 ChatMessage가 들어오면, 생성자로 받은 다른 채널에 그대로 릴레이한다."
class ChatProxyHandler extends SimpleChannelInboundHandler<ChatMessage> {
    private final Channel wsChannel;

    public ChatProxyHandler(Channel wsChannel) {
        this.wsChannel = wsChannel;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatMessage msg) throws Exception {
        wsChannel.writeAndFlush(msg);
    }

    // 프록시가 h3 ChatServer에 TCP 클라이언트로 접속하고, 양방향 릴레이를 배선한다.
    // WebChatHandler.handlerAdded()에서 호출하므로 인스턴스 상태가 필요 없어 static으로 둔다.
    public static void connectToChatServer(Channel browserChannel) {
        Bootstrap b = new Bootstrap();
        b.group(browserChannel.eventLoop())   // 새 EventLoopGroup을 안 만들고 browserChannel의 EventLoop를 재사용
                .channel(NioSocketChannel.class)   // 클라이언트(connect) 채널이므로 NioServerSocketChannel이 아니라 NioSocketChannel
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // h3의 ChatServer.java 파이프라인을 그대로 재현 + h3->h4 방향 릴레이 핸들러
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new LineBasedFrameDecoder(1024, true, true))
                                .addLast(new StringDecoder(CharsetUtil.UTF_8), new StringEncoder(CharsetUtil.UTF_8))
                                .addLast(new ChatMessageCodec(), new LoggingHandler(LogLevel.INFO))
                                .addLast(new ChatProxyHandler(browserChannel));   // h3 -> h4(브라우저) 방향
                    }
                });

        b.connect("localhost", 8030)
                .addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        Channel h3Channel = future.channel();
                        // h4(브라우저) -> h3 방향: browserChannel 파이프라인에 반대쪽 릴레이 핸들러를 추가
                        browserChannel.pipeline().addAfter("wsChatCodec", "chatRelay",
                                new ChatProxyHandler(h3Channel));
                    } else {
                        future.cause().printStackTrace();
                        browserChannel.close();
                    }
                });
    }
}
