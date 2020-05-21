package xserver.api;

import java.nio.ByteBuffer;

/*
 * Интерфейс, описывающий подключение к сети с пакетной передачей данных.
 * Функции те же что и в StreamConnection, но подразумевается что пакеты 
 * приходят в неизменном виде (не режутся и не сливаются).
 * 
 * Потоковое соединение можно превратить в пакетное с помощью конвертора xserver.io.Packetizer
 */

public interface PacketConnection<DataType> extends StreamConnection<DataType>{}
