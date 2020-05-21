package xserver.util;

import java.util.Timer;
import java.util.TimerTask;

public class Scheduler{
   private static final Timer timer=new Timer();
   
   //returns cancel
   public static Runnable schedule(final Runnable job, long delay){
      final TimerTask task=new TimerTask(){
         public void run(){
            job.run();
         }
      };
      timer.schedule(task, delay);
      return new Runnable(){
         public void run(){
            task.cancel();
         }
      };
   }
}
