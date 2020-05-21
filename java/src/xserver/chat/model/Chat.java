package xserver.chat.model;

import java.util.*;
import java.util.concurrent.*;

public class Chat{
   Map<String, Set<User>> groups=new ConcurrentHashMap<>();
   
   public Collection<String> listGroups(){
      return groups.keySet();
   }
   
   public void subscribe(User u, String... groupNames){
      for(String groupName: groupNames){
         Set<User> group=groups.get(groupName);
         if(group==null) {
            group=ConcurrentHashMap.<User>newKeySet();
            groups.put(groupName, group);
         }
         if(group.contains(u)) return;
         
         Event e=new Event(Event.Type.USER_SUBSCRIBED, u, groupName, null);
         for(User u0: group) u0.eventHandler.accept(e);
         group.add(u);
      }
   }
   
   public void unsubscribe(User u, String... groupNames){
      for(String groupName: groupNames){
         Set<User> group=groups.get(groupName);
         if(group==null || !group.contains(u)) continue;
         group.remove(u);
         Event e=new Event(Event.Type.USER_UNSUBSCRIBED, u, groupName, null);
         for(User u0: group) u0.eventHandler.accept(e);
      }
   }
   
   public void unsubscribeAll(User u){
      Collection<String> groups=listGroups();
      unsubscribe(u, groups.toArray(new String[groups.size()]));
   }
   
   public void sendMessage(User from, String message, String... groupNames){
      for(String groupName: groupNames){
         Set<User> group=groups.get(groupName);
         if(group==null || !group.contains(from)) continue;
         Event e=new Event(Event.Type.GROUP_MESSAGE, from, groupName, message);
         for(User u: group) if(!u.equals(from)) u.eventHandler.accept(e);
      }
   }
   
   public static void main(String[] args) {
      Chat chat=new Chat();
      User u1=new User("user1", e->System.out.println("user1: "+e));
      User u2=new User("user2", e->System.out.println("user2: "+e));
      User u3=new User("user3", e->System.out.println("user3: "+e));
      
      chat.subscribe(u1, "group1");
      chat.subscribe(u1, "group2");
      chat.subscribe(u1, "group3");
      chat.subscribe(u2, "group2");
      chat.subscribe(u2, "group3");
      chat.subscribe(u3, "group3");
      System.out.println(chat.groups);
      
      chat.sendMessage(u1, "hi from user1", "group1", "group2", "group3");
      chat.sendMessage(u2, "hi from user2", "group1", "group2", "group3");
      chat.sendMessage(u3, "hi from user3", "group1", "group2", "group3");
      
      chat.unsubscribeAll(u3);
      System.out.println(chat.groups);
      chat.sendMessage(u1, "hi from user1", "group1", "group2", "group3");
      
      chat.unsubscribeAll(u2);
      System.out.println(chat.groups);
      chat.sendMessage(u1, "hi from user1", "group1", "group2", "group3");
   }   
}
