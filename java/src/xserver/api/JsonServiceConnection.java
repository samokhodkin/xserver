package xserver.api;

import java.util.Map;
import java.util.function.Consumer;
import xserver.util.JsonNode;

/*
 * ќбъект дл€ взаимодействи€ с сервисами на другом конце соединени€.
 * —оздаетс€ автоматически менеджером сервисов, при прив€зке соединени€.
 */

public interface JsonServiceConnection extends NetConnection<NetConnection.Listener>{
   /*
    * вызвать метод сервиса на другом конце соединени€;
    * если метод возвращает значение, resp об€зателен
    */
   public void call(String service, String method, JsonNode arg, Consumer<JsonNode> resp);
   
   /*
    * “аблица дл€ хранени€ промежуточных данных, относ€щихс€ к соединению.
    * ѕри вызове разных сервисов через одно соединение контекст будет один и тот же,
    * так что можно передавать данные между сервисами.
    */
   public Map<Object,Object> context();
}
