package xserver.test;

import xserver.api.*;

/*
 * »митаци€ канала передачи. 
 * ¬ пол€х nearEnd и remoteEnd автоматически создаютс€ соответствующие соединени€.
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
   
   public MockChannel(
      String localAddress, int localPort,
      String remoteAddress, int remotePort,
      int maxChunk, MockChannel... remote
   ){
      this.localAddress=localAddress;
      this.localPort=localPort;
      this.remoteAddress=remoteAddress;
      this.remotePort=remotePort;
      this.maxChunk=maxChunk;
      this.nearEnd=this;
      this.remoteEnd=this.remote= remote.length>0? remote[0]: new MockChannel(
         remoteAddress, remotePort, localAddress, localPort, maxChunk, this
      );
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
