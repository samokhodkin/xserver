package xserver.test;

import java.io.*;
import java.util.function.Consumer;
import xserver.api.*;
import xserver.io.*;
import xserver.util.JsonNode;

/*
 * �������� ���-������� � ������� �� ������ ��������
 */
   
public class MockEcho2 {
   public static void main(String[] args) throws Exception{
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //������ ���������� �������� �� 5 ������
      );
      ByteArrayStreamPacketizer localEnd=new ByteArrayStreamPacketizer(ch.nearEnd);
      ByteArrayStreamPacketizer remoteEnd=new ByteArrayStreamPacketizer(ch.remoteEnd);
      
      JsonServiceManager localManager=new JsonServiceManager();
      JsonServiceManager remoteManager=new JsonServiceManager();
      
      //��������� ���-������ �� ��������� �����
      remoteManager.services.put("echo", new JsonService(){
         //�������� ������ ����������
         public void call(
            JsonServiceConnection conn, String method, JsonNode arg, Consumer<JsonNode> resp
         ){
            //�������� ������ �������
            resp.accept(arg);
         }
      });
      //���������� ��������� ����� ���������� � ��������,
      //����������� ������ �� ��������
      remoteManager.attach(remoteEnd).setListener(new NetConnection.Listener(){
         public void error(Exception e){
            System.out.println("Error at remote end");
            e.printStackTrace();
         }
         public void closed(){
            System.out.println("Remote connection closed");
         }
      });
      
      //������������ � �������� �� ����� �������
      JsonServiceConnection localServiceConnection=localManager.attach(localEnd);
      
      System.out.println("Hi! Type something");
      
      //������ �� ������ �� System.in � ���� ����� ��������� ����� ����������,
      //������ �������� ����� ������
      BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
      for(String line;;){
         line=in.readLine();
         localServiceConnection.call("echo", null, JsonNode.valueOf(line), obj->{
            System.out.println("echo: "+obj);
         });
      }
   }
}
