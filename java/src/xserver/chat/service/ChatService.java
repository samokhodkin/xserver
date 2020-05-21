package xserver.chat.service;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import xserver.api.*;
import xserver.chat.model.*;
import xserver.util.*;
import xserver.io.*;
import xserver.test.*;

public class ChatService extends JsonMethodDispatcher{
   private final static String CONTEXT_KEY_USER="user";
   
   //inject
   public String clientServiceId;
   
   private final String clientEventHandlerId="handleEvent";//see ChatClientService
   private final Chat chat=new Chat();
   private final Map<String, User> users=new ConcurrentHashMap<>();
   
   /* 
    * Регистрация пользователя, только имя, без пароля.
    * аргумент: {
    *   name: ...;
    * }
    * ответ: true или ошибка
    */
   public void login(
      JsonServiceConnection conn, JsonNode args, Consumer<JsonNode> resp
   ) throws Exception{
      String name=args.get("name").string();
      User user=users.get(name);
      if(user!=null) throw new Exception("Such user already exisits: "+name);
      User u=new User(name, event->processUserEvent(event, conn));
      users.put(name, u);
      conn.context().put(CONTEXT_KEY_USER, u);
      conn.setListener(new NetConnection.Listener(){
         public void error(Exception e){
            System.out.println("Error on "+conn);
            e.printStackTrace();
         }
         public void closed(){
            users.remove(name);
            chat.unsubscribeAll(u);
         }
      });
      resp.accept(JsonNode.TRUE);
   }
   
   /*
    * Метод subscribe
    * аргумент - список групп: [group1, ....]
    * ответ: true или ошибка
    */
   public void subscribe(
      JsonServiceConnection conn, JsonNode args, Consumer<JsonNode> resp
   ) throws Exception{
      User user=(User)conn.context().get(CONTEXT_KEY_USER);
      if(user==null) throw new Exception("Not logged in");
      chat.subscribe(user, toStringArray(args));
      resp.accept(JsonNode.TRUE);
   }
   
   /*
    * Метод unsubscribe
    * аргумент - список групп: [group1, ....]
    * ответ: true или ошибка
    */
   public void unsubscribe(
      JsonServiceConnection conn, JsonNode args, Consumer<JsonNode> resp
   ) throws Exception{
      User user=(User)conn.context().get(CONTEXT_KEY_USER);
      if(user==null) throw new Exception("Not logged in");
      chat.unsubscribe(user, toStringArray(args.get("groups")));
      resp.accept(JsonNode.TRUE);
   }
   
   /*
    * Метод send
    * аргумент: {
    *   groups: [group1, ....];
    *   message: ...;
    * }
    * ответ: true или ошибка
    */
   public void send(
      JsonServiceConnection conn, JsonNode args, Consumer<JsonNode> resp
   ) throws Exception{
      User user=(User)conn.context().get(CONTEXT_KEY_USER);
      if(user==null) throw new RuntimeException("Not logged in");
      chat.sendMessage(user, args.get("message").string(), toStringArray(args.get("groups")));
      resp.accept(JsonNode.TRUE);
   }
   
   private void processUserEvent(Event event, JsonServiceConnection conn){
      conn.call(clientServiceId, clientEventHandlerId, JsonNode.valueOf(event), null);
   }
   
   private static String[] toStringArray(JsonNode obj){
      String[] data=new String[obj.size()];
      for(int i=data.length; i-->0;) data[i]=obj.get(i).string();
      return data;
   }
   
   public static void main(String[] args) {
      class Resp implements Consumer<JsonNode>{
         private String msg;
         Resp(String s){
            msg=s;
         }
         public void accept(JsonNode obj){
            System.out.println(msg+": "+obj);
         }
      }
      
      MockChannel ch1=new MockChannel(
         "local", 1111, "remote", 2222, 5 
      );
      MockChannel ch2=new MockChannel(
         "local", 1112, "remote", 2222, 5 
      );
      ByteArrayStreamPacketizer localEnd1=new ByteArrayStreamPacketizer(ch1.nearEnd);
      ByteArrayStreamPacketizer remoteEnd1=new ByteArrayStreamPacketizer(ch1.remoteEnd);
      ByteArrayStreamPacketizer localEnd2=new ByteArrayStreamPacketizer(ch2.nearEnd);
      ByteArrayStreamPacketizer remoteEnd2=new ByteArrayStreamPacketizer(ch2.remoteEnd);
      
      JsonServiceManager localManager1=new JsonServiceManager();
      JsonServiceManager localManager2=new JsonServiceManager();
      JsonServiceManager remoteManager=new JsonServiceManager();
      
      ChatService cs=new ChatService();
      cs.clientServiceId="chatClient";
      remoteManager.services.put("chat", cs);
      remoteManager.attach(remoteEnd1).setListener(new NetConnection.Listener(){
         public void error(Exception e){
            System.out.println("Error at remote end");
            e.printStackTrace();
         }
         public void closed(){
            System.out.println("Remote connection closed");
         }
      });
      remoteManager.attach(remoteEnd2);
      
      localManager1.services.put("chatClient", new JsonService(){
         public void call(
            JsonServiceConnection conn, String method, JsonNode args, 
            Consumer<JsonNode> resp
         ){
            System.out.println("chatClient1."+method+"("+args+")");
         }
      });
      localManager2.services.put("chatClient", new JsonService(){
         public void call(
            JsonServiceConnection conn, String method, JsonNode args, 
            Consumer<JsonNode> resp
         ){
            System.out.println("chatClient2."+method+"("+args+")");
         }
      });
      
      JsonServiceConnection chat1=localManager1.attach(localEnd1);
      JsonServiceConnection chat2=localManager2.attach(localEnd2);
      
      System.out.println("calling chat1.login()");
      chat1.call(
         "chat", "login", JsonNode.object().set("name","client1"),
         new Resp("login response 1")
      );
      System.out.println("calling chat2.login()");
      chat2.call(
         "chat", "login", JsonNode.object().set("name", "client2"),
         new Resp("login response 2")
      );
      
      System.out.println("calling chat1.subscribe()");
      chat1.call(
         "chat", "subscribe", JsonNode.valueOf(new String[]{"main_group"}),
         new Resp("subscribe response 1")
      );
      System.out.println("calling chat2.subscribe()");
      chat2.call(
         "chat", "subscribe", JsonNode.valueOf(new String[]{"main_group"}),
         new Resp("subscribe response 2")
      );
      
      System.out.println("calling chat1.send()");
      chat1.call(
         "chat", "send", JsonNode.object().
            set("groups", new String[]{"main_group"}).
            set("message", "Hi from client1")
         , null
      );
      System.out.println("calling chat2.send()");
      chat2.call(
         "chat", "send", JsonNode.object().
            set("groups", new String[]{"main_group"}).
            set("message", "Hi from client2")
         , null
      );
      System.out.println("closing localEnd1..");
      localEnd1.close();
   }
}
