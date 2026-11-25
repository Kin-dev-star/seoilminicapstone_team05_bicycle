import com.example.bicyclesafety.navigation.*;
import java.util.*;
import java.util.concurrent.*;
public class CoreTest {
  static final double LAT0=37.5, LNG0=127.0, MLAT=1/111320.0, MLNG=1/(111320.0*Math.cos(Math.toRadians(37.5)));
  static NavRoute.Point P(double eastM,double northM){return new NavRoute.Point(LAT0+northM*MLAT,LNG0+eastM*MLNG);}
  static NavRoute route(double startE,double startN){
    // north 500m, left (west) 300m, right (north) 300m to destination at (-300,800)
    List<NavRoute.Point> pts=new ArrayList<>();
    double[][] c={{startE,startN},{0,500},{-300,500},{-300,800}};
    for(int s=1;s<c.length;s++) for(int k=0;k<10;k++){double t=k/10.0; pts.add(P(c[s-1][0]+(c[s][0]-c[s-1][0])*t,c[s-1][1]+(c[s][1]-c[s-1][1])*t));}
    pts.add(P(-300,800));
    List<NavRoute.Step> st=new ArrayList<>();
    st.add(new NavRoute.Step("출발지",pts.get(0).latitude,pts.get(0).longitude));
    NavRoute.Point a=P(0,500),b=P(-300,500),e=P(-300,800);
    st.add(new NavRoute.Step("좌회전",a.latitude,a.longitude));
    st.add(new NavRoute.Step("우회전",b.latitude,b.longitude));
    st.add(new NavRoute.Step("목적지",e.latitude,e.longitude));
    return new NavRoute(pts,st,1100,300,"테스트 도착지",e.latitude,e.longitude,true);
  }
  public static void main(String[] x) throws Exception {
    Queue<Runnable> main=new ArrayDeque<>();
    Executor callback=main::add, worker=Runnable::run;
    List<String> log=new ArrayList<>();
    NavigationSession.Listener L=new NavigationSession.SimpleListener(){
      String last="";
      public void onUpdate(NavigationSession.State s){String k=s.hasFix?(s.maneuver+" "+s.distanceToNext+"m next="+s.nextManeuver+" rem="+s.remainingMeters+"m "+s.remainingSeconds+"s spd="+s.speedKmh+(s.offRoute?" OFF":"")+(s.rerouting?" REROUTING":"")+(s.nearbyHazard!=null?" HZ="+s.nearbyHazard.name:"")):"(no fix) rem="+s.remainingMeters; if(s.distanceToNext%100<5||s.offRoute||!s.hasFix) log.add("  state: "+k);}
      public void onSpeak(String t){log.add("SPEAK: "+t);}
      public void onHazardApproach(NavigationSession.Hazard h,double d){log.add("HAZARD: "+h.name+" "+Math.round(d)+"m");}
      public void onRerouteStarted(boolean a){log.add("REROUTE START auto="+a);}
      public void onRouteChanged(NavRoute r){log.add("ROUTE CHANGED points="+r.points.size()+" dist="+r.distanceMeters);}
      public void onRerouteFailed(String m){log.add("REROUTE FAIL "+m);}
      public void onArrived(){log.add("ARRIVED");}
      public void onEnded(){log.add("ENDED");}
    };
    NavigationSession.Rerouter fake=(lat,lng,prev)->{ double n=(lat-LAT0)/MLAT, e=(lng-LNG0)/MLNG; log.add(String.format("  (rerouter called from %.0fE %.0fN)",e,n)); return route(e,n);};
    NavigationSession nav=new NavigationSession(L,fake,callback,worker);
    nav.setHazards(Arrays.asList(new NavigationSession.Hazard("h1","사고지점A",LAT0+505*MLAT,LNG0-150*MLNG)));
    nav.start(route(0,0));
    log.add("fractions="+Arrays.toString(nav.hazardFractions(150)));
    long t=0; double sE=0,sN=0;
    // ride north 5m/s with 1s fixes, accuracy 10
    java.util.function.BiConsumer<Double,Double> fix=(e,n)->{};
    for(int i=0;i<=100;i++){ double n=i*5; nav.onLocation(LAT0+n*MLAT,LNG0,10f,-1f,(t+=1000)); }
    // left leg: go west; at -100 E, drift off-route north by 60m for 4 fixes
    for(int i=1;i<=20;i++){ nav.onLocation(LAT0+500*MLAT,LNG0-i*5*MLNG,10f,5f,(t+=1000)); }
    for(int i=0;i<4;i++){ nav.onLocation(LAT0+560*MLAT,LNG0-(100+i*5)*MLNG,10f,5f,(t+=1000)); while(!main.isEmpty()) main.poll().run(); }
    log.add("after reroute active="+nav.isActive()+" dest="+nav.getRoute().destinationName);
    // continue along new route from current point: rejoin -120E,560N -> (0,500)? new route goes north to 500?? simulate going along new route points
    NavRoute r=nav.getRoute();
    for(NavRoute.Point p: r.points){ nav.onLocation(p.latitude,p.longitude,10f,5f,(t+=1000)); if(!nav.isActive()) break; }
    for(String s: log) System.out.println(s);
  }
}
