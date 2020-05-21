package xserver.net;

import xserver.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/*
 * Соединяется с сервером на указанном адресе и порту и предоставляет соединение в 
 * виде StreamConnection<ByteBuffer>. 
 * Использование:
 * - блокирующий метод:
 *   connection=NioSocketClient.connect("host", port);
 * - неблокирующий метод:
 *   NioSocketClient.connect("host", port, conn->{
 *      //делаем что-то с соединением
 *   });
 * 
 * 
 * Алгоритм
 * Здесь обеспечивается селекторный цикл
 * Операции чтения-записи вынесены в класс NioSocketConnection (см. подробности там).
 */

public class NioSocketClient{
   private static final List<Runnable> selectorTasks=new ArrayList<>();
   private static final Selector selector;
   static{
      try{
         selector=Selector.open();
         new Thread(){
            public void run(){
               NioSocketClient.run();
            }
         }.start();
      }
      catch(Exception e){
         throw new Error(e);
      }
   }
   //устройство для выполнения кода в потоке селектора, см. NioSocketConnection
   private static final Consumer<Runnable> executeSelectorTask=r->{
      synchronized(selectorTasks){
         selectorTasks.add(r);
         selector.wakeup();
      }
   };
   
   //connect in blocking way
   public static StreamConnection<ByteBuffer> connect(String host, int port) throws Exception{
      LinkedBlockingQueue<StreamConnection<ByteBuffer>> queue=new LinkedBlockingQueue<>();
      connect(host, port, conn->{
         queue.add(conn);
      });
      return queue.take();
   }
   
   //connect in asychronous way
   public static void connect(
      String host, int port, Consumer<StreamConnection<ByteBuffer>> consumer
   ) throws Exception{
      SocketChannel socket=SocketChannel.open();
      socket.connect(new InetSocketAddress(host, port));
      executeSelectorTask.accept(()->{
         try{
            socket.configureBlocking(false);
            SelectionKey k=socket.register(selector, SelectionKey.OP_READ);
            NioSocketConnection conn=new NioSocketConnection(socket, selector, k, executeSelectorTask);
            consumer.accept(conn);
            k.attach(conn);
         }
         catch(Exception e){
            e.printStackTrace();
         }
      });
   }
   
   private static void run(){
      try{
         for(;;){
            selector.select();
            synchronized(selectorTasks){
               int i=selectorTasks.size();
               if(i>0){
                  while(i-->0) selectorTasks.remove(i).run();
                  continue;
               }
            }
            Iterator<SelectionKey> keys=selector.selectedKeys().iterator();
            for(;keys.hasNext();){
               SelectionKey k=keys.next();
               if(k.isReadable()) ((NioSocketConnection)k.attachment()).read();
               if(k.isWritable()) ((NioSocketConnection)k.attachment()).write();
               keys.remove();
            }
         }
      }
      catch(Exception e){
         e.printStackTrace();
      }
   }
   
   public static void main(String[] args) throws Exception{
      ServerSocket server=new ServerSocket(1000, 10, InetAddress.getByName("localhost"));
      new Thread(){
         public void run(){
            try{
               for(;;){
                  Socket s=server.accept();
                  System.out.println("server received connection: "+s);
                  InputStream in=s.getInputStream();
                  OutputStream out=s.getOutputStream();
                  new Thread(){
                     public void run(){
                        System.out.println("server: reading from "+s);
                        try{
                           byte[] buf=new byte[1000];
                           for(;;){
                              int n=in.read(buf);
                              System.out.println("server received "+n+" bytes from "+s);
                              if(n<0){
                                 System.out.println("server received eos from "+s);
                                 s.close();
                                 return;
                              }
                              out.write(buf,0,n);
                           }
                        }
                        catch(Exception e){
                           e.printStackTrace();
                        }
                     }
                  }.start();
               }
            }
            catch(Exception e){
               e.printStackTrace();
            }
         }
      }.start();
      
      System.out.println("client connecting");
      LinkedBlockingQueue<StreamConnection<ByteBuffer>> queue=new LinkedBlockingQueue<>();
      connect("localhost", 1000, conn->{
         queue.add(conn);
      });
      StreamConnection<ByteBuffer> c=queue.take();
      System.out.println("client connection ok");
      c.setListener(new StreamConnection.Listener<ByteBuffer>(){
         public void receive(ByteBuffer data, int off, int len){
            System.out.println(" client received "+len+" bytes");
         }
         public void error(Exception e){
            System.out.println(" client connection error "+e);
            e.printStackTrace();
         }
         public void closed(){
            System.out.println(" client connection closed");
         }
      });
      
      for(int n: new int[]{11,22,33,44,55}){
         System.out.println("sending "+n+" bytes");
         c.send(new byte[n]);
         Thread.sleep(100);
      }
   }
}
