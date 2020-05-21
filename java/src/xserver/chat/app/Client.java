package xserver.chat.app;

import java.io.*;
import java.nio.*;
import java.util.*;
import xserver.api.*;
import xserver.util.*;
import xserver.io.*;
import xserver.net.*;
import xserver.chat.model.*;
import xserver.chat.service.*;
import static xserver.util.Util.*;

public class Client{
   static int remotePort=5555;
   static String remoteHost="localhost";
   static String remoteServiceId="chat";
   static String clientServiceId="chatClient";
   
   public static void main(String[] args) throws Exception{
      if(args.length>0 && args[0].equals("?")){
         usage();
         return;
      }
      if(args.length>0) remotePort=Integer.parseInt(args[0]);
      if(args.length>1) remoteHost=args[1];
      if(args.length>2) remoteServiceId=args[2];
      if(args.length>3) clientServiceId=args[3];
      
      print("Running with parameters:");
      print("  remotePort="+remotePort);
      print("  remoteHost="+remoteHost);
      print("  remoteServiceId="+remoteServiceId);
      print("  clientServiceId="+clientServiceId);
      print();
      
      print("Connecting...");
      StreamConnection<ByteBuffer> conn=NioSocketClient.connect(remoteHost, remotePort);
      JsonServiceManager sm=new JsonServiceManager();
      ChatClientService service=new ChatClientService();
      sm.services.put(clientServiceId, service);
      service.onUserSubscribed= data->{ //[user, group]
         print("["+data[1]+"] User "+data[0]+" entered group");
      };
      service.onUserUnsubscribed= data->{  //[user, group]
         print("["+data[1]+"] User "+data[0]+" quit");
      };
      service.onMessage= data->{  //[user, group, message]
         print("["+data[1]+"] "+data[0]+": "+data[2]);
      };
      service.onError= err->print(err);
      JsonServiceConnection serviceConnecton=sm.attach(new ByteBufferStreamPacketizer(conn));
      
      print("Connected", "Type command or ? for help");
      
      BufferedReader in=new BufferedReader(new InputStreamReader(System.in, "cp1251"));
      for(;;){
         String[] cmd=in.readLine().split("\\s+");
         switch(cmd[0]){
            case "?":
               printHelp();
               continue;
            case "login":
               call(
                  serviceConnecton, "login", JsonNode.object().set("name", cmd[1]), 
                  "Login ok"
               );
               continue;
            case "subscribe":
               call(
                  serviceConnecton, "subscribe", 
                  JsonNode.valueOf(Arrays.copyOfRange(cmd, 1, cmd.length)),
                  "Subscribe ok"
               );
               continue;
            case "unsubscribe":
               call(
                  serviceConnecton, "unsubscribe", 
                  JsonNode.valueOf(Arrays.copyOfRange(cmd, 1, cmd.length)),
                  "Unsubscribe ok"
               );
               continue;
            case "send":
               System.out.print("Type message: ");
               String msg=in.readLine();
               call(
                  serviceConnecton, "send", JsonNode.object().
                     set("groups", Arrays.copyOfRange(cmd, 1, cmd.length)).
                     set("message", msg)
                  , "Send ok"
               );
               continue;
         }
      }
   }
   
   static void printHelp(){
      print(
         "Available commands:",
         "  login <name>",
         "  subscribe <group1>, <group2>, ...    - subscribe to the listed groups",
         "  unsubscribe <group1>, <group2>, ...    - unsubscribe from the listed groups",
         "  send  <group1>, <group2>, ...   - send a message to the listed groups (you will be prompted to enter the message)"
      );
   }
   
   static void call(
      JsonServiceConnection conn, String method, JsonNode args, String onSuccess
   ){
      conn.call(remoteServiceId, method, args, onSuccess==null? null: resp->{
         if(resp.isPrimitive()) print(method+": "+onSuccess); //{true}
         else{
            try{
               print(errorInstance(resp), method+" failed");
            }
            catch(Exception e){
               print(method+" failed: "+resp.toString());
            }
         }
      });
   }
   
   static void usage(){
      print("Usage:");
      print("java xserver.chat.app.Server [?] [remotePort [remoteHost [remoteServiceId [clientServiceId]]]]");
      print(" ? - print this help and exit");
      print(" remotePort - port of the remote server, default 5555");
      print(" remoteHost - address of the remote server, default 'localhost'");
      print("     may be symbolic or numeric");
      print(" remoteServiceId - name of the remote chat service, default 'chat'");
      print(" clientServiceId - name of the local chat-client service, default 'chatClient'");
      print("     must match the server's clientServiceId parameter");
   }
}
