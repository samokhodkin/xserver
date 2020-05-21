package xserver.api;

import java.nio.ByteBuffer;

/*
 * Интерфейс, описывающий подключение к сети с потоковой передачей данных.
 * Тип буффера, содержащего входящие данные параметризован (DataType). Предполагается, что 
 * DataType может быть либо byte[] (если используется java.io) , либо java.nio.ByteBuffer
 * (если используется java.nio).
 * Посыка данных - функция send()
 * Прием данных - через Listener.receive()
 *
 * Подключение устанавливается с помощью классов в пакете xserver.net
 * Для тестов можно использовать xserver.test.MockConnection - имитация канала.
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
