package com.repforge.app;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.*;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-enabled watch connection. Independent of the WebView; only verified battery/steps queries. */
public class HealthBackgroundService extends Service {
 static final String STOP="health.WATCH_STOP",REFRESH="health.WATCH_REFRESH",CHANNEL="trainpilot_health_watch",STATUS="health_background_status";
 static final UUID SERVICE=UUID.fromString("6e40ab01-b5a3-f393-e0a9-e50e24dcca9e"),WRITE=UUID.fromString("6e40ab02-b5a3-f393-e0a9-e50e24dcca9e"),NOTIFY=UUID.fromString("6e40ab03-b5a3-f393-e0a9-e50e24dcca9e"),CCCD=UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
 private static final int NOTIFICATION=1204;
 private static volatile HealthBackgroundService instance;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService storage=Executors.newSingleThreadExecutor();
 private BluetoothGatt gatt;
 private BluetoothGattCharacteristic write,notify;
 private RdfitProtocol decoder;
 private volatile boolean active;
 private boolean writePending,received;
 private int command,attempts,notifications;
 private String state="stopped",reason="",language="hu",identity="",name="";
 private Long steps;private Integer battery;
 private long started,epoch,lastReading;
 private Runnable retry,timeout,poll;
 static boolean permission(Context context){return Build.VERSION.SDK_INT<31||ContextCompat.checkSelfPermission(context,Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
 static boolean configured(Context context)throws Exception{
  BleWatchPreference watch=new BleWatchPreference(context);JSONObject prefs=HealthJournalStore.get(context).preferences();
  return prefs.optBoolean("bleBackground")&&prefs.optBoolean("logWatchSteps")&&!watch.address().isEmpty()&&SERVICE.toString().equals(watch.service())&&permission(context)&&(Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED);
 }
 static void resume(Context context){try{if(configured(context)&&HealthJournalStore.get(context).pendingRestore().isEmpty())ContextCompat.startForegroundService(context,new Intent(context,HealthBackgroundService.class));}catch(RuntimeException ignored){}catch(Exception ignored){} }
 static void stop(Context context){HealthBackgroundService service=instance;if(service!=null)service.main.post(()->{service.active=false;service.close();service.status("stopped","");service.stopSelf();});context.stopService(new Intent(context,HealthBackgroundService.class));}
 static void yieldConnection(){HealthBackgroundService service=instance;if(service!=null){service.close();service.status("waiting","TRIAL_ACTIVE");service.retry(15000);}}
 static JSONObject snapshot(Context context){try{JSONObject value=new JSONObject(context.getSharedPreferences(STATUS,MODE_PRIVATE).getString("state","{}"));boolean running=instance!=null&&instance.active;value.put("running",running);if(!running)value.put("state","stopped");return value;}catch(Exception e){return new JSONObject();}}
 @Override public void onCreate(){super.onCreate();instance=this;}
 @Override public IBinder onBind(Intent intent){return null;}
 @Override public int onStartCommand(Intent intent,int flags,int id){
  if(intent!=null&&STOP.equals(intent.getAction())){try{HealthJournalStore.get(this).setPreferences(new JSONObject().put("bleBackground",false));}catch(Exception ignored){}active=false;close();status("stopped","");stopSelf();return START_NOT_STICKY;}
  try{
   if(!configured(this)){active=false;close();status("stopped","DISABLED_OR_PERMISSION");stopSelf();return START_NOT_STICKY;}
   if(intent!=null)language=intent.getStringExtra("language")==null?language:intent.getStringExtra("language");
   if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"TrainPilot Bluetooth",NotificationManager.IMPORTANCE_LOW));
   startForeground(NOTIFICATION,notification());
   if(!active){active=true;attempts=0;connect();}
   else if(intent!=null&&REFRESH.equals(intent.getAction())&&"connected".equals(state))query();
   return START_STICKY;
  }catch(Exception e){active=false;close();status("stopped","SERVICE_UNAVAILABLE");stopSelf();return START_NOT_STICKY;}
 }
 private boolean usable(){try{return active&&configured(this)&&HealthJournalStore.get(this).pendingRestore().isEmpty();}catch(Exception e){return false;}}
 private void connect(){
  if(!usable()){active=false;close();status("stopped","DISABLED_OR_PERMISSION");stopSelf();return;}
  if(!BleConnectionLease.acquire(this)){status("waiting","TRIAL_ACTIVE");retry(15000);return;}
  try{
   BluetoothManager manager=(BluetoothManager)getSystemService(BLUETOOTH_SERVICE);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
   if(adapter==null||!adapter.isEnabled()){failed("BLUETOOTH_OFF");return;}
   BleWatchPreference watch=new BleWatchPreference(this);identity=watch.journalId();name=watch.name();
   status("connecting","");gatt=adapter.getRemoteDevice(watch.address()).connectGatt(this,false,callback,BluetoothDevice.TRANSPORT_LE);
   if(gatt==null){failed("CONNECTION_FAILED");return;}timeout=()->failed("CONNECTION_TIMEOUT");main.postDelayed(timeout,60000);
  }catch(RuntimeException e){failed("CONNECTION_FAILED");}
 }
 private final BluetoothGattCallback callback=new BluetoothGattCallback(){
  @Override public void onConnectionStateChange(BluetoothGatt connection,int status,int next){main.post(()->{
   if(connection!=gatt)return;if(status!=BluetoothGatt.GATT_SUCCESS||next==BluetoothProfile.STATE_DISCONNECTED){failed("CONNECTION_LOST");return;}
   if(next==BluetoothProfile.STATE_CONNECTED)try{HealthBackgroundService.this.status("discovering","");if(!connection.discoverServices())failed("DISCOVERY_FAILED");}catch(RuntimeException e){failed("DISCOVERY_FAILED");}
  });}
  @Override public void onServicesDiscovered(BluetoothGatt connection,int result){main.post(()->{
   if(connection!=gatt)return;try{
    BluetoothGattService service=connection.getService(SERVICE);write=service==null?null:service.getCharacteristic(WRITE);notify=service==null?null:service.getCharacteristic(NOTIFY);BluetoothGattDescriptor descriptor=notify==null?null:notify.getDescriptor(CCCD);
    if(result!=BluetoothGatt.GATT_SUCCESS||write==null||notify==null||descriptor==null||(write.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE)==0||(notify.getProperties()&BluetoothGattCharacteristic.PROPERTY_NOTIFY)==0){disable("WATCH_CHANGED");return;}
    if(!connection.setCharacteristicNotification(notify,true)){failed("SUBSCRIBE_FAILED");return;}
    boolean queued;if(Build.VERSION.SDK_INT>=33)queued=connection.writeDescriptor(descriptor,BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)==BluetoothStatusCodes.SUCCESS;else{descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);queued=connection.writeDescriptor(descriptor);}
    if(!queued)failed("SUBSCRIBE_FAILED");
   }catch(RuntimeException e){failed("SUBSCRIBE_FAILED");}
  });}
  @Override public void onDescriptorWrite(BluetoothGatt connection,BluetoothGattDescriptor descriptor,int result){main.post(()->{if(connection!=gatt)return;if(result!=BluetoothGatt.GATT_SUCCESS){failed("SUBSCRIBE_FAILED");return;}if(timeout!=null)main.removeCallbacks(timeout);status("connected","");query();});}
  @Override public void onCharacteristicWrite(BluetoothGatt connection,BluetoothGattCharacteristic characteristic,int result){main.post(()->{if(connection!=gatt||characteristic!=write||command==0)return;if(result!=BluetoothGatt.GATT_SUCCESS){failed("QUERY_FAILED");return;}writePending=false;advance();});}
  @Override public void onCharacteristicChanged(BluetoothGatt connection,BluetoothGattCharacteristic characteristic){receive(connection,characteristic,characteristic.getValue());}
  @Override public void onCharacteristicChanged(BluetoothGatt connection,BluetoothGattCharacteristic characteristic,byte[] payload){receive(connection,characteristic,payload);}
 };
 private void query(){
  if(!usable()){disable("DISABLED_OR_PERMISSION");return;}if(gatt==null||write==null||command!=0)return;
  if(poll!=null)main.removeCallbacks(poll);decoder=new RdfitProtocol();notifications=0;battery=null;steps=null;started=System.currentTimeMillis();epoch=HealthJournalStore.get(this).generation();status("reading","");
  timeout=()->failed("QUERY_TIMEOUT");main.postDelayed(timeout,25000);send(RdfitProtocol.BATTERY);
 }
 private void send(int next){
  command=next;received=false;writePending=true;try{byte[] payload=RdfitProtocol.request(next);boolean queued;
   if(Build.VERSION.SDK_INT>=33)queued=gatt.writeCharacteristic(write,payload,BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)==BluetoothStatusCodes.SUCCESS;
   else{write.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);write.setValue(payload);queued=gatt.writeCharacteristic(write);}
   if(!queued)failed("QUERY_FAILED");
  }catch(RuntimeException e){failed("QUERY_FAILED");}
 }
 private void receive(BluetoothGatt connection,BluetoothGattCharacteristic characteristic,byte[] payload){final byte[] bytes=payload==null?null:payload.clone();main.post(()->{
  if(connection!=gatt||characteristic!=notify||command==0)return;if(++notifications>64){failed("QUERY_FAILED");return;}
  for(RdfitProtocol.Reading reading:decoder.accept(bytes)){if(reading.command!=command)continue;if(reading.battery!=null){battery=reading.battery;received=true;}if(reading.steps!=null){steps=reading.steps;received=true;}}
  advance();
 });}
 private void advance(){
  if(writePending||!received)return;if(command==RdfitProtocol.BATTERY){send(RdfitProtocol.STEPS);return;}
  if(command!=RdfitProtocol.STEPS||steps==null)return;attempts=0;command=0;if(timeout!=null)main.removeCallbacks(timeout);
  final long count=steps,requestAt=started,observedAt=System.currentTimeMillis(),generation=epoch;final String source=identity,device=name;
  storage.execute(()->{try{if(!usable()||!source.equals(new BleWatchPreference(this).journalId()))return;HealthJournalStore.get(this).watchSteps(source,device,count,requestAt,observedAt,generation);main.post(()->{if(!active)return;lastReading=observedAt;status("connected","");});}catch(Exception e){main.post(()->{if(active)status("connected","SAVE_RETRY");});}});
  status("connected","");poll=this::query;main.postDelayed(poll,5*60000L);
 }
 private void failed(String why){close();if(!active)return;if(!usable()){disable("DISABLED_OR_PERMISSION");return;}status("waiting",why);retry(BleReconnectPolicy.delay(attempts++));}
 private void retry(long delay){if(retry!=null)main.removeCallbacks(retry);if(!active)return;retry=this::connect;main.postDelayed(retry,delay);}
 private void disable(String why){try{HealthJournalStore.get(this).setPreferences(new JSONObject().put("bleBackground",false));}catch(Exception ignored){}active=false;close();status("stopped",why);stopSelf();}
 private void close(){if(retry!=null)main.removeCallbacks(retry);if(timeout!=null)main.removeCallbacks(timeout);if(poll!=null)main.removeCallbacks(poll);BluetoothGatt old=gatt;gatt=null;write=null;notify=null;decoder=null;command=0;writePending=false;received=false;if(old!=null){try{old.disconnect();}catch(RuntimeException ignored){}try{old.close();}catch(RuntimeException ignored){}}BleConnectionLease.release(this);}
 private void status(String next,String code){state=next;reason=code;try{JSONObject value=new JSONObject().put("state",state).put("code",reason).put("running",active).put("updatedAt",HealthJournalStore.iso(System.currentTimeMillis())).put("lastReadingAt",lastReading==0?JSONObject.NULL:HealthJournalStore.iso(lastReading)).put("battery",battery==null?JSONObject.NULL:battery);getSharedPreferences(STATUS,MODE_PRIVATE).edit().putString("state",value.toString()).apply();if(active)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}catch(Exception ignored){}}
 private Notification notification(){
  PendingIntent open=PendingIntent.getActivity(this,1204,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1205,new Intent(this,HealthBackgroundService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  String title=DistanceTrackingService.text(language,"Óra háttérkapcsolata","Background watch connection","Uhr-Hintergrundverbindung","Conexiunea ceasului în fundal");
  String body=DistanceTrackingService.text(language,"connected".equals(state)?"Kapcsolódva · lépések frissítése 5 percenként":"Kapcsolódás / újracsatlakozás…","connected".equals(state)?"Connected · steps refreshed every 5 minutes":"Connecting / reconnecting…","connected".equals(state)?"Verbunden · Schritte alle 5 Minuten":"Verbindung / Wiederverbindung…","connected".equals(state)?"Conectat · pași actualizați la 5 minute":"Conectare / reconectare…");
  return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(body).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,DistanceTrackingService.text(language,"Leállítás","Stop","Stoppen","Oprește"),stop).build();
 }
 @Override public void onDestroy(){active=false;close();status("stopped",reason);storage.shutdownNow();stopForeground(true);if(instance==this)instance=null;super.onDestroy();}
}
