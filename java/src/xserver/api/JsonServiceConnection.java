package xserver.api;

import java.util.Map;
import java.util.function.Consumer;
import xserver.util.JsonNode;

/*
 * ������ ��� �������������� � ��������� �� ������ ����� ����������.
 * ��������� ������������� ���������� ��������, ��� �������� ����������.
 */

public interface JsonServiceConnection extends NetConnection<NetConnection.Listener>{
   /*
    * ������� ����� ������� �� ������ ����� ����������;
    * ���� ����� ���������� ��������, resp ����������
    */
   public void call(String service, String method, JsonNode arg, Consumer<JsonNode> resp);
   
   /*
    * ������� ��� �������� ������������� ������, ����������� � ����������.
    * ��� ������ ������ �������� ����� ���� ���������� �������� ����� ���� � ��� ��,
    * ��� ��� ����� ���������� ������ ����� ���������.
    */
   public Map<Object,Object> context();
}
