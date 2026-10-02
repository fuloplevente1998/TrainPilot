package com.repforge.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.Locale;

/** User-started visible location service. No route, coordinates, token or map backend. */
public class DistanceTrackingService extends Service implements LocationListener {
    static final String PREFS="trainpilot-distance",START="distance.START",STOP="distance.STOP";
    private static final String CHANNEL="trainpilot_distance";
    private static final int NOTIFICATION=106;
    private static volatile DistanceTrackingService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DistanceAccumulator distance=new DistanceAccumulator();
    private LocationManager locations;
    private volatile boolean active;
    private String key="",name="",language="en",error="";
    private long began,lastFix;
    private float accuracy;
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;save();notifyStatus();handler.postDelayed(this,5000);}};
    static String text(String lang,String hu,String en,String de,String ro){return "hu".equals(lang)?hu:"de".equals(lang)?de:"ro".equals(lang)?ro:en;}
    @Override public void onCreate(){super.onCreate();instance=this;locations=(LocationManager)getSystemService(LOCATION_SERVICE);}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(STOP.equals(intent.getAction())){stopMeasurement();stopSelf();return START_NOT_STICKY;}
        if(active)return START_NOT_STICKY;
        key=intent.getStringExtra("key");name=intent.getStringExtra("name");language=intent.getStringExtra("language");
        if(key==null||language==null){stopSelf();return START_NOT_STICKY;}
        distance=new DistanceAccumulator();lastFix=0;accuracy=0;error="";began=SystemClock.elapsedRealtime();
        try{
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)throw new SecurityException("location");
            if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel(CHANNEL,"TrainPilot GPS",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
            active=true;startForeground(NOTIFICATION,notification());
            locations.requestLocationUpdates(LocationManager.GPS_PROVIDER,3000,3,this,Looper.getMainLooper());
            save();handler.post(tick);
        }catch(Exception e){error=text(language,"A GPS-mérés nem indult el. Ellenőrizd a pontos helyengedélyt és a GPS-t.","GPS tracking could not start. Check precise location permission and GPS.","GPS-Messung konnte nicht starten. Genauen Standortzugriff und GPS prüfen.","Măsurarea GPS nu a pornit. Verifică permisiunea de localizare precisă și GPS.");stopMeasurement();stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onLocationChanged(Location location){
        if(!active)return;
        long stamp=location.getElapsedRealtimeNanos()/1000000L,now=SystemClock.elapsedRealtime();
        if(!location.hasAccuracy()||now-stamp>15000||stamp>now+1000)return;
        if(distance.add(location.getLatitude(),location.getLongitude(),location.getAccuracy(),stamp,location.hasSpeed()?location.getSpeed():Double.NaN)){
            lastFix=stamp;accuracy=location.getAccuracy();save();
        }
    }
    @Override public void onProviderDisabled(String provider){lastFix=0;save();}
    @Override public void onProviderEnabled(String provider){}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
    private JSONObject current(){
        JSONObject out=new JSONObject();try{out.put("key",key);out.put("active",active);out.put("distanceMeters",Math.round(distance.totalMeters()*10)/10d);out.put("elapsedSeconds",Math.max(0,(SystemClock.elapsedRealtime()-began)/1000));out.put("hasFix",lastFix>0&&SystemClock.elapsedRealtime()-lastFix<30000);out.put("accuracyMeters",accuracy);out.put("error",error);}catch(Exception ignored){}return out;
    }
    private void save(){if(key.isEmpty())return;getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("state",current().toString()).apply();}
    private Notification notification(){
        Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,DistanceTrackingService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String title=text(language,"GPS-távolságmérés","GPS distance tracking","GPS-Distanzmessung","Măsurare distanță GPS");
        String body=String.format(Locale.getDefault(),"%.2f km · %s",distance.totalMeters()/1000,name==null?"TrainPilot":name);
        return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(body).setContentIntent(content).setOngoing(true).setOnlyAlertOnce(true).setCategory(NotificationCompat.CATEGORY_SERVICE).addAction(0,text(language,"Leállítás","Stop","Stoppen","Oprește"),stop).build();
    }
    private void notifyStatus(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}
    private void stopMeasurement(){if(active){save();active=false;try{locations.removeUpdates(this);}catch(Exception ignored){}handler.removeCallbacks(tick);save();}else if(!error.isEmpty())save();stopForeground(true);}
    @Override public void onTaskRemoved(Intent rootIntent){stopMeasurement();stopSelf();}
    @Override public void onDestroy(){stopMeasurement();if(instance==this)instance=null;super.onDestroy();}
    static JSONObject snapshot(Context context){
        DistanceTrackingService service=instance;if(service!=null&&service.active)return service.current();
        try{JSONObject out=new JSONObject(context.getSharedPreferences(PREFS,MODE_PRIVATE).getString("state","{}"));out.put("active",false);return out;}catch(Exception e){return new JSONObject();}
    }
    static JSONObject stop(Context context){DistanceTrackingService service=instance;if(service!=null){service.stopMeasurement();service.stopSelf();}return snapshot(context);}
    static void erase(Context context){stop(context);DistanceTrackingService service=instance;if(service!=null){service.key="";service.error="";}context.getSharedPreferences(PREFS,MODE_PRIVATE).edit().clear().apply();}
}
