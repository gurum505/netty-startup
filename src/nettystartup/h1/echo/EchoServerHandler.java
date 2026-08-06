// modified from io.netty.example.echo
package nettystartup.h1.echo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
class EchoServerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // TODO: [실습1-2] 받은대로 응답하는 코드를 한 줄 작성합니다. release는 필요하지 않습니다.
        //Netty 핸들러에서 ctx.write(buf)를 호출하면, 서버가 보낸 데이터(Outbound)는 자동으로 EmbeddedChannel의 Outbound 버퍼에 쌓이게 됩니다.
        //이때 Netty 내부적으로 ByteBuf의 레퍼런스 카운트(RefCnt)가 1 증가합니다. (소유권이 채널로 넘어감)

        ByteBuf buf = (ByteBuf) msg;
        ctx.write(buf);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
