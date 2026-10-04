package com.repforge.app;
import android.content.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class HealthBackgroundServiceTest {
 private Context context;private HealthJournalStore store;private ServiceController<HealthBackgroundService> controller;
 @Before public void setup()throws Exception{
  context=RuntimeEnvironment.getApplication();java.lang.reflect.Field field=HealthJournalStore.class.getDeclaredField("instance");field.setAccessible(true);field.set(null,null);context.deleteDatabase("health-journal.db");context.getSharedPreferences("ble_selected_watch",0).edit().clear().commit();store=HealthJournalStore.get(context);
 }
 @After public void cleanup()throws Exception{if(controller!=null)controller.destroy();store.close();java.lang.reflect.Field field=HealthJournalStore.class.getDeclaredField("instance");field.setAccessible(true);field.set(null,null);}
 private void enable()throws Exception{new BleWatchPreference(context).save("AA:BB:CC:DD:EE:FF","GT4Pro+",HealthBackgroundService.SERVICE.toString());store.setPreferences(new JSONObject().put("logWatchSteps",true).put("bleBackground",true));}
 @Test public void closedTaskRetainsEnabledServiceAndNotificationStopDisablesIt()throws Exception{
  enable();controller=Robolectric.buildService(HealthBackgroundService.class).create();HealthBackgroundService service=controller.get();service.onStartCommand(new Intent(context,HealthBackgroundService.class),0,1);
  assertNotNull(Shadows.shadowOf(service).getLastForegroundNotification());assertTrue(HealthBackgroundService.snapshot(context).getBoolean("running"));
  service.onTaskRemoved(new Intent());assertTrue(HealthBackgroundService.snapshot(context).getBoolean("running"));
  service.onStartCommand(new Intent(context,HealthBackgroundService.class).setAction(HealthBackgroundService.STOP),0,2);
  assertFalse(store.preferences().getBoolean("bleBackground"));assertFalse(HealthBackgroundService.snapshot(context).getBoolean("running"));
  Object trial=new Object();try{assertTrue(BleConnectionLease.acquire(trial));}finally{BleConnectionLease.release(trial);}
 }
 @Test public void destructionKeepsOptInForNextForegroundReopen()throws Exception{
  enable();controller=Robolectric.buildService(HealthBackgroundService.class).create();controller.get().onStartCommand(new Intent(context,HealthBackgroundService.class),0,1);controller.destroy();controller=null;
  assertTrue(store.preferences().getBoolean("bleBackground"));assertFalse(HealthBackgroundService.snapshot(context).getBoolean("running"));
 }
 @Test public void bluetoothPermissionDoesNotGrantHealthConnectBackgroundAccess()throws Exception{assertTrue(HealthBackgroundService.permission(context));assertFalse(HealthBackgroundJob.supported(context));assertFalse(HealthBackgroundJob.permitted(context));}
}
