package KV.KVDatabase;

import EasyRaft.client.CtxProxy;
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
        ServerConfig serverConfig = null;
        try {
            serverConfig = new ServerConfig(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }

        String appendInfo = serverConfig.getserverPort() + ":" + serverConfig.getshard();
        CtxProxy ctxProxy = new CtxProxy("127.0.0.1", appendInfo);
        ctxProxy.setCallBackClass(KVServerCallBack.class);
        ArrayList<String> strings = serverConfig.getIpPortList();

        for(String tmp:strings){
            String[] infos = tmp.split(":");
            try {
                ctxProxy.tryToConnect(infos[0],Integer.parseInt(infos[1]));
            } catch (Exception e) {
                System.out.println(infos + " connect failed");
            }
        }

        Server kvServer;

        try{
            kvServer = new Server(serverConfig.getserverPort());
        }catch (Exception e){
            System.out.println("?????");
            e.printStackTrace();
        }
         /*
        int idx = 1;
        int serverPort = 40000+idx;
        RaftClient raftClient = new RaftClient(new KVServerCallBack());

        raftClient.joinRaft();
        int splitPos = raftClient.getSelfAddress().indexOf(':');
        String address = raftClient.getSelfAddress().substring(0,splitPos);
        //idx代表slot的位置,ServerPort代表作为KV的服务端口号

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(address).append(':');
        stringBuilder.append(serverPort).append(':');
        stringBuilder.append(idx).append(':');

        raftClient.electLeader(stringBuilder.toString());

        raftClient.joinCLuster(stringBuilder.toString());
        Server kvServer;
        try{
            kvServer = new Server(serverPort);
        }catch (Exception e){
            e.printStackTrace();
        }
        */
    }
}
