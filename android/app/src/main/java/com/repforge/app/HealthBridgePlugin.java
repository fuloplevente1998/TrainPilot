package com.repforge.app;

import android.os.Build;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.*;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.util.concurrent.TimeoutException;

/** TrainPilot 2.4 platform Health Connect bridge. */
@CapacitorPlugin(name="HealthBridge",permissions={
 @Permission(alias="heart",strings={"android.permission.health.READ_HEART_RATE"}),
 @Permission(alias="restingHeart",strings={"android.permission.health.READ_RESTING_HEART_RATE"}),
 @Permission(alias="calories",strings={"android.permission.health.READ_ACTIVE_CALORIES_BURNED"}),
 @Permission(alias="totalCalories",strings={"android.permission.health.READ_TOTAL_CALORIES_BURNED"}),
 @Permission(alias="exercise",strings={"android.permission.health.READ_EXERCISE"}),
 @Permission(alias="sleep",strings={"android.permission.health.READ_SLEEP"}),
 @Permission(alias="hrv",strings={"android.permission.health.READ_HEART_RATE_VARIABILITY"}),
 @Permission(alias="steps",strings={"android.permission.health.READ_STEPS"}),
 @Permission(alias="weight",strings={"android.permission.health.READ_WEIGHT"}),
 @Permission(alias="bodyFat",strings={"android.permission.health.READ_BODY_FAT"}),
 @Permission(alias="oxygen",strings={"android.permission.health.READ_OXYGEN_SATURATION"}),
 @Permission(alias="vo2",strings={"android.permission.health.READ_VO2_MAX"}),
 @Permission(alias="distance",strings={"android.permission.health.READ_DISTANCE"}),
 @Permission(alias="speed",strings={"android.permission.health.READ_SPEED"}),
 @Permission(alias="bloodPressure",strings={"android.permission.health.READ_BLOOD_PRESSURE"}),
 @Permission(alias="bloodGlucose",strings={"android.permission.health.READ_BLOOD_GLUCOSE"}),
 @Permission(alias="respiratory",strings={"android.permission.health.READ_RESPIRATORY_RATE"}),
 @Permission(alias="background",strings={"android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"}),
 @Permission(alias="writeExercise",strings={"android.permission.health.WRITE_EXERCISE"})
})
public class HealthBridgePlugin extends Plugin {
 private volatile boolean foreground=true;
 @Override protected void handleOnPause(){foreground=false;}
 @Override protected void handleOnResume(){foreground=true;}
 private void requireForeground(){if(!foreground)throw new IllegalStateException("Keep the app open for Health Connect sync");}
 private final ThreadLocal<Long> journalGeneration = new ThreadLocal<>();
 private void journalRead(PluginCall call, java.util.concurrent.Callable<JSObject> action){
  if(!supported(call))return;final long epoch=HealthJournalStore.get(getContext()).generation();
  getBridge().execute(()->{journalGeneration.set(epoch);try{call.resolve(action.call());}catch(Exception e){call.reject(message(e));}finally{journalGeneration.remove();}});
 }
 private static final String[] READ_ALIASES={"heart","restingHeart","calories","totalCalories","exercise","sleep","hrv","steps","weight","bodyFat","oxygen","vo2","distance","speed","bloodPressure","bloodGlucose","respiratory"};
 private static final String[] READ_PERMS={"READ_HEART_RATE","READ_RESTING_HEART_RATE","READ_ACTIVE_CALORIES_BURNED","READ_TOTAL_CALORIES_BURNED","READ_EXERCISE","READ_SLEEP","READ_HEART_RATE_VARIABILITY","READ_STEPS","READ_WEIGHT","READ_BODY_FAT","READ_OXYGEN_SATURATION","READ_VO2_MAX","READ_DISTANCE","READ_SPEED","READ_BLOOD_PRESSURE","READ_BLOOD_GLUCOSE","READ_RESPIRATORY_RATE"};
 private String localized(PluginCall call,String[] text){String lang=call.getString("language",java.util.Locale.getDefault().getLanguage());return text[lang.equals("hu")?0:lang.equals("de")?2:lang.equals("ro")?3:1];}
 private boolean supported(PluginCall call){if(Build.VERSION.SDK_INT<34){call.reject(localized(call,new String[]{"A Health Connect szinkron Android 14 vagy újabb rendszert igényel.","Health Connect sync requires Android 14 or later.","Health Connect benötigt Android 14 oder neuer.","Sincronizarea Health Connect necesită Android 14 sau mai nou."}));return false;}if(getContext().getSystemService(HealthConnectManager.class)==null){call.reject(localized(call,new String[]{"A Health Connect ezen a készüléken nem érhető el.","Health Connect is unavailable on this device.","Health Connect ist auf diesem Gerät nicht verfügbar.","Health Connect nu este disponibil pe acest dispozitiv."}));return false;}return true;}
 private boolean allowed(String suffix){return getContext().checkSelfPermission("android.permission.health."+suffix)==PackageManager.PERMISSION_GRANTED;}
 private boolean anyReadAllowed(){for(String n:READ_PERMS)if(allowed(n))return true;return false;}
 private JSObject permissionStatus(){JSObject r=new JSObject();for(String n:READ_PERMS)r.put(n,allowed(n));r.put("WRITE_EXERCISE",allowed("WRITE_EXERCISE"));return r;}
 @PluginMethod public void getStatus(PluginCall c){if(!supported(c))return;JSObject r=new JSObject();r.put("permissions",permissionStatus());r.put("backgroundSupported",HealthBackgroundJob.supported(getContext()));r.put("backgroundGranted",allowed("READ_HEALTH_DATA_IN_BACKGROUND"));c.resolve(r);}
 @PluginMethod public void requestRead(PluginCall c){if(!supported(c))return;getActivity().runOnUiThread(()->requestPermissionForAliases(READ_ALIASES,c,"readGranted"));}
 @PermissionCallback private void readGranted(PluginCall c){JSObject r=new JSObject();r.put("granted",anyReadAllowed());r.put("permissions",permissionStatus());c.resolve(r);}
 @PluginMethod public void requestSteps(PluginCall c){if(!supported(c))return;if(allowed("READ_STEPS")){stepsGranted(c);return;}getActivity().runOnUiThread(()->requestPermissionForAlias("steps",c,"stepsGranted"));}
 @PermissionCallback private void stepsGranted(PluginCall c){JSObject r=new JSObject();r.put("granted",allowed("READ_STEPS"));r.put("permissions",permissionStatus());c.resolve(r);}
 @PluginMethod public void requestWrite(PluginCall c){if(!supported(c))return;if(allowed("WRITE_EXERCISE")){writeGranted(c);return;}getActivity().runOnUiThread(()->requestPermissionForAlias("writeExercise",c,"writeGranted"));}
 @PermissionCallback private void writeGranted(PluginCall c){JSObject r=new JSObject();r.put("granted",allowed("WRITE_EXERCISE"));c.resolve(r);}
 @PluginMethod public void openSettings(PluginCall c){if(!supported(c))return;try{getActivity().startActivity(new Intent("android.health.connect.action.HEALTH_CONNECT_SETTINGS"));c.resolve();}catch(Exception e){c.reject("Keresd a Health Connect menüt az Android beállításaiban.");}}
 @PluginMethod public void readWorkout(PluginCall c){journalRead(c,()->{JSObject r=HealthConnectApi34.readTrainingWindow(access(),c);r.put("source","all");JSArray a=new JSArray();a.put("all");r.put("sources",a);JSObject labels=new JSObject();labels.put("all","Health Connect • összes forrás");r.put("sourceLabels",labels);return r;});}
 @PluginMethod public void readTrainingWindow(PluginCall c){journalRead(c,()->HealthConnectApi34.readTrainingWindow(access(),c));}
 @PluginMethod public void readStepsWindow(PluginCall c){if(!supported(c))return;getBridge().execute(()->{try{c.resolve(HealthConnectApi34.readStepsWindow(access(),c));}catch(Exception e){c.reject(message(e));}});}
 @PluginMethod public void readHealthDay(PluginCall c){journalRead(c,()->HealthConnectApi34.readHealthDay(access(),c.getData()));}
 @PluginMethod public void readRecovery(PluginCall c){journalRead(c,()->HealthConnectApi34.readRecovery(access(),c));}
 @PluginMethod public void readWellness(PluginCall c){journalRead(c,()->HealthConnectApi34.readWellness(access(),c));}
 @PluginMethod public void createChangeToken(PluginCall c){if(!supported(c))return;getBridge().execute(()->{try{c.resolve(HealthConnectApi34.createChangeToken(access(),c));}catch(Exception e){c.reject(message(e));}});}
 @PluginMethod public void pollChanges(PluginCall c){journalRead(c,()->HealthConnectApi34.pollChanges(access(),c));}
 @PluginMethod public void writeWorkout(PluginCall c){if(!supported(c))return;if(!allowed("WRITE_EXERCISE")){c.reject("Edzésírási engedély szükséges.");return;}getBridge().execute(()->{try{HealthConnectApi34.write(access(),c);c.resolve();}catch(Exception e){c.reject(message(e));}});}
 private String message(Exception e){Throwable t=e;while(t.getCause()!=null)t=t.getCause();if(t instanceof SecurityException)return "Hiányzik egy szükséges Health Connect engedély. Engedélyezd az Egészség oldalon.";if(t instanceof TimeoutException)return "A Health Connect nem válaszolt időben. Próbáld újra.";if(t instanceof IllegalArgumentException)return "Érvénytelen Health Connect időszak vagy szinkron token.";return "A Health Connect művelet nem sikerült: "+(t.getMessage()==null?t.getClass().getSimpleName():t.getMessage());}

