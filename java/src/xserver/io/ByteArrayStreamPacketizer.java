package xserver.io;

import xserver.api.*;

public class ByteArrayStreamPacketizer extends StreamPacketizer<byte[]>{
   public ByteArrayStreamPacketizer(StreamConnection<byte[]> parent){
      super(parent);
   }
   protected byte readByte(byte[] src, int off){
      return src[off];
   }
   protected void read(byte[] src, int srcOff, DataBuffer dst, int len){
      dst.append(src, srcOff, len);
   }
}
