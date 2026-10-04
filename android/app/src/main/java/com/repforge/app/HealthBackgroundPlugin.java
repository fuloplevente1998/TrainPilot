package com.repforge.app;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import org.json.JSONObject;

@CapacitorPlugin(name="HealthBackground",permissions={
 @Permission(alias="connect",strings={Manifest.permission.BLUETOOTH_CONNECT}),
 @Permission(alias="notifications",strings={Manifest.permission.POST_NOTIFICATIONS})
})
public class HealthBackgroundPlugin extends Plugin {
 @PluginMethod public void getStatus(PluginCall call){try{JSObject result=JSObject.fromJSONObject(HealthBackgroundService.snapshot(getContext()));result.put("hcSupported",HealthBackgroundJob.supported(getContext()));result.put("hcGranted",HealthBackgroundJob.permitted(getContext()));result.put("preferences",HealthJournalStore.get(getContext()).preferences());call.resolve(result);}catch(Exception e){call.reject("Background status unavailable");}}
 @PluginMethod public void configure(PluginCall call){
  String channel=call.getString("channel","");boolean enabled=call.getBoolean("enabled",false);
  if(!channel.equals("bleBackground")&&!channel.equals("hcBackground")){call.reject("Invalid background channel");return;}
  if(enabled&&channel.equals("bleBackground")){
   BleWatchPreference watch=new BleWatchPreference(getContext());
   if(watch.address().isEmpty()||!HealthBackgroundService.SERVICE.toString().equals(watch.service())){call.reject("Remember a verified RDFit watch first","NO_SAVED_WATCH");return;}
   if(Build.VERSION.SDK_INT>=31&&!HealthBackgroundService.permission(getContext())){requestPermissionForAlias("connect",call,"permissionResult");return;}
   if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(getContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissionForAlias("notifications",call,"permissionResult");return;}
  }
  apply(call);
 }
 @PermissionCallback private void permissionResult(PluginCall call){
  if(!HealthBackgroundService.permission(getContext())||Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(getContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){call.reject("Bluetooth and notification permission are required","PERMISSION_DENIED");return;}
  configure(call);
 }
 private void apply(PluginCall call){try{
  String channel=call.getString("channel","");boolean enabled=call.getBoolean("enabled",false);
  if(enabled&&channel.equals("hcBackground")&&!HealthBackgroundJob.permitted(getContext())){call.reject("Enable Health Connect background read permission first","PERMISSION_DENIED");return;}
  HealthJournalStore store=HealthJournalStore.get(getContext());JSONObject prefs=new JSONObject().put(channel,enabled);
  if(channel.equals("bleBackground")&&enabled)prefs.put("logWatchSteps",true);
  store.setPreferences(prefs);
  try{
   if(channel.equals("bleBackground")){if(enabled){Intent intent=new Intent(getContext(),HealthBackgroundService.class).putExtra("language",call.getString("language","hu"));ContextCompat.startForegroundService(getContext(),intent);}else HealthBackgroundService.stop(getContext());}
   else HealthBackgroundJob.schedule(getContext());
  }catch(Exception e){store.setPreferences(new JSONObject().put(channel,false));throw e;}
  call.resolve(JSObject.fromJSONObject(store.projection()));
 }catch(Exception e){call.reject("Background connection could not be changed","BACKGROUND_FAILED",e);}}
 @PluginMethod public void refreshWatch(PluginCall call){try{if(!HealthBackgroundService.configured(getContext())){call.reject("Background watch connection is disabled");return;}ContextCompat.startForegroundService(getContext(),new Intent(getContext(),HealthBackgroundService.class).setAction(HealthBackgroundService.REFRESH));call.resolve();}catch(Exception e){call.reject("Watch refresh unavailable");}}
}
