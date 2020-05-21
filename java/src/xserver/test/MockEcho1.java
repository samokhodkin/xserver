package xserver.test;

import java.io.*;
import xserver.api.*;
import xserver.io.*;

/*
 * Имитация эхо-сервера с логикой на уровне соединения
 */

public class MockEcho1{
   public static void main(String[] args) throws Exception{
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //данные передаются порциями по 5 байтов
      );
      ByteArrayStreamPacketizer localEnd=new ByteArrayStreamPacketizer(ch.nearEnd);
      ByteArrayStreamPacketizer remoteEnd=new ByteArrayStreamPacketizer(ch.remoteEnd);
      
      //на удаленном конце отправляем пришедшие пакеты обратно
      remoteEnd.setListener(new StreamConnection.Listener<byte[]>(){
         public void receive(byte[] data, int off, int len){
            remoteEnd.send(data, off, len);
         }
         public void error(Exception e){}
         public void closed(){}
      });
      
      //На ближнем конце распечатываем пришедшие пакеты
      localEnd.setListener(new StreamConnection.Listener<byte[]>(){
         public void receive(byte[] data, int off, int len){
            System.out.print("echo: ");
            System.out.write(data, off, len);
            System.out.println();
         }
         public void error(Exception e){}
         public void closed(){}
      });
      
      System.out.println("Hi! Type something");
      
      //читаем по строке из System.in и шлем через локальный конец соединения
      BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
      for(String line;;){
         line=in.readLine();
         localEnd.send(line.getBytes("utf8"));
      }
   }
}