 @PluginMethod public void requestBackground(PluginCall c){if(!supported(c))return;if(!HealthBackgroundJob.supported(getContext())){c.reject("Background Health Connect is unavailable on this Android version");return;}getActivity().runOnUiThread(()->requestPermissionForAlias("background",c,"backgroundGranted"));}
 @PermissionCallback private void backgroundGranted(PluginCall c){JSObject r=new JSObject();r.put("granted",allowed("READ_HEALTH_DATA_IN_BACKGROUND"));c.resolve(r);}
 private Access access(){return new Access(getContext(),journalGeneration.get(),this::requireForeground);}
 static final class Access {
  private final Context context;final Long epoch;final Runnable guard;String origin="";
  Access(Context context,Long epoch,Runnable guard){this.context=context.getApplicationContext();this.epoch=epoch;this.guard=guard;}
  Context getContext(){return context;}
  void requireForeground(){guard.run();}
  boolean allowed(String suffix){return context.checkSelfPermission("android.permission.health."+suffix)==PackageManager.PERMISSION_GRANTED;}
  boolean anyReadAllowed(){for(String n:READ_PERMS)if(allowed(n))return true;return false;}
  JSObject permissionStatus(){JSObject r=new JSObject();for(String n:READ_PERMS)r.put(n,allowed(n));return r;}
  String message(Exception e){Throwable t=e;while(t.getCause()!=null)t=t.getCause();return t instanceof SecurityException?"Permission denied":t instanceof TimeoutException?"Health Connect timed out":t.getClass().getSimpleName();}
 }
}
