package xserver.chat.service;

import java.util.function.*;
import xserver.api.*;
import xserver.util.*;
import xserver.chat.model.*;
import static xserver.util.Util.*;

public class ChatClientService extends JsonMethodDispatcher{
   public Consumer<String[]> onUserSubscribed; //[user, group]
   public Consumer<String[]> onUserUnsubscribed; //[user, group]
   public Consumer<String[]> onMessage; //[user, group, message]
   public Consumer<Exception> onError;
   
   /*
    * Принимает сообщения от сервера
    * Аргумент: model.Event
    * Ответ: отсутствует
    */
   public void handleEvent(
      JsonServiceConnection conn, JsonNode args, Consumer<JsonNode> resp
   ) throws Exception{
      try{
         Event e=args.instance(Event.class);
         switch(e.type){
            case USER_SUBSCRIBED:
               if(onUserSubscribed!=null) onUserSubscribed.accept(new String[]{
                  e.user.name, e.group
               });
               return;
            case USER_UNSUBSCRIBED:
               if(onUserUnsubscribed!=null) onUserUnsubscribed.accept(new String[]{
                  e.user.name, e.group
               });
               return;
            case GROUP_MESSAGE:
               if(onMessage!=null) onMessage.accept(new String[]{
                  e.user.name, e.group, e.message
               });
               return;
            default:
               if(onError!=null) onError.accept(new Exception("Unsupported event: "+args));
         }
      }
      catch(Exception e){
         if(onError!=null) onError.accept(e);
      }
   }
}
