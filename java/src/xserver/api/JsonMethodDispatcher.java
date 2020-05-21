package xserver.api;

import java.util.*;
import java.util.function.*;
import java.lang.reflect.*;

import xserver.util.*;

public abstract class JsonMethodDispatcher implements JsonService{
   private static final Class<?>[] argTypes={
      JsonServiceConnection.class,
      JsonNode.class,
      Consumer.class
   };
   
   private final Map<String, Method> methods;
   
   protected JsonMethodDispatcher(){
      Map<String, Method> map=new HashMap<>();
      for(Method m: getClass().getMethods()){
         if(Arrays.equals(m.getParameterTypes(), argTypes)){
            map.put(m.getName(), m);
         }
      }
      methods=Collections.unmodifiableMap(map);
   }
   
   public final void call(
      JsonServiceConnection conn, String method, JsonNode args, Consumer<JsonNode> resp
   ){
      Method m=methods.get(method);
      if(m==null){
         Util.sendError(resp, "Method not found: "+m);
         return;
      }
      try{
         m.invoke(this, new Object[]{conn, args, resp});
      }
      catch(InvocationTargetException e){
         Util.sendError(resp, e.getCause());
      }
      catch(Exception e){
         Util.sendError(resp, e);
      }
   }
}
