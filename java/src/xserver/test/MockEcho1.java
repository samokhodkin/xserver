package xserver.test;

import java.io.*;
import xserver.api.*;
import xserver.io.*;

/*
 * �������� ���-������� � ������� �� ������ ����������
 */

public class MockEcho1{
   public static void main(String[] args) throws Exception{
      MockChannel ch=new MockChannel(
         "local", 1111, "remote", 2222, 5 //������ ���������� �������� �� 5 ������
      );
      ByteArrayStreamPacketizer localEnd=new ByteArrayStreamPacketizer(ch.nearEnd);
      ByteArrayStreamPacketizer remoteEnd=new ByteArrayStreamPacketizer(ch.remoteEnd);
      
      //�� ��������� ����� ���������� ��������� ������ �������
      remoteEnd.setListener(new StreamConnection.Listener<byte[]>(){
         public void receive(byte[] data, int off, int len){
            remoteEnd.send(data, off, len);
         }
         public void error(Exception e){}
         public void closed(){}
      });
      
      //�� ������� ����� ������������� ��������� ������
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
      
      //������ �� ������ �� System.in � ���� ����� ��������� ����� ����������
      BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
      for(String line;;){
         line=in.readLine();
         localEnd.send(line.getBytes("utf8"));
      }
   }
}
