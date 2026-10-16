package com.repforge.app;

import android.content.Context;
import android.net.Uri;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONObject;

/** A receipt is published only after the existing command/dedupe store committed. */
final class WearCommandReceipts {
    static final String PREFIX="/trainpilot/command-receipt/";
    private WearCommandReceipts() {}

    static boolean complete(Context context, JSONObject command, String status, Uri uri) {
        if(!WearCommandQueueStore.ack(context,command,status))return false;
        replay(context,command,uri);
        return true;
    }

    static void replay(Context context, JSONObject command, Uri uri) {
        String id=command.optString("commandId");
        JSONObject receipt=WearCommandQueueStore.receipt(context,id);
        if(receipt==null||receipt.optString("workoutId").isEmpty()||receipt.optLong("sequence")<=0) {
            if(uri!=null)Wearable.getDataClient(context).deleteDataItems(uri);
            return; // Legacy dedupe entries never manufacture a success receipt.
        }
        if("undoSet".equals(command.optString("action"))){
            JSONObject active=ActiveWorkoutStore.current(context);
            if(active!=null&&active.optString("workoutId").equals(receipt.optString("workoutId")))try{receipt.put("undoFrame",active);}catch(Exception ignored){}
        }
        PutDataMapRequest map=PutDataMapRequest.create(PREFIX+id);
        map.getDataMap().putString("receipt",receipt.toString());
        Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
            .addOnSuccessListener(item->{if(uri!=null)Wearable.getDataClient(context).deleteDataItems(uri);});
        // Failed publication keeps the command DataItem for an idempotent retry.
    }
}
