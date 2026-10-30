package com.repforge.app;

import android.content.Context;
import java.lang.reflect.Field;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33, manifest=Config.NONE)
public class PhoneWorkoutBridgeTest {
    private Context context;
    private Field storeField;
    @Before public void open() throws Exception {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        storeField=PhoneWorkoutExecution.class.getDeclaredField("store");storeField.setAccessible(true);storeField.set(null,null);
    }
    @After public void close() throws Exception {
        PhoneWorkoutExecutionStore db=(PhoneWorkoutExecutionStore)storeField.get(null);if(db!=null)db.close();
        storeField.set(null,null);context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
    }
    @Test public void firstStatusReadsEmptyDatabaseThroughNativeApi() throws Exception {
        JSONObject result=PhoneWorkoutExecution.status(context);
        assertTrue(result.isNull("snapshot"));assertFalse(result.getBoolean("controllerArmed"));
        assertEquals(0,result.getJSONArray("events").length());
    }
    @Test public void statusReadsPreparedWorkoutThroughNativeApi() throws Exception {
        JSONObject frame=new JSONObject("{\"active\":true,\"timerOwner\":\"phone\",\"workoutId\":\"fixture\",\"generation\":1770000000000,\"executionRevision\":0,\"exercises\":[{\"id\":\"db-floor-press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"set-a\",\"done\":false,\"reps\":\"5\",\"weight\":5.5}]}]}");
        PhoneWorkoutExecution.store(context).prepare(frame,0);
        JSONObject stored=PhoneWorkoutExecution.status(context).getJSONObject("snapshot");
        assertEquals(1770000000000L,stored.getLong("generation"));assertEquals(1,stored.getLong("executionRevision"));
        assertEquals("5",stored.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
    }
}
