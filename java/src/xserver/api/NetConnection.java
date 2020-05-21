package xserver.api;

/*
 * ������� ���������, ��������� ����������� � ����, ��� �������� ������. 
 */

public interface NetConnection<L extends NetConnection.Listener>{
   //��������� ������� �� ���������� �����������
   public interface Listener{
      public void error(Exception e);
      public void closed();
   }
   
   public void setListener(L code);
   public String remoteAddress();
   public String localAddress();
   public int remotePort();
   public int localPort();
   public boolean isAlive();
   public void close();
}
