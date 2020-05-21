package xserver.test;

import java.io.*;
import java.util.function.Consumer;
import xserver.api.*;
import xserver.io.*;
import xserver.util.JsonNode;

/*
 * Имитация эхо-сервера с логикой на уровне сервисов
 */
   
public class MockEcho2 {
   public static void main(String[] args) throws Exception{
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //данные передаются порциями по 5 байтов
      );
      ByteArrayStreamPacketizer localEnd=new ByteArrayStreamPacketizer(ch.nearEnd);
      ByteArrayStreamPacketizer remoteEnd=new ByteArrayStreamPacketizer(ch.remoteEnd);
      
      JsonServiceManager localManager=new JsonServiceManager();
      JsonServiceManager remoteManager=new JsonServiceManager();
      
      //установим эхо-сервис на удаленном конце
      remoteManager.services.put("echo", new JsonService(){
         //название метода игнорируем
         public void call(
            JsonServiceConnection conn, String method, JsonNode arg, Consumer<JsonNode> resp
         ){
            //отсылаем данные обратно
            resp.accept(arg);
         }
      });
      //подключаем удаленный конец соединения к сервисам,
      //обязательно следим за ошибками
      remoteManager.attach(remoteEnd).setListener(new NetConnection.Listener(){
         public void error(Exception e){
            System.out.println("Error at remote end");
            e.printStackTrace();
         }
         public void closed(){
            System.out.println("Remote connection closed");
         }
      });
      
      //подключаемся к сервисам со своей стороны
      JsonServiceConnection localServiceConnection=localManager.attach(localEnd);
      
      System.out.println("Hi! Type something");
      
      //читаем по строке из System.in и шлем через локальный конец соединения,
      //ответы получаем через колбэк
      BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
      for(String line;;){
         line=in.readLine();
         localServiceConnection.call("echo", null, JsonNode.valueOf(line), obj->{
            System.out.println("echo: "+obj);
         });
      }
   }
}
