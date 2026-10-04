package com.repforge.app;

import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import org.json.JSONObject;

@CapacitorPlugin(name="HealthJournal")
public class HealthJournalPlugin extends Plugin {
    private volatile boolean foreground=true;
    @Override protected void handleOnPause(){foreground=false;}
    @Override protected void handleOnResume(){foreground=true;}
    private interface Action { JSONObject run() throws Exception; }
    private HealthJournalStore store() { return HealthJournalStore.get(getContext()); }
    private void run(PluginCall call, Action action) {
        getBridge().execute(() -> { try { call.resolve(JSObject.fromJSONObject(action.run())); }
            catch(Exception e) { call.reject("Health journal: "+e.getMessage(),"HEALTH_JOURNAL_FAILED",e); } });
    }
    @PluginMethod public void initialize(PluginCall call) { run(call,()->{store().retireWatchUi();HealthBackgroundService.stop(getContext());return store().initialize(call.getObject("legacy",new JSObject()));}); }
    @PluginMethod public void getProjection(PluginCall call) { run(call,()->store().projection()); }
    @PluginMethod public void getDirtyDays(PluginCall call) { run(call,()->new JSONObject().put("days",store().dirtyDays())); }
    @PluginMethod public void setPreferences(PluginCall call) { run(call,()->{store().setPreferences(call.getObject("preferences",new JSObject()));return store().projection();}); }
    @PluginMethod public void saveSyncMeta(PluginCall call) { run(call,()->{store().syncMeta(call.getObject("meta",new JSObject()));return new JSONObject();}); }
    @PluginMethod public void readPage(PluginCall call) { run(call,()->{
        String source=call.getString("source",""),channel=call.getString("channel","");String warning="";
        if(channel.equals(HealthJournalStore.HC)&&!source.isEmpty()&&android.os.Build.VERSION.SDK_INT>=34){
            try{HealthBridgePlugin.Api34.sourceDays(new HealthBridgePlugin.Access(getContext(),store().generation(),()->{if(!foreground)throw new IllegalStateException("Keep the app open");}),call.getString("from",""),call.getString("to",""),source);}catch(Exception e){warning="SOURCE_SUMMARY_UNAVAILABLE";}
        }
        return store().page(call.getString("from",""),call.getString("to",""),channel,source,call.getLong("before",0L),call.getLong("beforeTime",0L),call.getInt("limit",30)).put("sourceSummaryWarning",warning);
    }); }
    @PluginMethod public void exportSnapshot(PluginCall call) { run(call,()->store().exportSnapshot()); }
    @PluginMethod public void prepareRestore(PluginCall call) { run(call,()->{HealthBackgroundService.stop(getContext());((android.app.job.JobScheduler)getContext().getSystemService(android.content.Context.JOB_SCHEDULER_SERVICE)).cancel(HealthBackgroundJob.ID);try{return new JSONObject().put("token",store().prepareRestore(call.getObject("snapshot",new JSObject())));}catch(Exception e){if(foreground){HealthBackgroundService.stop(getContext());HealthBackgroundJob.schedule(getContext());}throw e;}}); }
    @PluginMethod public void installRestore(PluginCall call) { run(call,()->{store().installRestore(call.getString("token",""));return new JSONObject();}); }
    @PluginMethod public void finishRestore(PluginCall call) { run(call,()->{store().finishRestore(call.getString("token",""),call.getBoolean("commit",false));if(foreground){HealthBackgroundService.stop(getContext());HealthBackgroundJob.schedule(getContext());}return new JSONObject();}); }
    @PluginMethod public void pendingRestore(PluginCall call) { run(call,()->new JSONObject().put("token",store().pendingRestore())); }
}
