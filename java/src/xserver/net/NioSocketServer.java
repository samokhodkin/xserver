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
 * Запускает сервер на указанном порту и предоставляет входящие соединения в 
 * виде StreamConnection<ByteBuffer>. 
 * Использование:
 * server=NioSocketServer("host", port, backlog, conn->{
 *   //делаем что-то с входящим соединением
 * });
 * server.run();
 * 
 * Метод run() - точка входа для рабочего потока.
 * Метод close() - программная остановка сервера
 * 
 * Алгоритм
 * Здесь обеспечивается селекторный цикл и обработка входящих соединений.
 * Операции чтения-записи вынесены в класс NioSocketConnection (см. подробности там).
 */

public class NioSocketServer{
   private final Selector selector;
   private final ServerSocketChannel serverSocket;
   private final List<NetConnection> connections=new ArrayList<>();
   private final Consumer<StreamConnection<ByteBuffer>> connectionHandler;
   
   //устройство для выполнения кода в потоке селектора, см. NioSocketConnection
   private final List<Runnable> selectorTasks=new ArrayList<>();
   private final Consumer<Runnable> executeSelectorTask;
   
   private volatile boolean stopped;
   
   public NioSocketServer(
      String host, int port, int backlog, 
      Consumer<StreamConnection<ByteBuffer>> connectionHandler
   ) throws IOException{
      this.connectionHandler=connectionHandler;
      selector=Selector.open();
      serverSocket=ServerSocketChannel.open();
      serverSocket.configureBlocking(false);
      serverSocket.bind(new InetSocketAddress(host, port), backlog);
      serverSocket.register(selector, SelectionKey.OP_ACCEPT);
      executeSelectorTask=r->{
         synchronized(selectorTasks){
            selectorTasks.add(r);
            selector.wakeup();
         }
      };
   }
   
   public void close(){
      stopped=true;
      try{
         selector.close();
         serverSocket.close();
         for(NetConnection nc: connections) nc.close();
      }
      catch(Exception e){
         print(e);
      }
   }
   
   public void run() throws Exception{
      while(!stopped){
         selector.select();
         if(stopped) break;
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
            //эта проверка обязательна, см. NioSocketConnection, иначе будут ошибки
            if(k.isValid()){
               //обрабатываем входящее соединение, см. NioSocketConnection
               if(k.isAcceptable()){ //new connection
                  SocketChannel s=serverSocket.accept();
                  s.configureBlocking(false);
                  SelectionKey k1=s.register(selector, SelectionKey.OP_READ);
                  NioSocketConnection c=new NioSocketConnection(s,selector,k1,executeSelectorTask);
                  c.onClose= ()->connections.remove(c);
                  connections.add(c);
                  connectionHandler.accept(c);
                  k1.attach(c);
               }
               else{
                  //обрабатываем чтение-запись, см. NioSocketConnection
                  NioSocketConnection c=(NioSocketConnection)k.attachment();
                  if(k.isReadable()) c.read();
                  if(k.isWritable()) c.write();
               }
            }
            keys.remove();
         }
      }
   }
   
   public static void main(String[] args) throws Exception{
      NioSocketServer server=new NioSocketServer("localhost", 1000, 10, c->{
         System.out.println("connection from "+c.remoteAddress());
         c.setListener(new StreamConnection.Listener<ByteBuffer>(){
            public void receive(ByteBuffer data, int off, int len){
               print("Server received "+len+" bytes");
               c.send(new byte[len],off,len);
            }
            public void error(Exception e){
               print(e, "Server connection error");
            }
            public void closed(){
               print("Server connection closed");
            }
         });
      });
      new Thread(){
         public void run(){
            try{
               server.run();
            }
            catch(Exception e){
               print(e);
               return;
            }
         }
      }.start();
      
      Socket s=new Socket("localhost", 1000);
      System.out.println("out socket ok");
      InputStream in=s.getInputStream();
      System.out.println("in ok");
      new Thread(){
         public void run(){
            try{
               byte[] buf=new byte[1000];
               for(;;){
                  int n=in.read(buf);
                  System.out.println(" client received "+n+" bytes");
               }
            }
            catch(Exception e){
               print(e);
               System.out.println(" client error: "+e);
               return;
            }
            
         }
      }.start();
      System.out.println("in thread ok");
      
      OutputStream out=s.getOutputStream();
      System.out.println("out ok");
      for(int n: new int[]{1,3,5,7,11,21,31}){
         System.out.println("sending 2 packets * ["+n+"] bytes");
         out.write(new byte[n]);
         out.write(new byte[n]);
         System.out.println("2 packets sent");
         Thread.sleep(100);
      }
      
      System.out.println("closing the socket");
      s.close();
   }
}
