package com.repforge.app;

import android.content.*;
import java.lang.reflect.Field;
import java.time.Duration;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class PhoneBilateral192ServiceTest {
    private Context context;private PhoneWorkoutExecutionStore store;private ServiceController<IntervalTimerService> service;
    @Before public void open()throws Exception{
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);context.getSharedPreferences(IntervalTimerService.PREFS,0).edit().clear().commit();
        Field f=PhoneWorkoutExecution.class.getDeclaredField("store");f.setAccessible(true);f.set(null,null);store=PhoneWorkoutExecution.store(context);
        JSONObject frame=new JSONObject("{\"active\":true,\"automaticEnabled\":true,\"timerOwner\":\"phone\",\"bilateralProtocol192\":1,\"workoutId\":\"side192\",\"generation\":1,\"executionRevision\":0,\"currentExercise\":0,\"restSeconds\":2,\"timerConfig\":{\"work\":2,\"rest\":2,\"rounds\":1,\"sound\":false,\"vibration\":false},\"exercises\":[{\"id\":\"side-plank\",\"repUnit\":\"mp/oldal\",\"bilateral192\":{\"schema\":1,\"first\":\"left\",\"leftSeconds\":2,\"rightSeconds\":3,\"sideRestSeconds\":1,\"setRestSeconds\":2},\"sets\":[{\"setId\":\"a\",\"done\":false,\"reps\":\"0\",\"leftSeconds\":0,\"rightSeconds\":0},{\"setId\":\"b\",\"done\":false,\"reps\":\"0\",\"leftSeconds\":0,\"rightSeconds\":0}]}]}");
        store.prepare(frame,0);service=Robolectric.buildService(IntervalTimerService.class).create();service.get().onStartCommand(new Intent(context,IntervalTimerService.class).setAction(IntervalTimerService.ARM).putExtra("workoutId","side192").putExtra("generation",1L).putExtra("language","hu"),0,1);
    }
    @After public void close()throws Exception{service.destroy();store.close();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);Field f=PhoneWorkoutExecution.class.getDeclaredField("store");f.setAccessible(true);f.set(null,null);}
    private void advance(int seconds){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds));}
    private JSONObject start()throws Exception{JSONObject c=new JSONObject().put("nativeExecutionProtocol",2).put("bilateralProtocol192",1).put("commandId","start192").put("action","automaticControl").put("value","start").put("workoutId","side192").put("generation",1).put("executionRevision",store.snapshot().getLong("executionRevision")).put("exerciseId","side-plank").put("setId","a");assertEquals("applied",PhoneWorkoutExecution.execute(context,c).getString("status"));return c;}
    private JSONObject set(int index)throws Exception{return store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(index);}
    @Test public void backgroundCompletesEachPairExactlyOnceWithWorkOnlySeconds()throws Exception{
        JSONObject command=start();advance(1);assertEquals(1,set(0).getInt("leftSeconds"));assertFalse(set(0).getBoolean("done"));advance(5);
        assertTrue(set(0).getBoolean("done"));assertFalse(set(1).getBoolean("done"));assertEquals("2",set(0).getString("reps"));assertEquals(3,set(0).getInt("rightSeconds"));
        advance(8);assertTrue(set(1).getBoolean("done"));assertEquals(3,store.pendingEvents("side192",1,0).length());
        assertEquals("duplicate",PhoneWorkoutExecution.execute(context,command).getString("status"));advance(3);assertEquals(3,store.pendingEvents("side192",1,0).length());
        JSONObject timer=IntervalTimerService.snapshot(context);assertEquals("complete",timer.getString("phase"));JSONObject log=timer.getJSONObject("ledger");assertEquals(10000,log.getLong("workMs"));assertEquals(4000,log.getLong("restMs"));
    }
    @Test public void stalePrepareCannotReplaceDurablePartialSideProgress()throws Exception{
        start();JSONObject before=store.snapshot();store.acknowledgeEvents("side192",1,store.pendingEvents("side192",1,0).getJSONObject(0).getLong("sequence"));advance(1);
        assertEquals(1,set(0).getInt("leftSeconds"));store.prepare(before,before.getLong("executionRevision"));assertEquals(1,set(0).getInt("leftSeconds"));
    }
    @Test public void pauseSkipAndRestChangeKeepActualLedger()throws Exception{
        start();advance(2);String key=IntervalTimerService.snapshot(context).getString("key");IntervalTimerService.command(context,key,"pause");advance(4);IntervalTimerService.command(context,key,"resume");IntervalTimerService.command(context,key,"restPlus192");advance(1);IntervalTimerService.command(context,key,"skipRest");advance(3);
        JSONObject timer=IntervalTimerService.snapshot(context),log=timer.getJSONObject("ledger");assertEquals(5000,log.getLong("workMs"));assertEquals(1000,log.getLong("restMs"));assertEquals(4000,log.getLong("pausedMs"));assertTrue(set(0).getBoolean("done"));
    }
    @Test public void serviceRestartRestoresSameSideWithoutInventedGap()throws Exception{
        start();advance(1);String key=IntervalTimerService.snapshot(context).getString("key");service.destroy();advance(20);JSONObject saved=IntervalTimerService.restorableBilateral192(context,key);assertNotNull(saved);assertEquals(1000,saved.getLong("remainingMs"));
        service=Robolectric.buildService(IntervalTimerService.class).create();service.get().onStartCommand(new Intent(context,IntervalTimerService.class).setAction(IntervalTimerService.START).putExtra("plan",saved.toString()),0,2);advance(1);
        JSONObject timer=IntervalTimerService.snapshot(context);assertEquals("rest",timer.getString("phase"));assertEquals("right",timer.getString("side192"));assertEquals(2000,timer.getJSONObject("ledger").getLong("workMs"));assertEquals(0,timer.getJSONObject("ledger").getLong("pausedMs"));
    }
    @Test public void skippedSideNeverRecordsThePairAsDone()throws Exception{
        start();advance(1);String key=IntervalTimerService.snapshot(context).getString("key");IntervalTimerService.command(context,key,"skipSide192");advance(4);assertFalse(set(0).getBoolean("done"));assertTrue(set(0).getJSONObject("sideState192").getBoolean("skipped"));assertEquals(1,set(0).getInt("leftSeconds"));assertEquals(3,set(0).getInt("rightSeconds"));
    }
}
