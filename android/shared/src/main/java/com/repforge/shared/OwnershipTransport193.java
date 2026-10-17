package com.repforge.shared;

import android.content.Context;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import org.json.JSONObject;

public final class OwnershipTransport193 {
    private static final java.util.Map<String,Long> attempts=new java.util.HashMap<>();
    public interface Receiver { void receive(JSONObject message,String origin,String localNode) throws Exception; }
    public interface Nodes { void ready(String localNode,String peerNode) throws Exception; }
    private OwnershipTransport193() {}
    public static void nodes(Context context,Nodes callback) {
        Wearable.getNodeClient(context).getLocalNode().addOnSuccessListener(local ->
            Wearable.getNodeClient(context).getConnectedNodes().addOnSuccessListener(peers -> {
                if(peers.size()!=1)return;
                try { callback.ready(local.getId(),peers.get(0).getId()); } catch(Exception ignored) { /* No transfer without durable state. */ }
            }));
    }
    public static void send(Context context,OwnershipJournal193 journal) throws Exception {
        JSONObject outgoing=journal.read().optJSONObject("outgoing");if(outgoing==null)return;
        String id=outgoing.getString("id"),kind=outgoing.getString("kind");
        String attempt=outgoing.getString("sender")+":"+id+":"+kind;
        synchronized(attempts) {
            long now=android.os.SystemClock.elapsedRealtime();
            if(attempts.containsKey(attempt)&&now-attempts.get(attempt)<5000)return;
            if(attempts.size()>64)attempts.clear();attempts.put(attempt,now);
        }
        PutDataMapRequest item=PutDataMapRequest.create(WorkoutOwnership193.PATH+outgoing.getString("sender")+"/"+id);
        item.getDataMap().putString("ownership",outgoing.toString());item.getDataMap().putLong("retryAt",System.currentTimeMillis());
        Wearable.getDataClient(context).putDataItem(item.asPutDataRequest().setUrgent()).addOnSuccessListener(ignored -> {
            if("installed".equals(kind))try {journal.installedReceiptPublished(id);}catch(Exception error){/* Replayable until durable. */}
        });
    }
    public static boolean receive(Context context,DataItem item,Receiver callback) {
        if(item==null||item.getUri()==null||item.getUri().getPath()==null||!item.getUri().getPath().startsWith(WorkoutOwnership193.PATH))return false;
        try {
            final android.net.Uri itemUri=item.getUri(); // DataEvent/GetDataItems buffers are released before async node lookup completes.
            String raw=DataMapItem.fromDataItem(item).getDataMap().getString("ownership");
            if(raw==null||raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>96000)return true;
            JSONObject message=new JSONObject(raw);String origin=itemUri.getHost();
            if(!(WorkoutOwnership193.PATH+origin+"/"+message.optString("id")).equals(itemUri.getPath()))return true;
            Wearable.getNodeClient(context).getLocalNode().addOnSuccessListener(local -> {
                if(local.getId().equals(origin))return;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                    try {
                        callback.receive(message,origin,local.getId());
                        if("installed".equals(message.optString("kind"))) {
                            // The source durably settled before returning. Remove only this acknowledged transfer pair.
                            Wearable.getDataClient(context).deleteDataItems(itemUri);
                            android.net.Uri own=new android.net.Uri.Builder().scheme("wear").authority(local.getId())
                                    .path(WorkoutOwnership193.PATH+local.getId()+"/"+message.getString("id")).build();
                            Wearable.getDataClient(context).deleteDataItems(own);
                        }
                    }catch(Exception ignored){/* Invalid/stale messages keep the existing owner. */}
                });
            });
        }catch(Exception ignored){/* A bad item cannot change ownership. */}
        return true;
    }
}
