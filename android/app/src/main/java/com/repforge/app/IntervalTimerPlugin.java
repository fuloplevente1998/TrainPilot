package com.repforge.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import androidx.core.content.ContextCompat;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import org.json.JSONObject;

@CapacitorPlugin(name="IntervalTimer",permissions={@Permission(alias="notifications",strings={Manifest.permission.POST_NOTIFICATIONS})})
public class IntervalTimerPlugin extends Plugin {
    private boolean starting;
    @PluginMethod public void status(PluginCall c){getActivity().runOnUiThread(()->{try{c.resolve(new JSObject(IntervalTimerService.snapshot(getContext()).toString()));}catch(Exception e){c.reject(e.getMessage());}});}
    @PluginMethod public void start(PluginCall c){
        try{IntervalTimerService.validate(c.getData());if(starting)throw new IllegalStateException("Interval is starting");JSONObject previous=IntervalTimerService.snapshot(getContext());if(previous.optBoolean("running")||!previous.optBoolean("stopped",true)&&!"complete".equals(previous.optString("phase"))&&!previous.optBoolean("interrupted"))throw new IllegalStateException("Stop the current interval first");starting=true;
            if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(getContext(),Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissionForAlias("notifications",c,"notificationResult");else begin(c);
        }catch(Exception e){c.reject(e.getMessage());}
    }
    @PermissionCallback private void notificationResult(PluginCall c){begin(c);}
    private void begin(PluginCall c){getActivity().runOnUiThread(()->{try{
        ContextCompat.startForegroundService(getContext(),new Intent(getContext(),IntervalTimerService.class).setAction(IntervalTimerService.START).putExtra("plan",c.getData().toString()));
        Handler h=new Handler(Looper.getMainLooper());h.postDelayed(new Runnable(){int count;public void run(){JSONObject s=IntervalTimerService.snapshot(getContext());if(c.getString("key").equals(s.optString("key"))&&s.optBoolean("running")){starting=false;try{c.resolve(new JSObject(s.toString()));}catch(Exception e){c.reject(e.getMessage());}}else if(++count<30)h.postDelayed(this,100);else{starting=false;c.reject("Interval did not start. Retry with the app open.");}}},100);
    }catch(Exception e){starting=false;c.reject(e.getMessage());}});}
    private void command(PluginCall c,String action){getActivity().runOnUiThread(()->{try{c.resolve(new JSObject(IntervalTimerService.command(getContext(),c.getString("key",""),action).toString()));}catch(Exception e){c.reject(e.getMessage());}});}
    @PluginMethod public void pause(PluginCall c){command(c,"pause");}
    @PluginMethod public void resume(PluginCall c){command(c,"resume");}
    @PluginMethod public void stop(PluginCall c){command(c,"stop");}
    @PluginMethod public void attachGPS(PluginCall c){getActivity().runOnUiThread(()->{try{c.resolve(new JSObject(IntervalTimerService.attachGPS(getContext(),c.getString("key",""),c.getInt("index",-1),c.getString("trackingKey","")).toString()));}catch(Exception e){c.reject(e.getMessage());}});}
}
