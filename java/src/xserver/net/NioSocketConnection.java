package xserver.net;

import java.util.*;
import java.util.function.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import xserver.api.*;
import static xserver.util.Util.*;

/*
 * ���������� StreamConnection �� java.nio
 * �������������:
 * 1. ������� Selector, ������� � ��������� SocketChannel
 * 2. SocketChannel ������ � ������������� �����
 * 3. ������������ SocketChannel �� ��������� � ��������� OP_READ (������), �������� SelectionKey
 * 4. ������� NioSocketConnection � ����� ����� �����������, ������� ��� � �����. ���� ��� �������� 
 *    executor, �� �������� ����.
 * 5. ����� ������� ������� ������ ���:
 *   if(key.isValid()){
 *      if(key.isReadable()) ((NioSocketConnection)key.attachment()).read()
 *      if(key.isWritable()) ((NioSocketConnection)key.attachment()).write()
 *   }
 * 
 * �������� ������:
 * ������� ������:
 *  - ����� ���-�� �������� send(...), ������ ����������� � ������� queue � � ����� ���������� ����� OP_WRITE
 *  - ����� ����� ����� � ��������, ����������� key.isWritable() � ���������� ������ ����������� ������ � �����. 
 *    ���� ������ ���������� ���������, ����� OP_WRITE �����������, �� ���������� ������ send(...)
 * ����� ������:
 *  - ����� OP_READ ������� ���������, ����� ����������� key.isReadable(), ������ ����������� �� ������ � ������������ 
 *    � ��������.
 * ���������:
 *  - ����� ���������� Netconnection.close(), ����������� ����� � ����
 *    � ����� ��������� ���� ��������� key.isValid(), ���� false, ������ ����� ������ � ������ ��� ����������
 * 
 * ������ �������� �������� �� ��, ��� ��� �������� ��� ������ ������ ������������� *������*
 * � ������ ���������. ��� ����� �������� � ����������� executor, ������� ������������ 
 * ���������� ���� � ������ ��������� (���������� ��. � NioSocketClient ��� NioSocketServer).
 * ����� ������� 3 ����� �������� - enableWriteOp, disableWriteOp, close.
 */


class NioSocketConnection implements StreamConnection<ByteBuffer>{
   private final SocketChannel socket;
   private final Selector selector;
   final SelectionKey key;
   private final Consumer<Runnable> executor;
   
   private Listener<ByteBuffer> listener;
   private final String localAddr, remoteAddr;
   private final int localPort, remotePort;
   private boolean alive=true;
   
   final Queue<ByteBuffer> queue=new LinkedList<>();
   private final ByteBuffer input=ByteBuffer.allocate(1024);
   
   private final Runnable enableWriteOp, disableWriteOp, close;
   
   Runnable onClose;
   
   NioSocketConnection(
      SocketChannel s, Selector selector, SelectionKey key,
      Consumer<Runnable> executor
   ){
      this.socket=s;
      this.selector=selector;
      this.key=key;
      this.executor=executor;
      enableWriteOp=()->{
         key.interestOps(key.interestOps()|SelectionKey.OP_WRITE);
      };
      disableWriteOp=()->{
         key.interestOps(key.interestOps()&~SelectionKey.OP_WRITE);
      };
      close=()->{
         try{
            key.cancel();
            socket.close();
         }
         catch(Exception e){
            if(listener!=null) listener.error(e);
            else print(e);
            return;
         }
         if(listener!=null) listener.closed();
         if(onClose!=null) onClose.run();
      };
      Socket plainSocket=socket.socket();
      localAddr=plainSocket.getLocalAddress().toString();
      remoteAddr=plainSocket.getInetAddress().toString();
      localPort=plainSocket.getLocalPort();
      remotePort=plainSocket.getPort();
   }
   
   public void setListener(Listener<ByteBuffer> code){
      listener=code;
   }
   public String remoteAddress(){
      return remoteAddr;
   }
   public String localAddress(){
      return localAddr;
   }
   public int remotePort(){
      return remotePort;
   }
   public int localPort(){
      return localPort;
   }
   public boolean isAlive(){
      return socket.isConnected();
   }
   public void close(){
      executor.accept(close);
   }
   
   public synchronized void send(byte[] data, int off, int len){
      ByteBuffer buf=ByteBuffer.allocate(len);
      buf.put(data,off,len);
      buf.flip();
      queue.add(buf);
      executor.accept(enableWriteOp);
   }
   
   void read(){
      try{
         for(;;){
            input.clear();
            int len=socket.read(input);
            if(len==0) return;
            if(len<0){
               close();
               return;
            }
            input.flip();
            if(listener!=null) listener.receive(input, 0, input.limit());
         }
      }
      catch(IOException e){
         if(listener!=null) listener.error(e);
         close();
      }
      catch(Exception e){
         if(listener!=null) listener.error(e);
         else print(e);
      }
   }
   
   synchronized void write(){
      try{
         ByteBuffer buf;
         while((buf=queue.peek())!=null){
            int len=buf.remaining();
            if(len>0){
               int written=socket.write(buf);
               if(written<len) return;
            }
            queue.remove();
         }
         executor.accept(disableWriteOp);
      }
      catch(Exception e){
         if(listener!=null) listener.error(e);
         else print(e);
      }
   }
}
   

