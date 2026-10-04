package com.repforge.app;

import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.health.connect.HealthConnectManager;
import com.getcapacitor.JSObject;
import org.json.JSONObject;
import java.time.*;
import java.util.concurrent.*;

/** Health Connect background reads have a separate permission; the BLE service grants none. */
public class HealthBackgroundJob extends JobService {
    static final int ID=1204;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
    static boolean supported(Context context){
        return Build.VERSION.SDK_INT>=34&&SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)>=13
            &&context.getSystemService(HealthConnectManager.class)!=null;
    }
    static boolean permitted(Context context){return supported(context)&&context.checkSelfPermission("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND")==PackageManager.PERMISSION_GRANTED;}
    static void schedule(Context context)throws Exception {
        JobScheduler jobs=(JobScheduler)context.getSystemService(JOB_SCHEDULER_SERVICE);
        if(!HealthJournalStore.get(context).preferences().optBoolean("hcBackground")||!permitted(context)){jobs.cancel(ID);return;}
        if(jobs.getPendingJob(ID)==null&&jobs.schedule(new JobInfo.Builder(ID,new ComponentName(context,HealthBackgroundJob.class)).setPeriodic(15*60*1000L).setPersisted(false).build())!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Cannot schedule Health Connect background sync");
    }
    @Override public boolean onStartJob(JobParameters params){
        final java.util.concurrent.atomic.AtomicBoolean stopped=new java.util.concurrent.atomic.AtomicBoolean();cancelled=stopped;worker.execute(()->{boolean retry=false;try{
            HealthJournalStore store=HealthJournalStore.get(this);long epoch=store.generation();
            HealthBridgePlugin.Access access=new HealthBridgePlugin.Access(this,epoch,()->{
                try{if(stopped.get()||!permitted(this)||!store.preferences().optBoolean("hcBackground")||!store.pendingRestore().isEmpty())throw new IllegalStateException("Background sync stopped");}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
            });
            access.requireForeground();LocalDate today=LocalDate.now();boolean complete=true;
            // Refresh yesterday as well: a health app may export sleep/late activity after midnight.
            for(int i=1;i>=0;i--){LocalDate date=today.minusDays(i);Instant start=date.atStartOfDay(ZoneId.systemDefault()).toInstant(),end=i==0?Instant.now():date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                if(end.isAfter(start)){JSObject day=HealthConnectApi34.readHealthDay(access,new JSObject().put("start",start.toString()).put("end",end.toString()));if(day.optJSONArray("warnings")!=null&&day.optJSONArray("warnings").length()>0)complete=false;}
            }
            access.requireForeground();if(store.generation()!=epoch)throw new IllegalStateException("Dataset changed");JSONObject meta=new JSONObject().put("lastAttemptAt",Instant.now().toString());if(complete)meta.put("lastSyncAt",Instant.now().toString());store.syncMeta(meta);
        }catch(Exception e){retry=!stopped.get()&&permitted(this);}finally{if(!stopped.get())jobFinished(params,retry);}});return true;
    }
    @Override public boolean onStopJob(JobParameters params){cancelled.set(true);return false;}
    @Override public void onDestroy(){cancelled.set(true);worker.shutdownNow();super.onDestroy();}
}
