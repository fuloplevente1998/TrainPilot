package com.repforge.app;

import android.content.Intent;
import android.webkit.WebView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises the packaged WebView, Capacitor dispatcher, SQLite and foreground service together. */
@RunWith(AndroidJUnit4.class)
public class PhoneWorkoutWebViewTest {
    private String evaluate(WebView view,String script) throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->view.evaluateJavascript(script,value->{result.set(value);done.countDown();}));
        assertTrue("WebView callback timed out",done.await(10,TimeUnit.SECONDS));return result.get();
    }
    private void waitFor(WebView view,String expression) throws Exception {
        long end=System.currentTimeMillis()+30000;
        do{if("true".equals(evaluate(view,"Boolean("+expression+")")))return;Thread.sleep(100);}while(System.currentTimeMillis()<end);
        fail("WebView condition timed out: "+expression);
    }
    @Test public void phoneRecordingUsesRealNativeBackgroundController() throws Exception {
        Intent launch=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)InstrumentationRegistry.getInstrumentation().startActivitySync(launch);
        try{
            AtomicReference<WebView> reference=new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->reference.set(activity.getBridge().getWebView()));
            WebView view=reference.get();waitFor(view,"window.TrainPilotBoot?.finished");
            evaluate(view,"window.__executionTest={};(async()=>{try{"
                +"const api=window.Capacitor.Plugins.WearSync||window.Capacitor.registerPlugin('WearSync');"
                +"const initial=await api.executionStatus();"
                +"const exercise={...byId('db-floor-press'),sets:[{set:1,setId:'fixture-a',reps:'5',weight:5.5,done:false},{set:2,setId:'fixture-b',reps:'6',weight:5.5,done:false}]};"
                +"state.session={started:new Date().toISOString(),syncId:'webview-background-fixture',timerOwner:'phone',quickWorkout:true,type:'quick',exercises:[exercise]};state.current=0;state.workout='quick';state.tab='plan';"
                +"db.set('settings',{...settings(),rest:120,automaticWorkout162:false,intervalPreset110:{work:5,rest:3,rounds:1,sound:false,vibration:false}});persistDraft();renderWorkout();await TrainPilotAutomatic162.toggle(true);"
                +"let ready=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()&&!TrainPilotWearSync.isApplyingCommand()){ready=await TrainPilotPhoneExecution162.sync();if(ready||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,50));}"
                +"if(!ready)throw Error('prepare: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const recorded=await TrainPilotPhoneExecution162.recordPhoneSet(0,0);if(!recorded)throw Error('record: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const result=await api.executionStatus();window.__executionTest={done:true,recorded:exercise.sets[0].done,controllerArmed:result.controllerArmed,phase:result.timer.phase,remainingMs:result.timer.remainingMs,failure:TrainPilotPhoneExecution162.failure()};"
                +"}catch(e){window.__executionTest={done:true,error:String(e),stack:e.stack};}})();true");
            waitFor(view,"window.__executionTest?.done");
            String encoded=evaluate(view,"JSON.stringify(window.__executionTest)");
            String decoded=new org.json.JSONArray("["+encoded+"]").getString(0);JSONObject result=new JSONObject(decoded);
            assertFalse(result.toString(),result.has("error"));assertTrue(result.toString(),result.getBoolean("recorded"));
            assertTrue(result.toString(),result.getBoolean("controllerArmed"));assertEquals(result.toString(),"rest",result.getString("phase"));
            assertTrue(result.getLong("remainingMs")>110000);assertTrue(result.isNull("failure"));
        }finally{InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);}
    }
}
