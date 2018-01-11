package KV.KVDatabase;

import EasyRaft.client.RaftClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.ArrayList;

/**
 * Created by jrj on 17-12-23.
 */
public class Server {
    private static int[] kvPorts = new int[]{10200,10201};
     public Server(int port){
         EventLoopGroup bossGroup = new NioEventLoopGroup();
         EventLoopGroup workerGroup = new NioEventLoopGroup(1);
         ServerBootstrap b = new ServerBootstrap();
         b.group(bossGroup,workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG,64).
                 childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new RequestDecoder());
                        ch.pipeline().addLast(new RequestHandler());
                    }
                 });

         try {
             b.bind(port).sync().channel().closeFuture().sync();
         } catch (InterruptedException e) {
             e.printStackTrace();
         }
     }
    public static void main(String[] args){
        RaftClient raftClient = new RaftClient();
        Server kvServer;
        try{
            raftClient.joinCluster("aaa");
            ArrayList<String> memberList = raftClient.getMemberList();
            String localAddress = raftClient.getLocalAddress().substring(1);
            for (int i=0;i<memberList.size();i++){
                if (memberList.get(i).equals(localAddress)){
                    kvServer = new Server(kvPorts[i]);
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
