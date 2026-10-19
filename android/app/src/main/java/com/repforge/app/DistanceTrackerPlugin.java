package com.repforge.app;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.*;
import androidx.core.content.ContextCompat;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;

@CapacitorPlugin(name="DistanceTracker",permissions={
 @Permission(alias="location",strings={Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION}),
 @Permission(alias="notifications",strings={Manifest.permission.POST_NOTIFICATIONS})
})
public class DistanceTrackerPlugin extends Plugin {
 private boolean starting;
 private String message(PluginCall c,String hu,String en,String de,String ro){return DistanceTrackingService.text(c.getString("language","en"),hu,en,de,ro);}
 @PluginMethod public void status(PluginCall c){getActivity().runOnUiThread(()->{try{c.resolve(new JSObject(DistanceTrackingService.snapshot(getContext()).toString()));}catch(Exception e){c.reject(e.getMessage());}});}
 @PluginMethod public void stop(PluginCall c){getActivity().runOnUiThread(()->{try{c.resolve(new JSObject(DistanceTrackingService.stop(getContext()).toString()));}catch(Exception e){c.reject(e.getMessage());}});}
 @PluginMethod public void start(PluginCall c){
  String key=c.getString("key","");if(!key.matches("[A-Za-z0-9:._|+\\-]{1,200}")){c.reject("Invalid tracking key.");return;}
  if(starting){c.reject(message(c,"GPS-indítás folyamatban.","GPS is starting.","GPS wird gestartet.","GPS pornește."));return;}
  if(DistanceTrackingService.snapshot(getContext()).optBoolean("active")){if(key.equals(DistanceTrackingService.snapshot(getContext()).optString("key"))){status(c);return;}c.reject(message(c,"Előbb állítsd le a futó GPS-mérést.","Stop the current GPS measurement first.","Zuerst die laufende GPS-Messung stoppen.","Oprește întâi măsurarea GPS curentă."));return;}
  starting=true;
  if(ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)requestPermissionForAlias("location",c,"locationResult");else notifications(c);
 }
 @PermissionCallback private void locationResult(PluginCall c){
  if(ContextCompat.checkSelfPermission(getContext(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){starting=false;c.reject(message(c,"A GPS-hez pontos helyengedély kell. A távolság kézzel továbbra is megadható.","GPS needs precise location permission. Manual distance entry remains available.","GPS benötigt genauen Standortzugriff. Die Strecke kann weiterhin manuell eingetragen werden.","GPS necesită permisiune de localizare precisă. Distanța poate fi introdusă manual."));return;}notifications(c);
 }
 private void notifications(PluginCall c){if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(getContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissionForAlias("notifications",c,"notificationResult");else begin(c);}
 @PermissionCallback private void notificationResult(PluginCall c){begin(c);}
 private void begin(PluginCall c){getActivity().runOnUiThread(()->{
  try{
   LocationManager manager=(LocationManager)getContext().getSystemService(Context.LOCATION_SERVICE);
   if(manager==null||!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){starting=false;c.reject(message(c,"Kapcsold be a telefon helymeghatározását/GPS-ét. A távolság kézzel is megadható.","Enable location/GPS on your phone. You can also enter the distance manually.","Standort/GPS am Telefon aktivieren. Manuelle Streckeneingabe ist möglich.","Activează localizarea/GPS pe telefon. Poți introduce distanța manual."));return;}
   ContextCompat.startForegroundService(getContext(),new Intent(getContext(),DistanceTrackingService.class).setAction(DistanceTrackingService.START).putExtra("key",c.getString("key","")).putExtra("name",c.getString("name","TrainPilot")).putExtra("language",c.getString("language","en")));
   Handler handler=new Handler(Looper.getMainLooper());handler.postDelayed(new Runnable(){int attempts;public void run(){
    org.json.JSONObject state=DistanceTrackingService.snapshot(getContext());
    if(state.optBoolean("active")&&c.getString("key","").equals(state.optString("key"))){starting=false;try{c.resolve(new JSObject(state.toString()));}catch(Exception e){c.reject(e.getMessage());}return;}
    String error=c.getString("key","").equals(state.optString("key"))?state.optString("error"):"";
    if(++attempts<30&&error.isEmpty()){handler.postDelayed(this,100);return;}
    starting=false;c.reject(!error.isEmpty()?error:message(c,"A GPS-mérés nem indult el. Próbáld újra az app megnyitása után.","GPS did not start. Retry with the app open.","GPS startete nicht. In der geöffneten App erneut versuchen.","GPS nu a pornit. Reîncearcă din aplicația deschisă."));
   }},100);
  }catch(Exception e){starting=false;c.reject(message(c,"A GPS-mérés nem indult el. Nyisd meg az appot és ellenőrizd az engedélyeket.","GPS did not start. Open the app and check permissions.","GPS startete nicht. App öffnen und Berechtigungen prüfen.","GPS nu a pornit. Deschide aplicația și verifică permisiunile."));}
 });}

 /** Sensitive coordinates only cross the native bridge after an explicit matching-key request. */
 @PluginMethod public void route(PluginCall call){
  String key=call.getString("key","");
  if(!GpsRouteStore.validKey(key)){call.reject("Invalid route owner key");return;}
  getBridge().execute(()->{
   try{
    if(key.matches("wear193:[a-f0-9]{64}")){
     call.resolve(new JSObject(com.repforge.shared.GpsRouteLedger193.open(getContext().getNoBackupFilesDir()).view(key).toString()));return;
    }
    GpsRouteStore.Route track=new GpsRouteStore(getContext().getNoBackupFilesDir()).read(key);
    JSObject result=new JSObject();
    result.put("key",key);result.put("pointCount",track.pointCount);result.put("partial",track.partial);
    JSArray sections=new JSArray();
    for(GpsRouteTrace.Segment segment:track.segments){
     JSObject section=new JSObject();
     section.put("ordinal",segment.ordinal);
     JSArray points=new JSArray();
     for(GpsRouteTrace.Point point:segment.points){
      JSObject p=new JSObject();
      p.put("latitude",point.latitude);p.put("longitude",point.longitude);
      p.put("accuracyMeters",point.accuracyMeters);
      p.put("elapsedMillis",point.elapsedMillis);p.put("epochMillis",point.epochMillis);
      points.put(p);
     }
     section.put("points",points);sections.put(section);
    }
    result.put("segments",sections);call.resolve(result);
   }catch(Exception e){call.reject("Route unavailable",e);}
  });
 }

 /** References only; native coordinates stay private until route() is explicitly requested. */
 @PluginMethod public void routeBindings193(PluginCall call){
  String workoutId=call.getString("workoutId","");
  if(workoutId.isEmpty()||workoutId.length()>512){call.reject("Invalid workout identity");return;}
  getBridge().execute(()->{
   try{
    JSObject result=new JSObject();result.put("workoutId",workoutId);
    result.put("bindings",com.repforge.shared.GpsRouteLedger193.open(getContext().getNoBackupFilesDir()).bindings(workoutId));call.resolve(result);
   }catch(Exception e){call.reject("Route references unavailable",e);}
  });
 }
 @PluginMethod public void deleteWorkoutRoutes193(PluginCall call){
  String workoutId=call.getString("workoutId","");
  if(workoutId.isEmpty()||workoutId.length()>512){call.reject("Invalid workout identity");return;}
  getBridge().execute(()->{
   try{com.repforge.shared.GpsRouteLedger193.open(getContext().getNoBackupFilesDir()).deleteWorkout(workoutId);
    JSObject result=new JSObject();result.put("deleted",true);call.resolve(result);
   }catch(Exception e){call.reject("Workout routes could not be erased",e);}
  });
 }

 /** Separate explicit erasure for a deleted workout; never delete routes on ordinary STOP. */
 @PluginMethod public void deleteRoute(PluginCall call){
  String key=call.getString("key","");
  if(!GpsRouteStore.validKey(key)){call.reject("Invalid route owner key");return;}
  getBridge().execute(()->{
   try{
    // Never remove an actively recorded owner's route.
    org.json.JSONObject status=DistanceTrackingService.snapshot(getContext());
    if(status.optBoolean("active")&&key.equals(status.optString("key"))){call.reject("Recording still active");return;}
    if(key.matches("wear193:[a-f0-9]{64}")){
     com.repforge.shared.GpsRouteLedger193.open(getContext().getNoBackupFilesDir()).delete(key);
     JSObject result=new JSObject();result.put("deleted",true);call.resolve(result);return;
    }
    boolean deleted=new GpsRouteStore(getContext().getNoBackupFilesDir()).delete(key);
    if(!deleted){call.reject("Route could not be erased");return;}
    JSObject result=new JSObject();result.put("deleted",true);call.resolve(result);
   }catch(Exception e){call.reject("Route erase failed",e);}
  });
 }
}
