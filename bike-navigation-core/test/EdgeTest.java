import com.example.bicyclesafety.navigation.*;
import java.util.*;
import java.util.concurrent.*;
public class EdgeTest {
  public static void main(String[] a) throws Exception {
    Queue<Runnable> main=new ArrayDeque<>(); List<String> log=new ArrayList<>();
    NavigationSession.Listener L=new NavigationSession.SimpleListener(){
      public void onUpdate(NavigationSession.State s){ if(s.offRoute) log.add("OFF"); }
      public void onRerouteStarted(boolean au){log.add("REROUTE auto="+au);}
      public void onRouteChanged(NavRoute r){log.add("CHANGED");}
      public void onRerouteFailed(String m){log.add("FAIL "+m);}
      public void onEnded(){log.add("ENDED");}
    };
    NavRoute r=CoreTest.route(0,0);
    NavigationSession nav=new NavigationSession(L,(lat,lng,p)->CoreTest.route(0,0),main::add,Runnable::run);
    nav.start(r); long t=0;
    for(int i=0;i<5;i++) nav.onLocation(37.5+100/111320.0,127.0+0.001,50f,-1f,t+=1000); // 88m off but accuracy 50
    log.add("1) inaccurate -> offRoute events: "+log.stream().filter(s->s.equals("OFF")).count());
    nav.reroute(); log.add("2) manual reroute pending="+main.size());
    nav.stop(); while(!main.isEmpty()) main.poll().run();
    log.add("3) after stop, active="+nav.isActive());
    NavigationSession nav2=new NavigationSession(L,(lat,lng,p)->{throw new java.io.IOException("네트워크 없음");},main::add,Runnable::run);
    nav2.start(r); nav2.onLocation(37.5,127.0,5f,-1f,1000); nav2.reroute(); while(!main.isEmpty()) main.poll().run();
    NavigationSession nav3=new NavigationSession(L,null,main::add,Runnable::run); nav3.start(r); nav3.onLocation(37.5,127.0,5f,-1f,1000); nav3.reroute(); log.add("4) null rerouter ok, active="+nav3.isActive());
    log.forEach(System.out::println);
  }
}
