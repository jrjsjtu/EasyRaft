package EasyRaft.state;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

import java.nio.ByteBuffer;

/**
 * Created by jrj on 17-11-7.
 */
public class RaftLog {
    long term;long index;
    String log;
    Object append;

    public RaftLog(long term,long index,String log){
        this.term = term;this.index = index;this.log = log;
    }

    public RaftLog(String log,Object ctx){
        this.log = log;
        this.append = ctx;
    }

    public void setTerm(long term) {
        this.term = term;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public void sendResponse(){
        if (append != null && append instanceof ChannelHandlerContext){
            ByteBuf byteBuf = Unpooled.buffer();
            byteBuf.writeInt(7);
            byteBuf.writeBytes("success".getBytes());
            ((ChannelHandlerContext)append).writeAndFlush(byteBuf);
        }else if(append instanceof Runnable){
            ((Runnable)append).run();
        }
        //让GC回收Runnable
        append = null;
    }


    public long getTerm(){return term;}
    public long getIndex(){return index;}
    //public byte[] getLog(){return log.getBytes();}
    public String getLog(){
        return log;
    }

    public boolean asOrMoreUpToDate(long lastLogIndex,long lastLogTerm){
        if (lastLogTerm == term){
            return index>=lastLogIndex;
        }else{
            return term>lastLogIndex;
        }
    }

    public boolean moreUpToDate(long lastLogIndex,long lastLogTerm){
        if (lastLogTerm == term){
            return index>lastLogIndex;
        }else{
            return term>lastLogIndex;
        }
    }
}
