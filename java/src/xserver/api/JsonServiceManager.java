package xserver.api;

import java.util.*;
import java.util.function.*;
import java.util.concurrent.ConcurrentHashMap;
import xserver.io.*;
import xserver.util.*;
import xserver.test.*;

/* 
 * Обеспечивает вызов сервисов через пакетное соединение.
 * Использование:
 * 1. создаем new JsonServiceManager()
 * 2. регистрируем нужные сервисы в поле services
 * 3. подключаем входящие соединения (PacketConnection<byte[]>) в методе attach().
 * Дальше все работает само.
 * Работает только с пакетным соединением типа byte[] !!!
 * 
 * Протокол обмена (основан на json):
 * Вызов метода:
 * {
 *    call: <service name>;
 *    method: <method name>;
 *    args: <list of args>;
 *    from: <return address, optional>;
 * }
 * Результат вызова, опционально:
 * {
 *    return: <return address>;
 *    value: <object>;
 * }
 * Результат посылается если в вызове было поле from
 */

public class JsonServiceManager{
   private static final int RESP_TTL=20_000; //20 sec
   
   /*
    * Таблица зарегистрированных сервисов.
    * Поле публичное, можно использовать напрямую:
    * myServiceManager.services.put("name", myService);
    */
   public final Map<String, JsonService> services=new ConcurrentHashMap<>();
   
   public JsonServiceConnection attach(PacketConnection<byte[]> conn){
      return new ConnectionAdapter(conn);
   }
   
   /*
    * Вспом. объект, обеспечивает связ между соединением и сервисами.
    * Для этого, с одной стороны имеет интерфейс PacketConnection.Listener, позволяющий
    * получать данные из соединения, с другой стороны интерфейс JsonServiceConnection,
    * через который получает команды на вызов удаленного сервися.
    * Алгоритм:
    * При поступлении пакета из соединения (метод receive(...)), пакет конвертируется в Json, 
    * если имеется поле "call", то находится соотв. сервис в таблице 
    * и вызывается его метод call(). Если имеется в пакете есть поле "return",
    * значит пришел резултат удаленного вызова. В таблице ищется соответствующий
    * обработчик и вызывается.
    * 
    * При получении команды на вызов удаленного сервиса (метод call(...)) 
    * создается пакет с соотв. полями и регистриется обработчик ответа, 
    * пакет посылается в соединение.
    */
   class ConnectionAdapter implements PacketConnection.Listener<byte[]>, JsonServiceConnection{
      private final PacketConnection connection;
      private final Map<Object,Object> context=new HashMap<>();
      
      //таблица обработчиков результатов удаленных вызовов
      private final TempCache<Consumer<JsonNode>> responseHandlers=new TempCache<>();
      
      private Listener listener;
      
      ConnectionAdapter(PacketConnection connection){
         this.connection=connection;
         connection.setListener(this);
      }
      
      public void receive(byte[] data, int off, int len){
         try{
            JsonNode obj=new JsonNode(new String(data, off, len, "utf8"));
            JsonNode serviceName=obj.get("call");
            if(serviceName!=null){
               JsonService service=services.get(serviceName.string());
               if(service==null){
                  error("Service not found: "+serviceName);
                  return;
               }
               JsonNode from=obj.get("from");
               Consumer<JsonNode> resp= from==null? null: node->{
                  JsonNode msg=JsonNode.object();
                  msg.set("return", from);
                  msg.set("value", node);
                  connection.send(utf8(msg.toString()));
               };
               service.call(this, obj.get("method").string(), obj.get("args"), resp);
               return;
            }
            JsonNode returnAddr=obj.get("return");
            if(returnAddr!=null){
               Consumer<JsonNode> handler=responseHandlers.get(returnAddr.string());
               if(handler==null){
                  error("Return handler not found: "+returnAddr);
                  return;
               }
               handler.accept(obj.get("value"));
               return;
            }
            error("Malformed inbound message: "+obj);
         }
         catch(Exception e){
            error(e);
         }
      }
      
      public void error(Exception e){
         if(listener!=null) listener.error(e);
      }
      
      public void closed(){
         if(listener!=null) listener.closed();
      }
      
      public void setListener(Listener l){
         listener=l;
      }
      
      public void call(String service, String method, JsonNode args, Consumer<JsonNode> resp){
         JsonNode msg=JsonNode.object();
         msg.set("call", JsonNode.valueOf(service));
         msg.set("method", JsonNode.valueOf(method));
         msg.set("args", args);
         if(resp!=null){
            String id=responseHandlers.put(obj->resp.accept(obj), RESP_TTL, ()->{
               error("Response handler expired for "+service+"."+method+"() -> "+msg.get("from"));
            });
            msg.set("from", JsonNode.valueOf(id));
         }
         connection.send(utf8(msg.toString()));
      }
      
      public Map<Object,Object> context(){
         return context;
      }
      
      public boolean isAlive(){
         return connection.isAlive();
      }
      
      public String localAddress(){
         return connection.localAddress();
      }
      
      public String remoteAddress(){
         return connection.remoteAddress();
      }
      
      public int localPort(){
         return connection.localPort();
      }
      
      public int remotePort(){
         return connection.remotePort();
      }
      
      public void close(){
         connection.close();
      }
      
      private void error(String s){
         error(new RuntimeException(s));
      }
   
      public String toString(){
         return getClass().getSimpleName()+"{"+remoteAddress()+":"+remotePort()+"->"+localAddress()+":"+localPort()+"}";
      }
   }
   
   private static byte[] utf8(String s){
      try{
         return s.getBytes("utf8");
      }
      catch(Exception e){
         throw new Error(e);
      }
   }
   
   public static void main(String[] args){
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //данные передаются порциями по 5 байтов
      );
      ByteArrayStreamPacketizer localEnd=new ByteArrayStreamPacketizer(ch.nearEnd);
      ByteArrayStreamPacketizer remoteEnd=new ByteArrayStreamPacketizer(ch.remoteEnd);
      
      JsonServiceManager localManager=new JsonServiceManager();
      JsonServiceManager remoteManager=new JsonServiceManager();
      
      remoteManager.services.put("test", new JsonService(){
         //название метода игнорируем
         public void call(
            JsonServiceConnection conn, String method, JsonNode arg, Consumer<JsonNode> resp
         ){
            System.out.println("test service: called "+method+", arg="+arg);
            resp.accept(JsonNode.valueOf("test service ok, arg="+arg));
         }
      });
      remoteManager.attach(remoteEnd);
      JsonServiceConnection sc=localManager.attach(localEnd);
      
      sc.call("test", "main", JsonNode.valueOf("hi"), obj->System.out.println(obj.string()));
   }
}
