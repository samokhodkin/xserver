package xserver.util;

import com.google.gson.*;
import java.util.*;
import java.util.stream.*;

/*
 * All Gson in one class
 * author samokhodkin@gmail.com
 */

public class JsonNode{
   private static final Gson gson=new GsonBuilder().setPrettyPrinting().serializeNulls().create();
   
   public static final JsonNode TRUE=valueOf(true);
   public static final JsonNode FALSE=valueOf(false);
   public static final JsonNode NULL=valueOf(null);
   public static final JsonNode EMPTY_ARRAY=valueOf(new Object[0]);
   
   private static class UnmodifiableJsonNode extends JsonNode{
      UnmodifiableJsonNode(JsonElement data){
         super(data);
      }
      public JsonNode set(String field, JsonNode value){
         return this;
      }
      public JsonNode set(String field, Object obj){
         return this;
      }
      public JsonNode add(JsonNode value){
         return this;
      }
      public JsonNode set(int index, JsonNode value){
         return this;
      }
   }
   
   public static JsonNode object(){
      return new JsonNode(new JsonObject());
   }
   
   public static JsonNode array(){
      return new JsonNode(new JsonArray());
   }
   
   public static JsonNode valueOf(Object o){
      return o instanceof JsonNode? (JsonNode)o: new JsonNode(gson.toJsonTree(o));
   }
   
   public static JsonNode unmodifiableValueOf(Object o){
      return new UnmodifiableJsonNode(
         o instanceof JsonNode? ((JsonNode)o).data: gson.toJsonTree(o)
      );
   }
   
   private final JsonElement data;
   
   public JsonNode(String jsonString){
      this(new JsonParser().parse(jsonString));
   }
   
   JsonNode(JsonElement data){
      this.data=data;
   }
   
   public boolean isNull(){
      return data.isJsonNull();
   }
   
   public boolean isPrimitive(){
      return data.isJsonPrimitive();
   }
   
   public boolean isObject(){
      return data.isJsonObject();
   }
   
   public boolean isArray(){
      return data.isJsonArray();
   }
   
   //set object field
   public JsonNode set(String field, JsonNode value){
      data.getAsJsonObject().add(field, value.data);
      return this;
   }
   
   public JsonNode set(String field, Object obj){
      return set(field, valueOf(obj));
   }
   
   //add to array
   public JsonNode add(JsonNode value){
      data.getAsJsonArray().add(value.data);
      return this;
   }
   
   //set array element
   public JsonNode set(int index, JsonNode value){
      data.getAsJsonArray().set(index, value.data);
      return this;
   }
   
   public JsonNode get(String... path){
      JsonElement e=data;
      for(int i=0;i<path.length;i++){
         e=e.getAsJsonObject().get(path[i]);
         if(e==null) return null;
      }
      return new JsonNode(e);
   }
   
   public JsonNode get(int index){
      return new JsonNode(data.getAsJsonArray().get(index));
   }
   
   public int size(){
      return data.getAsJsonArray().size();
   }
   
   //getters
   //JsonNull throws error on any getXXX(), fix it
   
   public String string(){
      return data.isJsonNull()? null: data.getAsString();
   }
   
   public Number number(){
      return data.isJsonNull()? null: data.getAsNumber();
   }
   
   public Boolean bool(){
      return data.isJsonNull()? null: data.getAsBoolean();
   }
   
   public <T> T instance(Class<T> klass){
      return gson.fromJson(data,klass);
   }
   
   public String toString(){
      return gson.toJson(data);
   }
   
   public List<String> fieldNames(){
      return data.getAsJsonObject().entrySet().stream().map(e->e.getKey()).collect(
         Collectors.toList()
      );
   }
   
   public static void main(String[] args){
      System.out.println(valueOf("123"));
      System.out.println(valueOf(123));
      System.out.println(valueOf(true));
      System.out.println(valueOf(new Date()));
      System.out.println(valueOf(null)+" ("+valueOf(null).data.getClass()+")");
      System.out.println();
      
      JsonNode obj=object();
      obj.set("strField", valueOf("abc"));
      obj.set("numField", valueOf(123));
      obj.set("boolField", valueOf(true));
      obj.set("dateField", valueOf(new Date()));
      obj.set("nullField", valueOf(null));
      
      System.out.println(obj);
      System.out.println();
      
      obj=array();
      obj.add(valueOf("abc"));
      obj.add(valueOf(123));
      obj.add(valueOf(true));
      obj.add(valueOf(new Date()));
      obj.add(valueOf(null));
      System.out.println(obj);
      System.out.println();
      
      String s="{\"aaa\":\"abc\",\"bbb\":123,\"ccc\": null}";
      obj=new JsonNode(s);
      System.out.println(obj.get("aaa"));
      System.out.println(obj.get("bbb"));
      System.out.println(obj.get("ccc"));
      System.out.println(obj.get("ddd"));
      
      System.out.println(valueOf(new Tmp()));
   }
   public static class Tmp{
      public int a=1;
      public String b="b";
   }
}
