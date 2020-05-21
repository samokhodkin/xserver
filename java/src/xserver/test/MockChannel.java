package xserver.test;

import xserver.api.*;

/*
 * Имитация канала передачи. 
 * В полях nearEnd и remoteEnd автоматически создаются соответствующие соединения.
 */

public class MockChannel implements StreamConnection<byte[]>{
   public final StreamConnection<byte[]> nearEnd;
   public final StreamConnection<byte[]> remoteEnd;
   
   private final String localAddress, remoteAddress; 
   private final int localPort, remotePort;
   private final int maxChunk;
   private final MockChannel remote;
   private StreamConnection.Listener<byte[]> listener;
   
   private boolean alive=true;
   
   /**
    * Создается имитация канала с двумя соединениями на концах, см. поля nearEnd и remoteEnd.
    * Пакеты, посланные на одном конце будут получены на другом.
    * При этом симулируется разбитие сообщений на пакеты сетевого уровня.
    * Различие ближний-дальний условное, концы симметричны.
    *  
    * @param localAddress произвольная строка
    * @param localPort произвольное число
    * @param remoteAddress произвольная строка
    * @param remotePort произвольное число
    * @param maxChunk максимальная длина пакетов, на которые разбивается сообщение
    * @param remote
    */
   public MockChannel(
         String localAddress, int localPort,
         String remoteAddress, int remotePort,
         int maxChunk
   ){
      this.localAddress=localAddress;
      this.localPort=localPort;
      this.remoteAddress=remoteAddress;
      this.remotePort=remotePort;
      this.maxChunk=maxChunk;
      this.nearEnd=this;
      this.remoteEnd=this.remote= new MockChannel(
         remoteAddress, remotePort, localAddress, localPort, maxChunk, this
      );
   }
   
   private MockChannel(
      String localAddress, int localPort,
      String remoteAddress, int remotePort,
      int maxChunk, MockChannel remote
   ){
      this.localAddress=localAddress;
      this.localPort=localPort;
      this.remoteAddress=remoteAddress;
      this.remotePort=remotePort;
      this.maxChunk=maxChunk;
      this.nearEnd=this;
      this.remoteEnd=this.remote= remote;
   }
   
   public void setListener(StreamConnection.Listener<byte[]> code){
      listener=code;
   }
   
   public void send(byte[] data, int off, int len){
      if(remote!=null && remote.listener!=null) while(len>0){
         int chunk= maxChunk<0? len: Math.min(len, maxChunk);
         remote.listener.receive(data, off, chunk);
         len-=chunk;
         off+=chunk;
      }
   }
   
   public void close(){
      alive=false;
      if(listener!=null) listener.closed();
      if(remote!=null){
         remote.alive=false;
         if(remote.listener!=null) remote.listener.closed();
      }
   }
   
   public String remoteAddress(){
      return remoteAddress;
   }
   public String localAddress(){
      return localAddress;
   }
   public int remotePort(){
      return remotePort;
   }
   public int localPort(){
      return localPort;
   }
   public boolean isAlive(){
      return alive;
   }
}
