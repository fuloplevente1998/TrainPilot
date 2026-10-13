package com.repforge.app;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.*;
import androidx.core.app.NotificationCompat;
import org.json.*;
import java.util.*;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/** Explicitly started, finite fitness timer with an ongoing Stop notification. */
public class IntervalTimerService extends Service {
    static final String START="interval.START",STOP="interval.STOP",PAUSE="interval.PAUSE",RESUME="interval.RESUME",PREFS="trainpilot-interval",CHANNEL="trainpilot_interval";
    private static final int NOTIFICATION=162;
    private static IntervalTimerService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<IntervalTimeline.Step> steps=new ArrayList<>();
    private final JSONArray segments=new JSONArray();
    private IntervalTimeline timeline;
    private String key="",language="en",lastToken="";
    private String workoutId="";
    private JSONArray planSteps=new JSONArray();
    private long phaseNumber=0;
    private boolean running,advance,sound,vibration,stopped,restOnly;
    private int initialRest;
    private long elapsed,began,lastSavedSecond=-1;
    private int gpsStep;
    private PowerManager.WakeLock wake;
    private ToneGenerator tones;
    private IntervalHapticCues haptics=new IntervalHapticCues();
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;update();if(running)handler.postDelayed(this,200);}};
    @Override public void onCreate(){super.onCreate();instance=this;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(STOP.equals(intent.getAction())){if(key.equals(intent.getStringExtra("key")))stopRun();return START_NOT_STICKY;}
        if(PAUSE.equals(intent.getAction())||RESUME.equals(intent.getAction())){try{command(this,intent.getStringExtra("key"),PAUSE.equals(intent.getAction())?"pause":"resume");}catch(Exception ignored){}return START_NOT_STICKY;}
        if(!START.equals(intent.getAction())||running)return START_NOT_STICKY;
        try{
            JSONObject plan=new JSONObject(intent.getStringExtra("plan"));parse(plan);stopped=false;elapsed=plan.optLong("elapsedMs",0);began=SystemClock.elapsedRealtime();running=true;
            if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"TrainPilot intervals",NotificationManager.IMPORTANCE_LOW));
            startForeground(NOTIFICATION,notification());acquire();handler.post(tick);save();
        }catch(Exception e){running=false;release();stopForeground(true);stopSelf();}
        return START_NOT_STICKY;
    }
    static IntervalTimeline validate(JSONObject plan)throws Exception{
        String key=plan.optString("key");if(!key.matches("[A-Za-z0-9:._|+\\-]{1,200}"))throw new IllegalArgumentException("Invalid interval key");
        if(plan.has("initialRest")&&plan.getDouble("initialRest")!=plan.getInt("initialRest"))throw new IllegalArgumentException("Invalid initial rest");
        JSONArray rows=plan.getJSONArray("steps");ArrayList<IntervalTimeline.Step> parsed=new ArrayList<>();Set<String> owners=new HashSet<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String set=row.getString("setId");if(!owners.add(set))throw new IllegalArgumentException("Duplicate interval owner");String gps=row.optString("gpsKey","");if(!gps.isEmpty()&&!gps.matches("[A-Za-z0-9:._|+\\-]{1,200}"))throw new IllegalArgumentException("Invalid GPS key");parsed.add(new IntervalTimeline.Step(row.getString("exerciseId"),set,row.optString("name","TrainPilot"),row.getInt("work"),row.getInt("rest"),row.getInt("rounds"),gps));}
        if(!plan.optBoolean("autoAdvance")&&parsed.size()!=1)throw new IllegalArgumentException("Manual interval has one owner");
        return new IntervalTimeline(parsed,plan.optBoolean("autoAdvance"),plan.optInt("initialRest",0),plan.optBoolean("restOnly"));
    }
    private void parse(JSONObject plan)throws Exception{
        JSONObject current=ActiveWorkoutStore.current(this);
        workoutId=plan.optString("workoutId",current==null?"":current.optString("workoutId"));
        planSteps=new JSONArray(plan.getJSONArray("steps").toString());phaseNumber=0;
        timeline=validate(plan);steps.clear();JSONArray rows=plan.getJSONArray("steps");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);steps.add(new IntervalTimeline.Step(row.getString("exerciseId"),row.getString("setId"),row.optString("name","TrainPilot"),row.getInt("work"),row.getInt("rest"),row.getInt("rounds"),row.optString("gpsKey","")));}
        initialRest=plan.optInt("initialRest",0);restOnly=plan.optBoolean("restOnly");timeline=new IntervalTimeline(steps,plan.optBoolean("autoAdvance"),initialRest,restOnly);key=plan.getString("key");language=plan.optString("language","en");advance=plan.optBoolean("autoAdvance");sound=plan.optBoolean("sound",true);vibration=plan.optBoolean("vibration",true);while(segments.length()>0)segments.remove(0);gpsStep=0;lastToken="";haptics=new IntervalHapticCues();
    }
    private long now(){return elapsed+(running?Math.max(0,SystemClock.elapsedRealtime()-began):0);}
    private void update(){
        if(timeline==null)return;IntervalTimeline.Phase p=timeline.at(now());
        if(p.step!=gpsStep||running&&"complete".equals(p.phase))handoff(p);
        DistanceTrackingService.pauseOwned(this,steps.get(p.step).gpsKey,!running||!"work".equals(p.phase));
        int vibrationMs=haptics.next(p,running&&!stopped);
        if(vibrationMs>0)vibrate(vibrationMs);
        if(!p.token.equals(lastToken)){lastToken=p.token;phaseNumber++;if(!stopped)cue("complete".equals(p.phase));notifyStatus();publishWatchClock();}
        if("complete".equals(p.phase)){elapsed=p.elapsedMs;running=false;began=0;release();handler.removeCallbacks(tick);save();stopForeground(true);stopSelf();}else if(p.elapsedMs/1000!=lastSavedSecond){lastSavedSecond=p.elapsedMs/1000;save();notifyStatus();}
    }
    private void handoff(IntervalTimeline.Phase p){
        IntervalTimeline.Step old=steps.get(gpsStep);String tracking=old.gpsKey;
        if(!tracking.isEmpty()){
            JSONObject current=DistanceTrackingService.snapshot(this);
            if(tracking.equals(current.optString("key"))){
                JSONObject capture;
                if(!"complete".equals(p.phase)&&p.step!=gpsStep&&!steps.get(p.step).gpsKey.isEmpty())capture=DistanceTrackingService.rollover(this,tracking,steps.get(p.step).gpsKey,steps.get(p.step).name);
                else capture=DistanceTrackingService.stopOwned(this,tracking);
                if(capture!=null)try{capture.put("exerciseId",old.exerciseId);capture.put("setId",old.setId);segments.put(capture);}catch(Exception ignored){}
            }
        }
        gpsStep=p.step;
    }
    private void acquire(){if(wake==null)wake=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TrainPilot:interval");if(!wake.isHeld())wake.acquire(IntervalTimeline.MAX_MS+60000);}
    private void release(){if(wake!=null&&wake.isHeld())wake.release();}
    private void cue(boolean complete){
        if(sound)try{if(tones==null)tones=new ToneGenerator(AudioManager.STREAM_MUSIC,80);tones.startTone(complete?ToneGenerator.TONE_PROP_ACK:ToneGenerator.TONE_PROP_BEEP,complete?450:180);}catch(Exception ignored){}
    }
    private Vibrator vibrator(){return Build.VERSION.SDK_INT>=31?((VibratorManager)getSystemService(VIBRATOR_MANAGER_SERVICE)).getDefaultVibrator():(Vibrator)getSystemService(VIBRATOR_SERVICE);}
    private void vibrate(int milliseconds){if(vibration)try{Vibrator v=vibrator();if(v!=null&&v.hasVibrator()){if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(milliseconds,VibrationEffect.DEFAULT_AMPLITUDE));else v.vibrate(milliseconds);}}catch(Exception ignored){}}
    private void cancelVibration(){try{Vibrator v=vibrator();if(v!=null)v.cancel();}catch(Exception ignored){}}
    private void publishWatchClock(){
        if(workoutId.isEmpty()||!advance&&!restOnly)return;
        try {
            IntervalTimeline.Phase p=timeline.at(now());
            JSONObject value=new JSONObject().put("workoutId",workoutId).put("key",key).put("owner","phone")
                .put("enabled",!stopped).put("phase",stopped?"ready":p.phase).put("paused",!running)
                .put("remainingMs",p.remainingMs).put("deadlineAt",running?System.currentTimeMillis()+p.remainingMs:0)
                .put("workSeconds",steps.get(p.step).work).put("restSeconds",steps.get(p.step).rest).put("rounds",steps.get(p.step).rounds).put("round",p.round).put("vibration",false)
                .put("autoAdvance",advance).put("restOnly",restOnly).put("stopped",stopped).put("completed",p.completed)
                .put("stepIndex",p.step).put("stepElapsedMs",p.stepElapsedMs).put("steps",planSteps).put("phaseNumber",phaseNumber);
            PutDataMapRequest map=PutDataMapRequest.create("/trainpilot/phone-automatic");
            map.getDataMap().putString("automatic",value.toString());map.getDataMap().putLong("publishedAt",System.currentTimeMillis());
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent());
        }catch(Exception ignored){}
    }
    private JSONObject current(){
        JSONObject out=new JSONObject();if(timeline==null)return out;IntervalTimeline.Phase p=timeline.at(now());try{
            out.put("key",key).put("phaseNumber",phaseNumber).put("running",running).put("stopped",stopped).put("restOnly",restOnly).put("initialRest",initialRest).put("autoAdvance",advance).put("sound",sound).put("vibration",vibration).put("language",language).put("elapsedMs",p.elapsedMs).put("stepElapsedMs",p.stepElapsedMs).put("stepIndex",p.step).put("completed",p.completed).put("phase",p.phase).put("round",p.round).put("remainingMs",p.remainingMs).put("totalMs",p.totalMs);
            JSONArray rows=new JSONArray();for(IntervalTimeline.Step s:steps)rows.put(new JSONObject().put("exerciseId",s.exerciseId).put("setId",s.setId).put("name",s.name).put("work",s.work).put("rest",s.rest).put("rounds",s.rounds).put("gpsKey",s.gpsKey));out.put("steps",rows).put("gpsSegments",segments);JSONObject gps=DistanceTrackingService.snapshot(this);if(steps.get(p.step).gpsKey.equals(gps.optString("key"))&&!steps.get(p.step).gpsKey.isEmpty())out.put("gps",gps);
        }catch(Exception ignored){}return out;
    }
    private void save(){if(!key.isEmpty())getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("state",current().toString()).apply();}
    private Notification notification(){
        String title=DistanceTrackingService.text(language,"Intervallumidőzítő","Interval timer","Intervalltimer","Cronometru de intervale"),pause=DistanceTrackingService.text(language,"Szünetel","Paused","Pausiert","În pauză");
        IntervalTimeline.Phase p=timeline.at(now());String label=DistanceTrackingService.text(language,"Munka","Work","Belastung","Efort");if("rest".equals(p.phase))label=DistanceTrackingService.text(language,"Pihenő","Rest","Pause","Odihnă");
        String body=(running?label+" · "+p.round+"/"+steps.get(p.step).rounds+" · "+(p.remainingMs+999)/1000+" s":pause)+" · "+steps.get(p.step).name;
        PendingIntent open=PendingIntent.getActivity(this,162,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,162,new Intent(this,IntervalTimerService.class).setAction(STOP).putExtra("key",key),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toggle=PendingIntent.getService(this,163,new Intent(this,IntervalTimerService.class).setAction(running?PAUSE:RESUME).putExtra("key",key),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String toggleLabel=running?pause:DistanceTrackingService.text(language,"Folytatás","Resume","Fortsetzen","Continuă");
        return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(body).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,toggleLabel,toggle).addAction(0,DistanceTrackingService.text(language,"Leállítás","Stop","Stoppen","Oprește"),stop).build();
    }
    private void notifyStatus(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}
    private void pauseRun(){if(!running){cancelVibration();return;}update();cancelVibration();if(!running)return;elapsed=now();running=false;began=0;handler.removeCallbacks(tick);release();DistanceTrackingService.pauseOwned(this,steps.get(gpsStep).gpsKey,true);save();notifyStatus();publishWatchClock();}
    private void stopRun(){pauseRun();stopped=true;DistanceTrackingService.stopOwned(this,steps.get(gpsStep).gpsKey);save();publishWatchClock();stopForeground(true);stopSelf();}
    static JSONObject snapshot(Context context){
        IntervalTimerService service=instance;if(service!=null&&service.timeline!=null){service.update();return service.current();}
        try{JSONObject out=new JSONObject(context.getSharedPreferences(PREFS,MODE_PRIVATE).getString("state","{}"));if(out.optBoolean("running")){out.put("running",false).put("interrupted",true);context.getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("state",out.toString()).apply();}return out;}catch(Exception ignored){return new JSONObject();}
    }
    static JSONObject command(Context context,String key,String action)throws Exception{
        IntervalTimerService s=instance;if(s==null||!key.equals(s.key))throw new IllegalStateException("Interval owner is no longer active");
        if("pause".equals(action))s.pauseRun();else if("stop".equals(action))s.stopRun();else if("resume".equals(action)&&!s.running&&!s.stopped&&!"complete".equals(s.timeline.at(s.elapsed).phase)){s.began=SystemClock.elapsedRealtime();s.running=true;s.acquire();DistanceTrackingService.pauseOwned(context,s.steps.get(s.gpsStep).gpsKey,!"work".equals(s.timeline.at(s.elapsed).phase));s.handler.post(s.tick);s.save();}
        else if("skipRest".equals(action)&&!s.stopped){IntervalTimeline.Phase p=s.timeline.at(s.now());if(!"rest".equals(p.phase))throw new IllegalStateException("Not resting");s.handler.removeCallbacks(s.tick);s.elapsed=p.elapsedMs+p.remainingMs;s.began=SystemClock.elapsedRealtime();s.running=true;s.acquire();s.update();if(s.running)s.handler.post(s.tick);}
        s.publishWatchClock();return s.current();
    }
    static JSONObject attachGPS(Context context,String key,int index,String tracking)throws Exception{
        IntervalTimerService s=instance;if(s==null||!key.equals(s.key)||s.timeline.at(s.now()).step!=index||s.stopped||"complete".equals(s.timeline.at(s.now()).phase))throw new IllegalStateException("Interval owner changed");JSONObject gps=DistanceTrackingService.snapshot(context);if(!tracking.equals(gps.optString("key"))||!gps.optBoolean("active"))throw new IllegalStateException("GPS owner changed");s.steps.get(index).gpsKey=tracking;DistanceTrackingService.pauseOwned(context,tracking,!s.running||!"work".equals(s.timeline.at(s.now()).phase));s.save();return s.current();
    }
    @Override public void onTaskRemoved(Intent root){if(timeline!=null)stopRun();}
    @Override public void onDestroy(){if(running){cancelVibration();elapsed=now();running=false;stopped=true;DistanceTrackingService.stopOwned(this,steps.get(gpsStep).gpsKey);save();}handler.removeCallbacks(tick);release();if(tones!=null)tones.release();if(instance==this)instance=null;super.onDestroy();}
}
