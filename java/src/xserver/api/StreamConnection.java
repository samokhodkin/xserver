package xserver.api;

import java.nio.ByteBuffer;

/*
 * ���������, ����������� ����������� � ���� � ��������� ��������� ������.
 * ��� �������, ����������� �������� ������ �������������� (DataType). ��������������, ��� 
 * DataType ����� ���� ���� byte[] (���� ������������ java.io) , ���� java.nio.ByteBuffer
 * (���� ������������ java.nio).
 * ������ ������ - ������� send()
 * ����� ������ - ����� Listener.receive()
 *
 * ����������� ��������������� � ������� ������� � ������ xserver.net
 * ��� ������ ����� ������������ xserver.test.MockConnection - �������� ������.
 */

public interface StreamConnection<DataType> extends NetConnection<StreamConnection.Listener<DataType>>{
   public interface Listener<DT> extends NetConnection.Listener{
      public void receive(DT data, int off, int len);
   }
   
   public void send(byte[] data, int off, int len);
   
   default public void send(byte[] data){
      send(data, 0, data.length);
   }
}
