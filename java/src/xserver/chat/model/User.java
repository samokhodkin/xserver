package xserver.chat.model;

import java.util.function.*;

public class User {
   public final String name;
   public final transient Consumer<Event> eventHandler;
   
   public User(String name, Consumer<Event> eventHandler){
      this.name=name;
      this.eventHandler=eventHandler;
   }
   
   @Override
   public int hashCode(){
      return name.hashCode();
   }
   
   @Override
   public boolean equals(Object o){
      if(!(o instanceof User)) return false;
      return ((User)o).name.equals(name);
   }
   
   public String toString(){
      return name;
   }
}
