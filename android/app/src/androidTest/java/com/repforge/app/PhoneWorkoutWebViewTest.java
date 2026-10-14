package com.repforge.app;

import android.content.Intent;
import android.webkit.WebView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private void waitForWindowFocus(MainActivity activity) throws Exception {
        long end=System.currentTimeMillis()+30000;AtomicBoolean focused=new AtomicBoolean();
        do{
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->focused.set(activity.hasWindowFocus()));
            if(focused.get())return;
            Thread.sleep(100);
        }while(System.currentTimeMillis()<end);
        fail("The foreground phone Activity did not receive window focus");
    }
    @Test public void phoneRecordingUsesRealNativeBackgroundController() throws Exception {
        Intent launch=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity activity=(MainActivity)InstrumentationRegistry.getInstrumentation().startActivitySync(launch);
        try{
            AtomicReference<WebView> reference=new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->reference.set(activity.getBridge().getWebView()));
            WebView view=reference.get();waitFor(view,"window.TrainPilotBoot?.finished");
            waitForWindowFocus(activity);
            evaluate(view,"window.__executionTest={};(async()=>{try{"
                +"const api=window.Capacitor.Plugins.WearSync||window.Capacitor.registerPlugin('WearSync');"
                +"const initial=await api.executionStatus();"
                +"const exercise={...byId('db-floor-press'),sets:[{set:1,setId:'fixture-a',reps:'5',weight:5.5,done:false},{set:2,setId:'fixture-b',reps:'6',weight:5.5,done:false}]};"
                +"state.session={started:new Date().toISOString(),syncId:'webview-background-fixture',timerOwner:'phone',quickWorkout:true,type:'quick',exercises:[exercise]};state.current=0;state.workout='quick';state.tab='plan';"
                +"db.set('settings',{...settings(),rest:120,automaticWorkout162:false,intervalPreset110:{work:5,rest:3,rounds:1,sound:false,vibration:false}});persistDraft();renderWorkout();await TrainPilotAutomatic162.toggle(true);"
                +"let ready=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()&&!TrainPilotWearSync.isApplyingCommand()){ready=await TrainPilotPhoneExecution162.sync();if(ready||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,50));}"
                +"if(!ready)throw Error('prepare: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const recorded=await TrainPilotPhoneExecution162.recordPhoneSet(0,0);if(!recorded)throw Error('record: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const clock=window.Capacitor.Plugins.IntervalTimer||window.Capacitor.registerPlugin('IntervalTimer');let lagObserved=false;"
                +"for(let i=0;i<25;i++){const live=await clock.status();const cached=await api.executionStatus();const older=cached.snapshot.timeLogs162?.[live.key];if(live.ledger&&older&&live.ledger.positionMs>older.positionMs){TrainPilotTime162Core.merge(state.session,{[live.key]:{...live.ledger,key:live.key}});persistDraft();lagObserved=true;break;}await new Promise(r=>setTimeout(r,25));}"
                +"if(!lagObserved)throw Error('No live/persisted checkpoint lag observed');"
                +"let reconciled=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()&&!TrainPilotWearSync.isApplyingCommand()){reconciled=await TrainPilotPhoneExecution162.sync({prepare:false});if(reconciled||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,25));}"
                +"if(!reconciled)throw Error('checkpoint: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"upd(0,0,'reps','7');"
                +"let corrected=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()){corrected=await TrainPilotPhoneExecution162.sync();if(corrected||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,25));}"
                +"if(!corrected)throw Error('correction: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const edited=await api.executionStatus();if(edited.snapshot.exercises[0].sets[0].reps!=='7'||!edited.snapshot.exercises[0].sets[0].done)throw Error('corrected set did not reach native storage');"
                +"await toggleSet(0,0);"
                +"let undone=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()){undone=await TrainPilotPhoneExecution162.sync();if(undone||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,25));}"
                +"if(!undone)throw Error('undo: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const reversed=await api.executionStatus();if(reversed.snapshot.exercises[0].sets[0].done||exercise.sets[0].done)throw Error('explicit undo did not reach native storage');"
                +"let rerecorded=false;for(let i=0;i<100;i++){if(!TrainPilotPhoneExecution162.isReconciling()&&!TrainPilotWearSync.isApplyingCommand()){rerecorded=await TrainPilotPhoneExecution162.recordPhoneSet(0,0);if(rerecorded||TrainPilotPhoneExecution162.failure())break;}await new Promise(r=>setTimeout(r,25));}"
                +"if(!rerecorded)throw Error('rerecord: '+JSON.stringify(TrainPilotPhoneExecution162.failure()));"
                +"const result=await api.executionStatus();window.__executionTest={done:false,recorded:exercise.sets[0].done,controllerArmed:result.controllerArmed,phase:result.timer.phase,remainingMs:result.timer.remainingMs,failure:TrainPilotPhoneExecution162.failure()};"
                +"const old=result.snapshot;const discarded=await TrainPilotWearSync.discardDraft(state.session);if(!discarded)throw Error('native discard did not finish');"
                +"const closed=await api.executionStatus();if(closed.snapshot.active||closed.controllerArmed||db.get('draft')||state.session)throw Error('discard left an active execution or draft');"
                +"const late=await api.executeCommand({command:{nativeExecutionProtocol:2,commandId:'after-discard',action:'completeSet',workoutId:old.workoutId,generation:old.generation,executionRevision:old.executionRevision,exerciseId:old.exercises[0].id,setId:old.exercises[0].sets[1].setId,reps:'6'}});if(late.status!=='rejected')throw Error('discarded native command was accepted');"
                +"window.__executionTest.done=true;"
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
