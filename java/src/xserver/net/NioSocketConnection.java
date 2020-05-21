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
 * Реализация StreamConnection на java.nio
 * Использование:
 * 1. создаем Selector, создаем и коннектим SocketChannel
 * 2. SocketChannel ставим в неблокирующий режим
 * 3. регистрируем SocketChannel на селекторе с операцией OP_READ (строго), получаем SelectionKey
 * 4. создаем NioSocketConnection с этими тремя параметрами, аттачим его к ключу. Есть еще параметр 
 *    executor, он объяснен ниже.
 * 5. после каждого селекта делаем это:
 *   if(key.isValid()){
 *      if(key.isReadable()) ((NioSocketConnection)key.attachment()).read()
 *      if(key.isWritable()) ((NioSocketConnection)key.attachment()).write()
 *   }
 * 
 * Алгоритм работы:
 * Посылка данных:
 *  - когда кто-то вызывает send(...), данные добавляются в очередь queue и в ключе включается режим OP_WRITE
 *  - когда сокет готов к отправке, срабатывает key.isWritable() и происходит запись накопленных данных в сокет. 
 *    Если данные отправлены полностью, режим OP_WRITE отключается, до следующего вызова send(...)
 * Прием данных:
 *  - режим OP_READ включен постоянно, когда срабатывает key.isReadable(), данные считываются из сокета и отправляются 
 *    в листенер.
 * Остановка:
 *  - когда вызывается Netconnection.close(), закрывается сокет и ключ
 *    В цикле селектора надо проверять key.isValid(), если false, значит сокет закрыт и просто его игнорируем
 * 
 * Особое внимание обращаем на то, что все операции над ключом должны производиться *строго*
 * в потоке селектора. Для этого передаем в конструктор executor, который обеспечивает 
 * выполнение кода в потоке селектора (реализацию см. в NioSocketClient или NioSocketServer).
 * Здесь имеются 3 такие операции - enableWriteOp, disableWriteOp, close.
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
   

