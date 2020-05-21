package xserver.api;

import java.util.Map;
import java.util.function.Consumer;
import xserver.util.JsonNode;

/*
 * ќбъект дл€ взаимодействи€ с сервисами на другом конце соединени€.
 * ѕозвол€ет вызвать по имени определенный метод определенного сервиса на удаленном конце. 
 * —оздаетс€ автоматически менеджером сервисов, при прив€зке соединени€.
 */

public interface JsonServiceConnection extends NetConnection<NetConnection.Listener>{
   /*
    * ¬ызвать метод сервиса на другом конце соединени€. 
    * Ќа другом конце вызываетс€ метод call(..) соответствующего сервиса (см. JsonService),
    * ответ получаем через resp.
    * 
    * @param service им€ удаленного сервиса
    * @param method им€ удаленного метода
    * @param arg аргумент
    * @param resp приемник ответа, может быть null если известно, что метод ничего не возвращает
    */
   
   public void call(String service, String method, JsonNode arg, Consumer<JsonNode> resp);
   
   /*
    * “аблица дл€ хранени€ промежуточных данных, относ€щихс€ к соединению.
    * ѕри вызове разных сервисов через одно соединение контекст будет один и тот же,
    * так что можно передавать данные между сервисами.
    */
   public Map<Object,Object> context();
}
