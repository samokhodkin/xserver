package xserver.io;

import java.nio.*;
import xserver.api.*;

public class ByteBufferStreamPacketizer extends StreamPacketizer<ByteBuffer>{
   public ByteBufferStreamPacketizer(StreamConnection<ByteBuffer> parent){
      super(parent);
   }
   protected byte readByte(ByteBuffer src, int off){
      return src.get(off);
   }
   protected void read(ByteBuffer src, int srcOff, DataBuffer dst, int len){
      dst.append(src, srcOff, len);
   }
}
