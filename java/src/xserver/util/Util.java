package xserver.util;

import java.util.function.*;

public class Util{
   public static JsonNode errorNode(String message){
      return JsonNode.object().set("error",message);
   }
   
   public static JsonNode errorNode(Throwable e){
      return JsonNode.object()
         .set("error",e.getMessage())
         .set("javaClass", e.getClass().getName())
         .set("javaInstance", e)
      ;
   }
   
   public static Throwable errorInstance(JsonNode obj) throws Exception{
      if(obj.get("javaClass")!=null) return (Throwable)obj.get("javaInstance").instance(Class.forName(obj.get("javaClass").string()));
      else return new Error(obj.get("error").string());
   }
   
   public static void sendError(Consumer<JsonNode> resp, String message){
      if(resp!=null) resp.accept(errorNode(message));
      else System.out.println("ERROR: "+message);
   }
   
   public static void sendError(Consumer<JsonNode> resp, Throwable e){
      if(resp!=null) resp.accept(errorNode(e));
      else e.printStackTrace();
   }
   
   public static void print(){
      System.out.println();
   }
   
   public static void print(String... lines){
      synchronized(System.out){
         for(String s: lines){
            if(s==null) System.out.println();
            else System.out.println(s);
         }
      }
   }
   
   public static void print(Throwable e, String... context){
      synchronized(System.out){
         print(context);
         e.printStackTrace(System.out);
      }
   }
   
   public static void main(String[] args) throws Exception{
      JsonNode errObj=errorNode(new Exception("test"));
      System.out.println(errObj);
      System.out.println(errorInstance(errObj));
   }
}
