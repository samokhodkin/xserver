package xserver.chat.app;

import xserver.api.*;
import xserver.io.*;
import xserver.net.*;
import xserver.chat.model.*;
import xserver.chat.service.*;
import static xserver.util.Util.*;

public class Server{
   static final int BACKLOG=40;
   
   //args: host, port, service id (you choose it), clientServiceId (you choose it)
   //where host is some local address
   public static void main(String[] args) throws Exception{
      if(args.length>0 && args[0].equals("?")){
         usage();
         return;
      }
      int port=5555;
      String host="localhost";
      String serviceId="chat";
      String clientServiceId="chatClient";
      
      if(args.length>0) port=Integer.parseInt(args[0]);
      if(args.length>1) host=args[1];
      if(args.length>2) serviceId=args[2];
      if(args.length>3) clientServiceId=args[3];
      
      print("Running with parameters:");
      print("  port="+port);
      print("  host="+host);
      print("  serviceId="+serviceId);
      print("  clientServiceId="+clientServiceId);
      print();
      
      ChatService service=new ChatService();
      service.clientServiceId=clientServiceId;
      JsonServiceManager sm=new JsonServiceManager();
      sm.services.put(serviceId, service);
      
      NioSocketServer server=new NioSocketServer(host, port, BACKLOG, conn->{
         print("Accepted connection from "+conn.remoteAddress()+":"+conn.remotePort());
         JsonServiceConnection sc=sm.attach(new ByteBufferStreamPacketizer(conn));
         sc.setListener(new NetConnection.Listener(){
            public void error(Exception e){
               synchronized(System.out){
                  print("Connection error at "+sc);
                  e.printStackTrace();
               }
            }
            public void closed(){
               print("Connection "+sc+" closed");
            }
         });
      });
      
      print("Server is ready, waiting for client connections");
      
      server.run();
   }
   
   static void usage(){
      print("Usage:");
      print("java xserver.chat.app.Server [?] [port [host [serviceId [clientServiceId]]]]");
      print(" ? - print this help and exit");
      print(" port - port for accepting connections, default 5555");
      print(" host - name of some local network interface, default 'localhost'");
      print("     may be symbolic (localhost) ot numeric (0.0.0.0)");
      print(" serviceId - name of chat service, default 'chat'");
      print(" clientServiceId - name of client chat service, default 'chatClient'");
      print("     clients should use the same name for their service ids");
   }
}
