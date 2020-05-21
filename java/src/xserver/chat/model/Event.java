package xserver.chat.model;

public class Event {
   public enum Type{
      USER_SUBSCRIBED,
      USER_UNSUBSCRIBED,
      GROUP_MESSAGE,
      PRIVATE_MESSAGE
   }
   
   public final Type type;
   public final User user;
   public final String group;
   public final String message;
   
   public Event(Type type, User user, String group, String message){
      this.type=type;
      this.user=user;
      this.group=group;
      this.message=message;
   }
   
   public String toString(){
      return "{"+type+" "+user+" -> "+group+": "+message+"}";
   }
}
