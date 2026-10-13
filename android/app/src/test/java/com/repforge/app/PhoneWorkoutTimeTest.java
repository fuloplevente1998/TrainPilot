package com.repforge.app;
import android.content.Context;
import android.content.Intent;
import java.time.Duration;
import java.lang.reflect.Field;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ServiceController;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class PhoneWorkoutTimeTest {
    private Context context;private PhoneWorkoutExecutionStore store;private ServiceController<IntervalTimerService> service;
    @Before public void open()throws Exception{
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        Field f=PhoneWorkoutExecution.class.getDeclaredField("store");f.setAccessible(true);f.set(null,null);store=PhoneWorkoutExecution.store(context);
        store.prepare(new JSONObject("{\"active\":true,\"automaticEnabled\":true,\"timerOwner\":\"phone\",\"workoutId\":\"workout\",\"generation\":1,\"executionRevision\":0,\"currentExercise\":0,\"restSeconds\":20,\"timerConfig\":{\"work\":5,\"rest\":3,\"rounds\":2,\"sound\":false,\"vibration\":false},\"exercises\":[{\"id\":\"plank\",\"repUnit\":\"mp\",\"sets\":[{\"setId\":\"a\",\"done\":false,\"reps\":\"2\"}]},{\"id\":\"press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"b\",\"done\":false,\"reps\":\"\"}]}]}"),0);
        service=Robolectric.buildService(IntervalTimerService.class).create();service.get().onStartCommand(new Intent(context,IntervalTimerService.class).setAction(IntervalTimerService.ARM).putExtra("workoutId","workout").putExtra("generation",1L).putExtra("language","hu"),0,1);
    }
    @After public void close()throws Exception{service.destroy();store.close();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);Field f=PhoneWorkoutExecution.class.getDeclaredField("store");f.setAccessible(true);f.set(null,null);}
    private void advance(int seconds){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds));}
    private JSONObject start()throws Exception{
        JSONObject command=new JSONObject().put("nativeExecutionProtocol",2).put("commandId","start").put("workoutId","workout").put("generation",1)
            .put("executionRevision",store.snapshot().getLong("executionRevision")).put("action","automaticControl").put("value","start").put("exerciseId","plank").put("setId","a");
        assertEquals("applied",PhoneWorkoutExecution.execute(context,command).getString("status"));return command;
    }
    @Test public void sleepingUiCompletionCommitsActualWorkRestAndSetTogether()throws Exception{
        start();advance(13);JSONObject frame=store.snapshot(),set=frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0);
        assertTrue(set.getBoolean("done"));assertEquals("15",set.getString("reps"));assertEquals(1,frame.getInt("currentExercise"));
        JSONObject log=frame.getJSONObject("timeLogs162").getJSONObject(frame.getJSONObject("latestTimer162").getString("key"));
        assertEquals(10000,log.getLong("workMs"));assertEquals(3000,log.getLong("restMs"));assertEquals(0,log.getLong("pausedMs"));
        assertEquals(2,store.pendingEvents("workout",1,0).length());
    }
    @Test public void pauseAndSkippedRestReceiveOnlyActualCredit()throws Exception{
        start();advance(5);String key=IntervalTimerService.snapshot(context).getString("key");
        IntervalTimerService.command(context,key,"pause");advance(4);IntervalTimerService.command(context,key,"resume");advance(1);
        IntervalTimerService.command(context,key,"skipRest");advance(5);
        JSONObject log=store.snapshot().getJSONObject("timeLogs162").getJSONObject(key);
        assertEquals(10000,log.getLong("workMs"));assertEquals(1000,log.getLong("restMs"));assertEquals(4000,log.getLong("pausedMs"));
        assertEquals("13",store.snapshot().getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
    }
    @Test public void nativeTimedCompletionDoesNotBlockNextRepetitionRecord()throws Exception{
        start();advance(13);JSONObject frame=store.snapshot();
        JSONObject command=new JSONObject().put("nativeExecutionProtocol",2).put("commandId","record").put("action","completeSet")
            .put("workoutId","workout").put("generation",1).put("executionRevision",frame.getLong("executionRevision")).put("exerciseId","press").put("setId","b").put("reps","7");
        advance(2);assertEquals("applied",PhoneWorkoutExecution.execute(context,command).getString("status"));
        JSONObject logs=store.snapshot().getJSONObject("timeLogs162");long work=0;for(java.util.Iterator<String> i=logs.keys();i.hasNext();)work+=logs.getJSONObject(i.next()).getLong("workMs");assertEquals(12000,work);
    }
    @Test public void staleCheckpointCannotRewindLedger()throws Exception{
        start();advance(4);JSONObject current=IntervalTimerService.snapshot(context),older=new JSONObject(current.toString());advance(2);
        assertFalse(store.checkpointTimer(older,android.os.SystemClock.elapsedRealtime()));assertEquals(5000,store.snapshot().getJSONObject("timeLogs162").getJSONObject(current.getString("key")).getLong("workMs"));
    }
    @Test public void explicitAmendmentRequiresImportedEventsAndExactBase()throws Exception{
        start();advance(13);JSONObject frame=store.snapshot(),set=frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0);
        JSONObject bases=new JSONObject().put("a",PhoneWorkoutExecutionStore.recordedBase(set));
        JSONObject edit=new JSONObject(frame.toString());edit.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","12").put("done",false);
        long revision=frame.getLong("executionRevision");assertThrows(IllegalStateException.class,()->store.amend(edit,revision,bases));
        JSONArray events=store.pendingEvents("workout",1,0);store.acknowledgeEvents("workout",1,events.getJSONObject(events.length()-1).getLong("sequence"));
        assertThrows(IllegalStateException.class,()->store.amend(edit,revision,new JSONObject()));
        JSONObject result=store.amend(edit,revision,bases);assertFalse(result.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getBoolean("done"));
        assertTrue(result.has("timeLogs162"));assertThrows(IllegalStateException.class,()->store.amend(edit,revision,bases));
    }
}
