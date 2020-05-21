package xserver.util;

import java.util.*;
import java.util.concurrent.*;

public class TimedMap<K,V>{
   protected static class Entry<V>{
      protected V value;
      protected Runnable cancel;
      
      public String toString(){
         return String.valueOf(value);
      }
   }
   
   private final Map<K,TimedMap.Entry<V>> map=new ConcurrentHashMap<K,TimedMap.Entry<V>>();
   
   public V put(K key, V value, long delay, Runnable onExpire){
      Entry<V> e=new Entry<>();
      e.value=value;
      e.cancel=Scheduler.schedule(()->{
         map.remove(key);
         if(onExpire!=null) onExpire.run();
      }, delay);
      Entry<V> e0=map.put(key, e);
      if(e0==null) return null;
      e0.cancel.run();
      return e0.value;
   }
   
   public V get(K key){
      Entry<V> e=map.get(key);
      if(e==null) return null;
      return e.value;
   }
   
   public V remove(K key){
      Entry<V> e=map.remove(key);
      if(e==null) return null;
      return e.value;
   }
   
   public static void main(String[] args) throws Exception{
      TimedMap<Integer, String> map=new TimedMap<>();
      map.put(1,"a",250,()->System.out.println("1 expired"));
      map.put(2,"b",750,()->System.out.println("2 expired"));
      map.put(3,"c",1250,()->System.out.println("3 expired"));
      System.out.println(map.map);
      Thread.sleep(500);
      System.out.println(map.map);
      Thread.sleep(500);
      System.out.println(map.map);
      Thread.sleep(500);
      System.out.println(map.map);
   }
}
