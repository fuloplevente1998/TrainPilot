package com.repforge.app;

import android.content.Context;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
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
    private WearSyncPlugin plugin;
    private Field storeField;
    private static final class Call extends PluginCall {
        JSObject result; Exception error;
        Call(String method,JSObject data){super(null,"WearSync","test",method,data);}
        @Override public void resolve(JSObject data){result=data;}
        @Override public void reject(String message,Exception cause){error=cause;}
    }
    @Before public void open() throws Exception {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
        storeField=PhoneWorkoutExecution.class.getDeclaredField("store");storeField.setAccessible(true);storeField.set(null,null);
        plugin=new WearSyncPlugin(){@Override public Context getContext(){return context;}};
    }
    @After public void close() throws Exception {
        PhoneWorkoutExecutionStore db=(PhoneWorkoutExecutionStore)storeField.get(null);if(db!=null)db.close();
        storeField.set(null,null);context.deleteDatabase(PhoneWorkoutExecutionStore.DATABASE);
    }
    @Test public void firstStatusReadsEmptyDatabaseThroughCapacitorPlugin() throws Exception {
        Call call=new Call("executionStatus",new JSObject());plugin.executionStatus(call);ShadowLooper.idleMainLooper();
        assertNull(call.error);assertNotNull(call.result);assertTrue(call.result.isNull("snapshot"));
        assertFalse(call.result.getBoolean("controllerArmed"));assertEquals(0,call.result.getJSONArray("events").length());
    }
    @Test public void statusReadsPreparedWorkoutThroughCapacitorPlugin() throws Exception {
        JSObject data=new JSObject("{\"expectedRevision\":0,\"snapshot\":{\"active\":true,\"timerOwner\":\"phone\",\"workoutId\":\"fixture\",\"generation\":1770000000000,\"executionRevision\":0,\"exercises\":[{\"id\":\"db-floor-press\",\"repUnit\":\"ism.\",\"sets\":[{\"setId\":\"set-a\",\"done\":false,\"reps\":\"5\",\"weight\":5.5}]}]}}");
        Call prepare=new Call("prepareExecution",data);plugin.prepareExecution(prepare);ShadowLooper.idleMainLooper();
        assertNull(prepare.error);assertNotNull(prepare.result);
        Call call=new Call("executionStatus",new JSObject());plugin.executionStatus(call);ShadowLooper.idleMainLooper();
        assertNull(call.error);JSONObject frame=call.result.getJSONObject("snapshot");
        assertEquals(1770000000000L,frame.getLong("generation"));assertEquals(1,frame.getLong("executionRevision"));
        assertEquals("5",frame.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
    }
}
