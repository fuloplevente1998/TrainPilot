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
    static final String ARM="interval.ARM",START="interval.START",STOP="interval.STOP",PAUSE="interval.PAUSE",RESUME="interval.RESUME",PREFS="trainpilot-interval",CHANNEL="trainpilot_interval";
    private static final int NOTIFICATION=162;
    private static IntervalTimerService instance;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ArrayList<IntervalTimeline.Step> steps=new ArrayList<>();
    private final JSONArray segments=new JSONArray();
    private IntervalTimeline timeline;
    private IntervalTimeLedger timeLedger;
    private long executionGeneration;
    private String key="",language="en",lastToken="";
    private String workoutId="", armedWorkoutId="";
    private long armedGeneration;
    private JSONArray planSteps=new JSONArray();
    private long phaseNumber=0;
    private boolean running,advance,sound,vibration,stopped,restOnly,interrupted;
    private int initialRest;
    private long elapsed,began,lastSavedSecond=-1;
    private int gpsStep;
    private PowerManager.WakeLock wake;
    private ToneGenerator tones;
    private IntervalHapticCues haptics=new IntervalHapticCues();
    private final Runnable expire=()->{armedWorkoutId="";if(timeline!=null)stopRun();else{stopForeground(true);stopSelf();}};
    private final Runnable tick=new Runnable(){public void run(){if(!running)return;update();if(running)handler.postDelayed(this,200);}};
    @Override public void onCreate(){super.onCreate();instance=this;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent==null){stopSelf();return START_NOT_STICKY;}
        if(STOP.equals(intent.getAction())){if(key.equals(intent.getStringExtra("key"))){armedWorkoutId="";stopRun();}return START_NOT_STICKY;}
        if(ARM.equals(intent.getAction())){
            String requested=intent.getStringExtra("workoutId");
            if(requested==null||requested.isEmpty()||intent.getLongExtra("generation",0)<=0)return START_NOT_STICKY;
            if(timeline!=null&&!stopped&&!"complete".equals(timeline.at(now()).phase)&&!requested.equals(workoutId))return START_NOT_STICKY;
            if(!requested.equals(armedWorkoutId)){handler.removeCallbacks(expire);handler.postDelayed(expire,IntervalTimeline.MAX_MS);}
            armedWorkoutId=requested;armedGeneration=intent.getLongExtra("generation",0);language=intent.getStringExtra("language");
            channel();startForeground(NOTIFICATION,notification());
            try{PhoneWorkoutExecution.store(this).beginManualWork(requested,armedGeneration,SystemClock.elapsedRealtime());}catch(Exception ignored){}
            return START_NOT_STICKY;
        }
        if(PAUSE.equals(intent.getAction())||RESUME.equals(intent.getAction())){try{command(this,intent.getStringExtra("key"),PAUSE.equals(intent.getAction())?"pause":"resume");}catch(Exception ignored){}return START_NOT_STICKY;}
        if(!START.equals(intent.getAction())||running)return START_NOT_STICKY;
        try{
            JSONObject plan=new JSONObject(intent.getStringExtra("plan"));parse(plan);stopped=false;elapsed=plan.optLong("elapsedMs",0);began=SystemClock.elapsedRealtime();running=true;
            if(plan.optBoolean("restoreBilateral192")){phaseNumber=plan.optLong("phaseNumber")+1;lastToken=timeline.at(elapsed).token;haptics.next(timeline.at(elapsed),true);PhoneWorkoutExecution.store(this).markTransition(key,"started");}
            channel();
            startForeground(NOTIFICATION,notification());acquire();handler.post(tick);save();
        }catch(Exception e){running=false;release();stopForeground(true);stopSelf();}
        return START_NOT_STICKY;
    }
    static IntervalTimeline validate(JSONObject plan)throws Exception{
        String key=plan.optString("key");if(!key.matches("[A-Za-z0-9:._|+\\-]{1,200}"))throw new IllegalArgumentException("Invalid interval key");
        if(plan.has("initialRest")&&plan.getDouble("initialRest")!=plan.getInt("initialRest"))throw new IllegalArgumentException("Invalid initial rest");
        JSONArray rows=plan.getJSONArray("steps");ArrayList<IntervalTimeline.Step> parsed=new ArrayList<>();Set<String> owners=new HashSet<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);String set=row.getString("setId");if(!owners.add(set))throw new IllegalArgumentException("Duplicate interval owner");String gps=row.optString("gpsKey","");if(!gps.isEmpty()&&!gps.matches("[A-Za-z0-9:._|+\\-]{1,200}"))throw new IllegalArgumentException("Invalid GPS key");parsed.add(step(row));}
        if(!plan.optBoolean("autoAdvance")&&parsed.size()!=1)throw new IllegalArgumentException("Manual interval has one owner");
        return new IntervalTimeline(parsed,plan.optBoolean("autoAdvance"),plan.optInt("initialRest",0),plan.optBoolean("restOnly"),overrides192(plan));
    }
    private static IntervalTimeline.Step step(JSONObject row)throws Exception{return new IntervalTimeline.Step(row.getString("exerciseId"),row.getString("setId"),row.optString("name","TrainPilot"),row.getInt("work"),row.getInt("rest"),row.getInt("rounds"),row.optString("gpsKey",""),Bilateral192.parse(row.optJSONObject("bilateral192")));}
    private static Map<String,Long> overrides192(JSONObject plan)throws Exception {
        Map<String,Long> values=new LinkedHashMap<>();JSONObject source=plan.optJSONObject("durationOverrides192");if(source==null)return values;
        if(source.length()>40200)throw new IllegalArgumentException("Invalid phase edits");
        for(Iterator<String> i=source.keys();i.hasNext();){String key=i.next();if(key.length()>128)throw new IllegalArgumentException("Invalid phase identity");long value=PhoneWorkoutRecording.version(source,key);if(value>3600000)throw new IllegalArgumentException("Invalid phase duration");values.put(key,value);}return values;
    }
    private void targets192()throws Exception{for(int i=0;i<planSteps.length();i++)if(planSteps.getJSONObject(i).has("bilateral192"))planSteps.getJSONObject(i)
        .put("targetLeftMs192",timeline.targetWork192(i,"left")).put("targetRightMs192",timeline.targetWork192(i,"right"));}
    static JSONObject configureBilateral192(Context context,String key,String exerciseId,JSONObject value)throws Exception {
        IntervalTimerService s=instance;if(s==null||!key.equals(s.key)||s.running||s.stopped)throw new IllegalStateException("Pause the current timer first");
        IntervalTimeline.Phase p=s.timeline.at(s.elapsed);if(p.phase.equals("complete"))throw new IllegalStateException("Completed timer");
        Bilateral192.Config c=Bilateral192.parse(value);if(c==null)throw new IllegalArgumentException("Missing bilateral configuration");
        if(!s.steps.get(p.step).exerciseId.equals(exerciseId)||s.steps.get(p.step).bilateral==null)throw new IllegalArgumentException("Bilateral target changed");
        if(!s.steps.get(p.step).bilateral.first.equals(c.first))throw new IllegalArgumentException("Change the first side before starting the set");
        Map<String,Long> frozen=s.timeline.freezeThrough192(p);JSONArray rows=new JSONArray(s.planSteps.toString());ArrayList<IntervalTimeline.Step> revised=new ArrayList<>();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(i>=p.step&&exerciseId.equals(row.optString("exerciseId"))&&row.has("bilateral192")){
            row.put("bilateral192",c.json());if(c.setRest>=0)row.put("rest",c.setRest);}
            revised.add(step(row));}
        IntervalTimeline candidate=new IntervalTimeline(revised,s.advance,s.initialRest,s.restOnly,frozen);
        if(rows.toString().length()+new JSONObject(frozen).toString().length()>65000)throw new IllegalArgumentException("Bilateral plan is too large");
        s.timeLedger.sample(s.timeline,p.elapsedMs,SystemClock.elapsedRealtime(),false,false,s.interrupted);
        s.timeline=candidate;s.steps.clear();s.steps.addAll(revised);s.planSteps=rows;s.phaseNumber++;s.targets192();s.save();s.notifyStatus();s.publishWatchClock();return s.current();
    }
    private void channel(){if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel(CHANNEL,"TrainPilot intervals",NotificationManager.IMPORTANCE_LOW));}
    static boolean armed(String workoutId,long generation){return instance!=null&&workoutId.equals(instance.armedWorkoutId)&&generation==instance.armedGeneration;}
    /** Runs only on the main looper, in a service already started by a visible phone workout. */
    static boolean startPrepared(JSONObject plan)throws Exception{
        IntervalTimerService s=instance;
        if(s==null||!armed(plan.getString("workoutId"),plan.getLong("generation")))return false;
        if(plan.optString("key").equals(s.key))return true; // Retried outbox delivery must not reset elapsed time.
        if(s.timeline!=null&&!s.stopped&&!"complete".equals(s.timeline.at(s.now()).phase))return false;
        s.handler.removeCallbacks(s.tick);s.parse(plan);s.stopped=false;s.elapsed=0;s.began=SystemClock.elapsedRealtime();s.running=true;
        s.save();s.acquire();s.handler.post(s.tick);s.notifyStatus();s.publishWatchClock();return true;
    }
    static void closeWorkout(String workoutId,long generation){
        IntervalTimerService s=instance;if(s==null||!armed(workoutId,generation))return;
        s.armedWorkoutId="";if(s.timeline==null){s.stopForeground(true);s.stopSelf();}else if(workoutId.equals(s.workoutId))s.stopRun();
    }
    static void disarm(){IntervalTimerService s=instance;if(s==null)return;s.armedWorkoutId="";if(s.timeline==null){s.stopForeground(true);s.stopSelf();}else if(!s.running&&"complete".equals(s.timeline.at(s.now()).phase)){s.stopForeground(true);s.stopSelf();}}
    private void parse(JSONObject plan)throws Exception{
        JSONObject current=ActiveWorkoutStore.current(this);
        workoutId=plan.optString("workoutId",current==null?"":current.optString("workoutId"));
        JSONObject execution=PhoneWorkoutExecution.store(this).snapshot();
        executionGeneration=plan.optLong("generation",execution!=null&&execution.optString("workoutId").equals(workoutId)?execution.optLong("generation"):0);
        planSteps=new JSONArray(plan.getJSONArray("steps").toString());phaseNumber=0;
        timeline=validate(plan);timeLedger=new IntervalTimeLedger(plan.getJSONArray("steps").length(),SystemClock.elapsedRealtime());interrupted=false;lastSavedSecond=-1;steps.clear();JSONArray rows=plan.getJSONArray("steps");for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);steps.add(step(row));}
        if(plan.optBoolean("restoreBilateral192")){
            if(plan.getLong("elapsedMs")!=plan.getJSONObject("ledger").getLong("positionMs"))throw new IllegalArgumentException("Invalid restored position");
            timeLedger.restore192(plan.getJSONObject("ledger"),planSteps,SystemClock.elapsedRealtime());
        }
        initialRest=plan.optInt("initialRest",0);restOnly=plan.optBoolean("restOnly");timeline=new IntervalTimeline(steps,plan.optBoolean("autoAdvance"),initialRest,restOnly,overrides192(plan));targets192();key=plan.getString("key");language=plan.optString("language","en");advance=plan.optBoolean("autoAdvance");sound=plan.optBoolean("sound",true);vibration=plan.optBoolean("vibration",true);while(segments.length()>0)segments.remove(0);gpsStep=0;lastToken="";haptics=new IntervalHapticCues();
    }
    private long now(){return elapsed+(running?Math.max(0,SystemClock.elapsedRealtime()-began):0);}
    private void update(){
        if(timeline==null)return;IntervalTimeline.Phase p=timeline.at(now());
        timeLedger.sample(timeline,p.elapsedMs,SystemClock.elapsedRealtime(),running,stopped,interrupted);
        if(p.step!=gpsStep||running&&"complete".equals(p.phase))handoff(p);
        DistanceTrackingService.pauseOwned(this,steps.get(p.step).gpsKey,!running||!"work".equals(p.phase));
        int vibrationMs=haptics.next(p,running&&!stopped);
        if(vibrationMs>0)vibrate(vibrationMs);
        if(!p.token.equals(lastToken)){lastToken=p.token;phaseNumber++;if(!stopped)cue("complete".equals(p.phase));notifyStatus();publishWatchClock();}
        if("complete".equals(p.phase)){elapsed=p.elapsedMs;running=false;began=0;release();handler.removeCallbacks(tick);save();if(armedWorkoutId.isEmpty()){stopForeground(true);stopSelf();}else notifyStatus();}else if((running?p.elapsedMs:SystemClock.elapsedRealtime())/1000!=lastSavedSecond){lastSavedSecond=(running?p.elapsedMs:SystemClock.elapsedRealtime())/1000;save();notifyStatus();}
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
                .put("enabled",!stopped).put("phase",stopped?"ready":p.phase).put("paused",!running && !"complete".equals(p.phase))
                .put("remainingMs",p.remainingMs).put("deadlineAt",running?System.currentTimeMillis()+p.remainingMs:0)
                .put("bilateralProtocol192",1).put("side192",p.side192).put("restKind192",p.restKind192).put("phaseDurationMs",timeline.phaseDurationMs(p)).put("workSeconds",steps.get(p.step).bilateral==null?steps.get(p.step).work:steps.get(p.step).bilateral.work(p.side192)).put("restSeconds","rest".equals(p.phase)?timeline.phaseDurationMs(p)/1000:steps.get(p.step).rest).put("rounds",steps.get(p.step).rounds).put("round",p.round).put("vibration",false)
                .put("autoAdvance",advance).put("restOnly",restOnly).put("stopped",stopped).put("completed",p.completed)
                .put("durationOverrides192",new JSONObject(timeline.overrides192())).put("timeLog",timeLedger.json(planSteps).put("key",key)).put("ledger",timeLedger.json(planSteps)).put("stepIndex",p.step).put("stepElapsedMs",p.stepElapsedMs).put("steps",planSteps).put("phaseNumber",phaseNumber);
            PutDataMapRequest map=PutDataMapRequest.create("/trainpilot/phone-automatic");
            map.getDataMap().putString("automatic",value.toString());map.getDataMap().putLong("publishedAt",System.currentTimeMillis());
            Wearable.getDataClient(this).putDataItem(map.asPutDataRequest().setUrgent());
        }catch(Exception ignored){}
    }
    private JSONObject current(){
        JSONObject out=new JSONObject();if(timeline==null)return out;IntervalTimeline.Phase p=timeline.at(now());try{
            out.put("key",key).put("workoutId",workoutId).put("generation",executionGeneration).put("ledger",timeLedger.json(planSteps)).put("interrupted",interrupted).put("phaseNumber",phaseNumber).put("running",running).put("stopped",stopped).put("restOnly",restOnly).put("initialRest",initialRest).put("autoAdvance",advance).put("sound",sound).put("vibration",vibration).put("language",language).put("elapsedMs",p.elapsedMs).put("stepElapsedMs",p.stepElapsedMs).put("stepIndex",p.step).put("completed",p.completed).put("phase",p.phase).put("round",p.round).put("remainingMs",p.remainingMs).put("totalMs",p.totalMs).put("durationOverrides192",new JSONObject(timeline.overrides192())).put("bilateralProtocol192",1).put("side192",p.side192).put("restKind192",p.restKind192).put("phaseDurationMs",timeline.phaseDurationMs(p));
            JSONArray rows=new JSONArray();for(int i=0;i<steps.size();i++){IntervalTimeline.Step s=steps.get(i);rows.put(new JSONObject(planSteps.getJSONObject(i).toString()).put("exerciseId",s.exerciseId).put("setId",s.setId).put("name",s.name).put("work",s.work).put("rest",s.rest).put("rounds",s.rounds).put("gpsKey",s.gpsKey));}out.put("steps",rows).put("gpsSegments",segments);JSONObject gps=DistanceTrackingService.snapshot(this);if(steps.get(p.step).gpsKey.equals(gps.optString("key"))&&!steps.get(p.step).gpsKey.isEmpty())out.put("gps",gps);
        }catch(Exception ignored){}return out;
    }
    private void save(){
        if(key.isEmpty())return;
        timeLedger.sample(timeline,timeline.at(now()).elapsedMs,SystemClock.elapsedRealtime(),running,stopped,interrupted);
        JSONObject checkpoint=current();
        try{if(executionGeneration>0)if(PhoneWorkoutExecution.store(this).checkpointTimer(checkpoint,SystemClock.elapsedRealtime()))PhoneWorkoutExecution.publish(this);}
        catch(Exception error){throw new IllegalStateException("Unable to checkpoint workout",error);}
        if(!getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("state",checkpoint.toString()).commit())throw new IllegalStateException("Unable to checkpoint interval");
    }
    private Notification notification(){
        String title=DistanceTrackingService.text(language,"Intervallumidőzítő","Interval timer","Intervalltimer","Cronometru de intervale"),pause=DistanceTrackingService.text(language,"Szünetel","Paused","Pausiert","În pauză");
        if(timeline==null||!armedWorkoutId.isEmpty()&&!running&&(stopped||"complete".equals(timeline.at(now()).phase))){
            PendingIntent open=PendingIntent.getActivity(this,162,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent stop=PendingIntent.getService(this,162,new Intent(this,IntervalTimerService.class).setAction(STOP).putExtra("key",key),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title)
                .setContentText(DistanceTrackingService.text(language,"Automata edzés · Sorozat rögzítésére vár","Automatic workout · Waiting for set recording","Automatiktraining · Satz erfassen","Antrenament automat · Înregistrează seria"))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,DistanceTrackingService.text(language,"Leállítás","Stop","Stoppen","Oprește"),stop).build();
        }
        IntervalTimeline.Phase p=timeline.at(now());String label=DistanceTrackingService.text(language,"Munka","Work","Belastung","Efort");if("rest".equals(p.phase))label=DistanceTrackingService.text(language,"Pihenő","Rest","Pause","Odihnă");
        if(!p.side192.isEmpty())label=Bilateral192.label(language,p);
        String body=(running?label+" · "+p.round+"/"+steps.get(p.step).rounds+" · "+(p.remainingMs+999)/1000+" s":pause)+" · "+steps.get(p.step).name;
        PendingIntent open=PendingIntent.getActivity(this,162,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,162,new Intent(this,IntervalTimerService.class).setAction(STOP).putExtra("key",key),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toggle=PendingIntent.getService(this,163,new Intent(this,IntervalTimerService.class).setAction(running?PAUSE:RESUME).putExtra("key",key),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String toggleLabel=running?pause:DistanceTrackingService.text(language,"Folytatás","Resume","Fortsetzen","Continuă");
        return new NotificationCompat.Builder(this,CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(title).setContentText(body).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,toggleLabel,toggle).addAction(0,DistanceTrackingService.text(language,"Leállítás","Stop","Stoppen","Oprește"),stop).build();
    }
    private void notifyStatus(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION,notification());}
    private void pauseRun(){if(!running){cancelVibration();return;}update();cancelVibration();if(!running)return;elapsed=now();running=false;began=0;handler.removeCallbacks(tick);release();DistanceTrackingService.pauseOwned(this,steps.get(gpsStep).gpsKey,true);save();notifyStatus();publishWatchClock();}
    private void stopRun(){if(timeline==null){stopForeground(true);stopSelf();return;}pauseRun();stopped=true;DistanceTrackingService.stopOwned(this,steps.get(gpsStep).gpsKey);save();publishWatchClock();stopForeground(true);stopSelf();}
    /** Retire the old clock after accounting real elapsed time, while retaining the foreground controller. */
    static void pauseForUndo(Context context,String key)throws Exception {
        IntervalTimerService s=instance;
        if(s==null||!key.equals(s.key))return;
        s.pauseRun();s.stopped=true;s.handler.removeCallbacks(s.tick);s.release();s.cancelVibration();s.save();s.notifyStatus();
    }
    static JSONObject snapshot(Context context){
        IntervalTimerService service=instance;if(service!=null&&service.timeline!=null){service.update();return service.current();}
        try{JSONObject out=new JSONObject(context.getSharedPreferences(PREFS,MODE_PRIVATE).getString("state","{}"));boolean side=false;JSONArray rows=out.optJSONArray("steps");if(rows!=null)for(int i=0;i<rows.length();i++)side|=rows.getJSONObject(i).has("bilateral192");if(out.optBoolean("running")||side&&!out.optBoolean("stopped")&&!"complete".equals(out.optString("phase"))){out.put("running",false).put("interrupted",true);context.getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString("state",out.toString()).apply();}return out;}catch(Exception ignored){return new JSONObject();}
    }
    static JSONObject restorableBilateral192(Context context,String key)throws Exception {
        if(instance!=null&&instance.timeline!=null)return null;
        JSONObject saved=snapshot(context),frame=PhoneWorkoutExecution.store(context).snapshot();
        if(frame==null||!frame.optBoolean("active")||frame.optBoolean("workoutPaused")||(PhoneWorkoutExecution.store(context).restoreBlocked()||PhoneWorkoutExecution.store(context).commandsSuspended())
            ||!key.equals(saved.optString("key"))||saved.optBoolean("stopped")||!saved.optBoolean("interrupted")||"complete".equals(saved.optString("phase"))
            ||!frame.optString("workoutId").equals(saved.optString("workoutId"))||frame.optLong("generation")!=saved.optLong("generation"))return null;
        JSONArray rows=saved.optJSONArray("steps");boolean side=false;if(rows!=null)for(int i=0;i<rows.length();i++)side|=rows.getJSONObject(i).has("bilateral192");
        return side?saved.put("restoreBilateral192",true):null;
    }
    static JSONObject command(Context context,String key,String action)throws Exception{
        IntervalTimerService s=instance;if(s==null||!key.equals(s.key))throw new IllegalStateException("Interval owner is no longer active");
        if(("resume".equals(action)||"skipRest".equals(action))&&s.executionGeneration>0){
            JSONObject frame=PhoneWorkoutExecution.store(context).snapshot();
            if(frame!=null&&s.workoutId.equals(frame.optString("workoutId"))&&frame.optBoolean("workoutPaused"))throw new IllegalStateException("Workout is suspended");
        }
        if("pause".equals(action))s.pauseRun();else if("stop".equals(action))s.stopRun();else if("resume".equals(action)&&!s.running&&!s.stopped&&!"complete".equals(s.timeline.at(s.elapsed).phase)){s.timeLedger.sample(s.timeline,s.elapsed,SystemClock.elapsedRealtime(),false,false,false);s.began=SystemClock.elapsedRealtime();s.running=true;s.acquire();DistanceTrackingService.pauseOwned(context,s.steps.get(s.gpsStep).gpsKey,!"work".equals(s.timeline.at(s.elapsed).phase));s.handler.post(s.tick);s.save();}
        else if(("restPlus192".equals(action)||"restMinus192".equals(action))&&!s.stopped){
            IntervalTimeline.Phase p=s.timeline.at(s.now());s.timeLedger.sample(s.timeline,p.elapsedMs,SystemClock.elapsedRealtime(),s.running,s.stopped,s.interrupted);
            s.timeline.adjustRest192(p,action.equals("restPlus192")?5000:-5000);s.elapsed=p.elapsedMs;s.began=SystemClock.elapsedRealtime();s.update();s.save();s.publishWatchClock();
        }
        else if("skipSide192".equals(action)&&!s.stopped){
            IntervalTimeline.Phase p=s.timeline.at(s.now());if(!p.phase.equals("work")||p.side192.isEmpty())throw new IllegalStateException("Not bilateral work");
            s.handler.removeCallbacks(s.tick);s.timeLedger.sample(s.timeline,p.elapsedMs,SystemClock.elapsedRealtime(),s.running,s.stopped,s.interrupted);s.timeLedger.skipWork(p);
            s.elapsed=p.elapsedMs+p.remainingMs;s.timeLedger.skipTo(s.elapsed);s.began=SystemClock.elapsedRealtime();s.running=true;s.acquire();s.update();s.save();if(s.running)s.handler.post(s.tick);
        }
        else if("skipRest".equals(action)&&!s.stopped){IntervalTimeline.Phase p=s.timeline.at(s.now());if(!"rest".equals(p.phase))throw new IllegalStateException("Not resting");s.handler.removeCallbacks(s.tick);s.timeLedger.sample(s.timeline,p.elapsedMs,SystemClock.elapsedRealtime(),s.running,s.stopped,s.interrupted);s.elapsed=p.elapsedMs+p.remainingMs;s.timeLedger.skipTo(s.elapsed);s.began=SystemClock.elapsedRealtime();s.running=true;s.acquire();s.update();if(s.running)s.handler.post(s.tick);}
        s.publishWatchClock();return s.current();
    }
    static JSONObject attachGPS(Context context,String key,int index,String tracking)throws Exception{
        IntervalTimerService s=instance;if(s==null||!key.equals(s.key)||s.timeline.at(s.now()).step!=index||s.stopped||"complete".equals(s.timeline.at(s.now()).phase))throw new IllegalStateException("Interval owner changed");JSONObject gps=DistanceTrackingService.snapshot(context);if(!tracking.equals(gps.optString("key"))||!gps.optBoolean("active"))throw new IllegalStateException("GPS owner changed");s.steps.get(index).gpsKey=tracking;DistanceTrackingService.pauseOwned(context,tracking,!s.running||!"work".equals(s.timeline.at(s.now()).phase));s.save();return s.current();
    }
    @Override public void onTaskRemoved(Intent root){armedWorkoutId="";if(timeline!=null)stopRun();else{stopForeground(true);stopSelf();}}
    @Override public void onDestroy(){if(running){cancelVibration();elapsed=now();running=false;interrupted=true;DistanceTrackingService.stopOwned(this,steps.get(gpsStep).gpsKey);save();}handler.removeCallbacks(tick);handler.removeCallbacks(expire);release();if(tones!=null)tones.release();if(instance==this)instance=null;super.onDestroy();}
}
