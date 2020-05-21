package xserver.io;

import xserver.api.*;
import xserver.io.DataBuffer;
import xserver.test.*;

/* 
 * Converts parametric stream connection to byte array packet connection
 */

public abstract class StreamPacketizer<T>
   implements PacketConnection<byte[]>, StreamConnection.Listener<T>
{
   protected abstract byte readByte(T src, int off);
   protected abstract void read(T src, int srcOff, DataBuffer dst, int len);
   
   private final StreamConnection<T> parent;
   private Listener<byte[]> listener;
   private int remaining=-1; //next packet length; -1 unknown
   private final DataBuffer buffer=new DataBuffer();
   private final byte[] header=new byte[4];
   
   public StreamPacketizer(StreamConnection<T> parent){
      this.parent=parent;
      parent.setListener(this);
   }
   
   public String remoteAddress(){
      return parent.remoteAddress();
   }
   public String localAddress(){
      return parent.localAddress();
   }
   public int remotePort(){
      return parent.remotePort();
   }
   public int localPort(){
      return parent.localPort();
   }
   public boolean isAlive(){
      return parent.isAlive();
   }
   
   public void setListener(Listener<byte[]> code){
      listener=code;
   }
   
   public void send(byte[] data, int off, int len){
      writeInt(header, 0, len);
      parent.send(header, 0, 4);
      parent.send(data, off, len);
   }
   public void close(){
      parent.close();
   }
   
   public void receive(T data, int off, int len){
      while(len>0){
         if(remaining<0){
            int n=Math.min(len, 4-buffer.size());
            read(data, off, buffer, n);
            if(buffer.size()<4) return;
            remaining=readInt(buffer.data(), 0);
            len-=n;
            off+=n;
         }
         int n=Math.min(len, remaining);
         read(data, off, buffer, n);
         len-=n;
         off+=n;
         remaining-=n;
         if(remaining==0){
            if(listener!=null) listener.receive(buffer.data(), 4, buffer.size()-4);
            remaining=-1;
            buffer.setSize(0);
         }
      }
   }
   //test helper
   void receive(byte[] data){
      receive((T)data, 0, data.length);
   }
   
   public void error(Exception e){
      listener.error(e);
   }
   public void closed(){
      listener.closed();
   }
   
   private static int readInt(byte[] data, int off){
      return 
         ((data[off]&0xff)<<24) | ((data[off+1]&0xff)<<16) | 
         ((data[off+2]&0xff)<<8) | (data[off+3]&0xff)
      ;
   }
   
   private static void writeInt(byte[] buffer, int off, int value){
      buffer[off]=(byte)(value>>>24);
      buffer[off+1]=(byte)(value>>>16);
      buffer[off+2]=(byte)(value>>>8);
      buffer[off+3]=(byte)value;
   }
   
   public static void main(String[] args){
      class MyPacketizer extends StreamPacketizer<byte[]>{
         MyPacketizer(StreamConnection<byte[]> parent){
            super(parent);
         }
         protected byte readByte(byte[] src, int off){
            return src[off];
         }
         protected void read(byte[] src, int srcOff, DataBuffer dst, int len){
            dst.append(src, srcOff, len);
         }
         protected void writeByte(byte[] dst, int off, byte value){
            dst[off]=value;
         }
         protected void pass(Listener<byte[]> dst, byte[] data, int off, int len){
            dst.receive(data,off,len);
         }
      }
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //данные передаются порциями по 5 байтов
      );
      MyPacketizer localEnd=new MyPacketizer(ch.nearEnd);
      MyPacketizer remoteEnd=new MyPacketizer(ch.remoteEnd);

      localEnd.setListener(new StreamConnection.Listener<byte[]>(){
         public void receive(byte[] data, int off, int len){
            System.out.println("local.receive(data, "+off+", "+len+")");
         }
         public void error(Exception e){}
         public void closed(){}
      });
      remoteEnd.setListener(new StreamConnection.Listener<byte[]>(){
         public void receive(byte[] data, int off, int len){
            System.out.println("remote.receive("+new String(data,off,len)+")");
         }
         public void error(Exception e){}
         public void closed(){}
      });
      //must report packet lengths 1,2,3,4,5,6
      localEnd.receive(new byte[]{0,0,0,1, 1, 0,0});
      localEnd.receive(new byte[]{0,2, 1,2, 0,0,0,3, 1,2});
      localEnd.receive(new byte[]{3, 0,0,0,4, 1,2});
      localEnd.receive(new byte[]{3,4, 0,0,0,5, 1,2,3,4,5, 0,0,0,6, 1,2,3,4,5});
      localEnd.receive(new byte[]{6});
      
      //must report correct packets
      localEnd.send("a".getBytes());
      localEnd.send("one".getBytes());
      localEnd.send("two".getBytes());
      localEnd.send("three".getBytes());
      localEnd.send("one two three".getBytes());
   }
}
