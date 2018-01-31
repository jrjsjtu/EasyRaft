package EasyRaft.ServerIO;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by jrj on 18-1-27.
 */
public class CurrentLeader {
    public final static CurrentLeader nullLeader = new CurrentLeader(-1,"null",null);
    int epoch;
    String address;
    ChannelHandlerContext ctx;
    public CurrentLeader(int epoch,String address,ChannelHandlerContext ctx){
        this.epoch = epoch;
        this.address = address;
        this.ctx = ctx;
    }
}
