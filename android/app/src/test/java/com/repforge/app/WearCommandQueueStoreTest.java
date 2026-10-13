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
}
