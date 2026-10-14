package com.repforge.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class WearCommandQueueStoreTest {
    private Context context;

    @Before public void clearQueue() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("trainpilot_wear_commands", Context.MODE_PRIVATE)
            .edit().clear().commit();
    }

    private JSONObject command(String id, String action) throws Exception {
        return new JSONObject().put("commandId", id).put("workoutId", "session-1")
            .put("action", action);
    }

    @Test public void overflowKeepsOldUnacknowledgedCommands() throws Exception {
        for (int i = 0; i < 64; i++)
            WearCommandQueueStore.enqueue(context, command("record-" + i, "recordSet"), "wear:/" + i);
        WearCommandQueueStore.enqueue(context, command("record-overflow", "recordSet"), "wear:/overflow");
        JSONArray pending = WearCommandQueueStore.pending(context);
        assertEquals(64, pending.length());
        assertEquals("record-0", pending.getJSONObject(0).getString("commandId"));
        assertEquals("record-63", pending.getJSONObject(63).getString("commandId"));

        // The overflow DataItem remains in Wear Data Layer; after a receipt it
        // can be scanned and enqueued without losing any earlier recording.
        assertTrue(WearCommandQueueStore.ack(context, "record-0"));
        WearCommandQueueStore.enqueue(context, command("record-overflow", "recordSet"), "wear:/overflow");
        pending = WearCommandQueueStore.pending(context);
        assertEquals(64, pending.length());
        assertEquals("record-1", pending.getJSONObject(0).getString("commandId"));
        assertEquals("record-overflow", pending.getJSONObject(63).getString("commandId"));
    }

    @Test public void receiptPersistsAndPreventsDuplicateEnqueue() throws Exception {
        WearCommandQueueStore.enqueue(context, command("record-1", "recordSet"), "wear:/1");
        assertTrue(WearCommandQueueStore.ack(context, "record-1"));
        assertTrue(WearCommandQueueStore.seen(context, "record-1"));
        WearCommandQueueStore.enqueue(context, command("record-1", "recordSet"), "wear:/1");
        assertEquals(0, WearCommandQueueStore.pending(context).length());
    }

    @Test public void newerWatchStateMayReplaceOldStateEvenAtCapacity() throws Exception {
        WearCommandQueueStore.enqueue(context, command("state-1", "watchAutomationState")
            .put("sequence", 1), "wear:/state");
        for (int i = 1; i < 64; i++)
            WearCommandQueueStore.enqueue(context, command("record-" + i, "recordSet"), "wear:/" + i);
        WearCommandQueueStore.enqueue(context, command("state-2", "watchAutomationState")
            .put("sequence", 2), "wear:/state");
        JSONArray pending = WearCommandQueueStore.pending(context);
        assertEquals(64, pending.length());
        assertEquals("record-1", pending.getJSONObject(0).getString("commandId"));
        assertEquals("state-2", pending.getJSONObject(63).getString("commandId"));
    }
    @Test public void typedReceiptIsDurableMinimalAndFirstResultIsImmutable() throws Exception {
        JSONObject command=command("typed-1","completeSet").put("sequence",7)
            .put("healthSummary",new JSONObject().put("heartRate",123)).put("reps","10");
        WearCommandQueueStore.enqueue(context,command,"wear:/typed-1");
        assertTrue(WearCommandQueueStore.ack(context,command,"applied"));
        JSONObject receipt=WearCommandQueueStore.receipt(context,"typed-1");
        assertEquals(1,receipt.getInt("schema"));assertEquals(7,receipt.getLong("sequence"));
        assertEquals("session-1",receipt.getString("workoutId"));assertEquals("completeSet",receipt.getString("action"));
        assertEquals("applied",receipt.getString("status"));assertEquals(7,receipt.length());
        assertFalse(receipt.has("healthSummary"));assertFalse(receipt.has("reps"));
        // Re-reading from durable preferences does not require a WebView or Activity.
        assertEquals(receipt.toString(),WearCommandQueueStore.receipt(context,"typed-1").toString());
        assertTrue(WearCommandQueueStore.ack(context,command,"error"));
        assertEquals("applied",WearCommandQueueStore.receipt(context,"typed-1").getString("status"));
        WearCommandQueueStore.enqueue(context,command,"wear:/typed-1");
        assertEquals(0,WearCommandQueueStore.pending(context).length());
    }

    @Test public void legacyDedupeDoesNotFabricatePhoneSuccess() throws Exception {
        JSONObject c=command("legacy-1","completeSet").put("sequence",3);
        assertTrue(WearCommandQueueStore.ack(context,"legacy-1"));
        assertTrue(WearCommandQueueStore.ack(context,c,"applied"));
        assertTrue(WearCommandQueueStore.seen(context,"legacy-1"));
        assertNull(WearCommandQueueStore.receipt(context,"legacy-1"));
    }

    @Test public void failedCommitCannotLeaveAVolatileSuccessReceipt() throws Exception {
        JSONObject c=command("fail-1","completeSet").put("sequence",4);
        WearCommandQueueStore.enqueue(context,c,"wear:/fail-1");
        android.content.SharedPreferences real=context.getSharedPreferences("trainpilot_wear_commands",0);
        android.content.SharedPreferences failing=(android.content.SharedPreferences)java.lang.reflect.Proxy.newProxyInstance(
            getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.class},(proxy,method,args)->{
                if(!method.getName().equals("edit"))return method.invoke(real,args);
                android.content.SharedPreferences.Editor editor=real.edit();
                return java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{android.content.SharedPreferences.Editor.class},
                    (ep,em,ea)->{Object result=em.invoke(editor,ea);if(em.getName().equals("commit"))return false;return result==editor?ep:result;});
            });
        Context wrapper=new android.content.ContextWrapper(context){
            @Override public android.content.SharedPreferences getSharedPreferences(String name,int mode){
                return name.equals("trainpilot_wear_commands")?failing:super.getSharedPreferences(name,mode);
            }
        };
        assertFalse(WearCommandQueueStore.ack(wrapper,c,"applied"));
        assertFalse(WearCommandQueueStore.seen(context,"fail-1"));
        assertNull(WearCommandQueueStore.receipt(context,"fail-1"));
        assertEquals(1,WearCommandQueueStore.pending(context).length());
    }
}
