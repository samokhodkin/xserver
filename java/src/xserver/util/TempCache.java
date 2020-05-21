package xserver.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class TempCache<T>{
   public static final String TEMP_PREFIX="__";
   
   private final AtomicLong temIdGen=new AtomicLong();
   
   private static class TempEntry<V>{
      V value;
      Runnable cancel;
      public String toString(){
         return "TempObject("+value+")";
      }
   }
   private final ConcurrentHashMap<String, TempEntry<T>> db=new ConcurrentHashMap<>();
   
   /*
    * регистрация: объект хранится только до первого вызова 
    * и не дольше указанного срока
    */
   public String put(final T obj, long ttl, final Runnable onExpire){
      final String id=TEMP_PREFIX+Long.toString(temIdGen.incrementAndGet(), 16);
      db.put(id, new TempEntry(){{
         value=obj;
         cancel=Scheduler.schedule(new Runnable(){
            public void run(){
               TempEntry e=db.remove(id);
               if(e!=null && onExpire!=null) onExpire.run();
            }
         }, ttl);
      }});
      return id;
   }
   
   public T get(String id){
      TempEntry<T> e=db.remove(id);
      if(e==null) return null;
      e.cancel.run();
      return e.value;
   }
   
   public static void main(String[] args) throws Exception{
      TempCache<String> cache=new TempCache<>();
      String id1=cache.put("<1>", 500L, ()->System.out.println("<1> expired"));
      String id2=cache.put("<2>", 500L, ()->System.out.println("<2> expired"));
      System.out.println("cache: "+cache.db);
      
      System.out.println("id1: "+cache.get(id1)); //<1>
      Thread.sleep(600); //<2> expired
      System.out.println("id2: "+cache.get(id2)); //null
      System.out.println("cache: "+cache.db);
   }
}
