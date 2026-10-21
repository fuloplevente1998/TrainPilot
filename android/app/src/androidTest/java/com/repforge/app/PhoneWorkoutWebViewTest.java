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
    @Test public void coldNavigationProfilesRealTouchesAcrossRelaunches() throws Exception {
        var instrumentation=InstrumentationRegistry.getInstrumentation();
        Intent launch=new Intent(instrumentation.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        MainActivity seed=(MainActivity)instrumentation.startActivitySync(launch);
        try {
            WebView view=seed.getBridge().getWebView();waitFor(view,"window.TrainPilotBoot?.finished");
            evaluate(view,"""
                db.set('language','hu');db.set('onboarding128','skipped');
                db.set('history',Array.from({length:200},(_,i)=>({id:'cold-nav-'+i,syncId:'cold-nav-'+i,
                 programId:'home-level2',dayId:'A',workout:'A',started:new Date(Date.now()-(i+1)*86400000).toISOString(),
                 finished:new Date(Date.now()-(i+1)*86400000+1800000).toISOString(),
                 exercises:['db-floor-press','db-squat','one-arm-row','rdl','db-curl','plank'].map(id=>({id,
                  loadType:'per_hand',repUnit:'ism.',sets:[{set:1,weight:8,reps:'10',done:true}]}))})));
                true
                """);
        } finally {instrumentation.runOnMainSync(seed::finish);}
        instrumentation.waitForIdleSync();
        org.json.JSONArray launches=new org.json.JSONArray();
        for(int attempt=0;attempt<2;attempt++) {
            MainActivity activity=(MainActivity)instrumentation.startActivitySync(launch);
            try {
                WebView view=activity.getBridge().getWebView();waitFor(view,"window.TrainPilotBoot?.finished");
                evaluate(view,"window.__nativeExitReady=false;Promise.resolve(TrainPilotBoot.nativeExitFinished).then(()=>window.__nativeExitReady=true);true");
                waitFor(view,"window.__nativeExitReady");waitForWindowFocus(activity);
                instrumentation.runOnMainSync(view::requestFocus);
                waitFor(view,"document.hasFocus()");
                evaluate(view,"""
                    window.__coldNav={rows:[],longTasks:[],pending:null,depth:0,events:[]};
                    for(const type of ['pointerdown','pointerup','click'])document.addEventListener(type,event=>{
                     __coldNav.events.push({type,target:event.target.id||event.target.className,x:event.clientX,y:event.clientY});
                     if(__coldNav.events.length>8)__coldNav.events.shift();
                    },true);
                    try{new PerformanceObserver(list=>{for(const e of list.getEntries())__coldNav.longTasks.push({at:e.startTime,ms:e.duration});}).observe({type:'longtask',buffered:true});}catch(_){}
                    for(const name of ['go','tp155R4OpenPanel','tp155R4ClosePanel']){
                     const base=window[name];window[name]=function(){const p=__coldNav.pending,outer=__coldNav.depth++===0,start=performance.now();
                      try{return base.apply(this,arguments);}finally{__coldNav.depth--;if(p&&outer)p.syncMs=performance.now()-start;}};
                    }
                    document.addEventListener('click',event=>{
                     const row=__coldNav.pending;if(!row||!event.target.closest('.top'))return;
                     const at=performance.now();row.at=at;row.inputDelayMs=Math.max(0,at-event.timeStamp);
                     requestAnimationFrame(()=>requestAnimationFrame(()=>{row.paintMs=performance.now()-at;row.nodes=document.querySelectorAll('*').length;
                      row.tab=state.tab;row.panel=document.getElementById('tp155R4PanelHost')?.dataset.panel||'';__coldNav.rows.push(row);__coldNav.pending=null;}));
                    },true);true
                    """);
                String[] routes={"plan","health","programs","history","calendar","coach","settings","home"};
                for(int cycle=1;cycle<=2;cycle++)for(String route:routes){
                    String selector=route.equals("coach")?".top .tp154-coach-action":route.equals("settings")?".top .tp154-settings-action":
                        ".top [onclick=\"go('"+route+"')\"]";
                    String encoded=evaluate(view,"JSON.stringify((()=>{const e=document.querySelector("+JSONObject.quote(selector)+");if(!e)throw Error('Missing navigation '+"+JSONObject.quote(route)+");const r=e.getBoundingClientRect();if(!e.contains(document.elementFromPoint(r.x+r.width/2,r.y+r.height/2)))throw Error('Navigation touch covered: '+document.elementFromPoint(r.x+r.width/2,r.y+r.height/2)?.outerHTML);__coldNav.pending={route:"+JSONObject.quote(route)+",cycle:"+cycle+"};return {x:r.x+r.width/2,y:r.y+r.height/2,viewportWidth:innerWidth,viewportHeight:innerHeight,dpr:devicePixelRatio};})())");
                    JSONObject position=new JSONObject(new org.json.JSONArray("["+encoded+"]").getString(0));
                    float[] touch=new float[2];
                    instrumentation.runOnMainSync(()->{
                        // CSS viewport pixels need the actual view/viewport scale. DPR alone
                        // is wrong when WebView scales a layout viewport to the screen.
                        float scale=(float)(view.getWidth()/position.optDouble("viewportWidth"));
                        float x=(float)position.optDouble("x")*scale,y=(float)position.optDouble("y")*scale;
                        assertTrue("Navigation touch outside WebView: "+position,x>=0&&x<view.getWidth()&&y>=0&&y<view.getHeight());
                        touch[0]=x;touch[1]=y;
                    });
                    long time=android.os.SystemClock.uptimeMillis();
                    instrumentation.runOnMainSync(()->{
                        android.view.MotionEvent event=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,touch[0],touch[1],0);
                        try{view.dispatchTouchEvent(event);}finally{event.recycle();}
                    });
                    android.os.SystemClock.sleep(30);
                    instrumentation.runOnMainSync(()->{
                        android.view.MotionEvent event=android.view.MotionEvent.obtain(time,android.os.SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,touch[0],touch[1],0);
                        try{view.dispatchTouchEvent(event);}finally{event.recycle();}
                    });
                    waitFor(view,"window.__coldNav.pending===null");
                    String result=evaluate(view,"JSON.stringify(__coldNav.rows.at(-1))");JSONObject row=new JSONObject(new org.json.JSONArray("["+result+"]").getString(0));
                    assertEquals(route,row.getString("route"));assertTrue(row.toString(),row.getDouble("paintMs")>=0);
                    assertEquals(row.toString(),route.equals("calendar")||route.equals("coach")||route.equals("settings")?route:"",row.getString("panel"));
                    if(!route.equals("calendar")&&!route.equals("coach")&&!route.equals("settings"))assertEquals(row.toString(),route,row.getString("tab"));
                }
                evaluate(view,"""
                    __coldNav.catalog=[];let reference=null;
                    for(const scoped of [false,true,true,false]){
                     const scope=window.tp7WithHistorySnapshot,get=Storage.prototype.getItem;let reads=0;
                     if(!scoped)window.tp7WithHistorySnapshot=fn=>fn();
                     Storage.prototype.getItem=function(key){reads++;return get.call(this,key);};
                     try{const start=performance.now(),snapshot=TrainPilotWearSync.makeHomeSnapshot(),payload=JSON.stringify(snapshot);
                      if(reference!==null&&reference!==payload)throw Error('Catalog scope changed output');reference=payload;
                      __coldNav.catalog.push({scoped,ms:performance.now()-start,reads,exercises:snapshot.quickExercises.length});
                     }finally{window.tp7WithHistorySnapshot=scope;Storage.prototype.getItem=get;}
                    }true
                    """);
                String encoded=evaluate(view,"JSON.stringify({bootMs:TrainPilotBoot.durationMs,rows:__coldNav.rows,longTasks:__coldNav.longTasks,catalog:__coldNav.catalog})");
                JSONObject result=new JSONObject(new org.json.JSONArray("["+encoded+"]").getString(0));result.put("launch",attempt+1);launches.put(result);
                assertEquals(16,result.getJSONArray("rows").length());assertEquals(4,result.getJSONArray("catalog").length());
            } catch(Exception|AssertionError failure) {
                try {
                    String diagnostic=evaluate(activity.getBridge().getWebView(),"JSON.stringify({url:location.href,viewport:{width:innerWidth,height:innerHeight,dpr:devicePixelRatio},pending:window.__coldNav?.pending,events:window.__coldNav?.events,rows:window.__coldNav?.rows?.length,focus:document.hasFocus()})");
                    System.out.println("COLD_NAVIGATION_FAILURE "+new org.json.JSONArray("["+diagnostic+"]").getString(0));
                    java.io.File output=new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"cold-navigation.json");
                    try(java.io.FileWriter writer=new java.io.FileWriter(output)){writer.write(new JSONObject().put("failedLaunch",attempt+1).put("error",failure.toString()).put("state",new org.json.JSONArray("["+diagnostic+"]").getString(0)).toString(2));}
                }catch(Exception diagnosticFailure){failure.addSuppressed(diagnosticFailure);}
                throw failure;
            } finally {instrumentation.runOnMainSync(activity::finish);}
            instrumentation.waitForIdleSync();
        }
        JSONObject report=new JSONObject().put("fixtureWorkouts",200).put("launches",launches)
            .put("limits","Debug emulator timings; Android MotionEvents dispatched to WebView, without system input routing. Activity/WebView relaunches, not process-force-stop. Two-RAF proxy, no device-FPS claim.");
        java.io.File output=new java.io.File(instrumentation.getTargetContext().getExternalFilesDir(null),"cold-navigation.json");
        try(java.io.FileWriter writer=new java.io.FileWriter(output)){writer.write(report.toString(2));}
        // Logcat truncates individual entries at ~4 KB. Export small records too:
        // Gradle may remove the target package before a post-test adb pull.
        System.out.println("COLD_NAVIGATION_META "+new JSONObject().put("fixtureWorkouts",200).put("limits",report.getString("limits")));
        for(int i=0;i<launches.length();i++) {
            JSONObject result=launches.getJSONObject(i);
            System.out.println("COLD_NAVIGATION_LAUNCH "+new JSONObject().put("launch",i+1).put("bootMs",result.getDouble("bootMs")).put("catalog",result.getJSONArray("catalog")));
            for(int j=0;j<result.getJSONArray("rows").length();j++)
                System.out.println("COLD_NAVIGATION_ROW "+new JSONObject().put("launch",i+1).put("row",result.getJSONArray("rows").getJSONObject(j)));
            for(int j=0;j<result.getJSONArray("longTasks").length();j++)
                System.out.println("COLD_NAVIGATION_TASK "+new JSONObject().put("launch",i+1).put("task",result.getJSONArray("longTasks").getJSONObject(j)));
        }
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
