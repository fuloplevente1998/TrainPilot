package com.repforge.shared;

import android.content.Context;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** One outstanding transfer, with the exact retry envelope committed before transport. */
public final class OwnershipJournal193 {
    public interface Host {
        JSONObject current() throws Exception;
        void install(JSONObject value) throws Exception;
    }
    private final AtomicFile file;
    private final Host host;
    public OwnershipJournal193(Context context, Host host) {
        file=new AtomicFile(new File(context.getNoBackupFilesDir(),"ownership193.json"));this.host=host;
    }
    public synchronized JSONObject read() throws Exception {
        if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return new JSONObject();
        if(file.getBaseFile().length()>512000||new File(file.getBaseFile()+".bak").length()>512000)
            throw new IllegalStateException("Ownership journal exceeds storage budget");
        byte[] bytes=file.readFully();
        if(bytes.length>512000)throw new IllegalStateException("Ownership journal exceeds storage budget");
        return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
    }
    private void write(JSONObject value) throws Exception {
        byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>512000)throw new IllegalStateException("Ownership journal exceeds storage budget");
        FileOutputStream output=null;
        try { output=file.startWrite();output.write(bytes);output.getFD().sync();file.finishWrite(output); }
        catch(Exception error) { if(output!=null)file.failWrite(output);throw error; }
    }
    private static void require(boolean value) { if(!value)throw new IllegalStateException("Ownership transfer changed"); }
    private static boolean matching(JSONObject a,JSONObject b) {
        return a!=null&&b!=null&&a.optString("id").equals(b.optString("id"))
                && a.optString("workoutId").equals(b.optString("workoutId"))&&a.optLong("epoch")==b.optLong("epoch");
    }
    private static JSONObject envelope(String kind,JSONObject request,JSONObject snapshot,String sender,String target) throws Exception {
        JSONObject value=WorkoutOwnership193.copy(request);
        value.put("protocol",1).put("kind",kind).put("sender",sender).put("target",target);
        value.remove("snapshot");
        if(snapshot!=null)value.put("snapshot",snapshot);
        require(value.toString().getBytes(StandardCharsets.UTF_8).length<=96000);
        return value;
    }
    public synchronized JSONObject request(String localNode,String peerNode,long now) throws Exception {
        JSONObject current=host.current();require(current!=null&&current.optBoolean("active",true));
        JSONObject state=read(),previous=state.optJSONObject("request");
        if(WorkoutOwnership193.pending(current))return state;
        require(!localNode.equals(peerNode));
        JSONObject request=new JSONObject().put("protocol",1).put("kind","request")
                .put("id",matchingWorkout(previous,current)?previous.getString("id"):java.util.UUID.randomUUID().toString())
                .put("workoutId",current.getString("workoutId")).put("epoch",WorkoutOwnership193.epoch(current))
                .put("sender",localNode).put("target",peerNode).put("createdAt",now);
        state=new JSONObject().put("request",request).put("outgoing",request).put("stage","requested");
        write(state);return state;
    }
    private static boolean matchingWorkout(JSONObject request,JSONObject snapshot) {
        return request!=null&&request.optString("workoutId").equals(snapshot.optString("workoutId"))
                &&request.optLong("epoch")==WorkoutOwnership193.epoch(snapshot);
    }
    /** Called only after the source actually stopped its timers/GPS and saved all measured values. */
    public synchronized JSONObject prepare(JSONObject frozen,String localNode,long now) throws Exception {
        JSONObject state=read(),request=state.optJSONObject("request");
        require(request!=null&&localNode.equals(request.optString("target"))&&matchingWorkout(request,frozen)
                &&now-request.optLong("createdAt")<=30000&&now>=request.optLong("createdAt")-5000
                &&frozen.optBoolean("workoutPaused"));
        JSONObject offered=WorkoutOwnership193.offer(frozen,frozen.optString("timerOwner","phone"),localNode,
                request.getString("sender"),request.getString("id"),request.getLong("epoch"),now);
        state.put("snapshot",offered).put("stage","offered")
                .put("outgoing",envelope("offered",request,offered,localNode,request.getString("sender")));
        write(state);host.install(offered);return state;
    }
    /** Messages are bound to the DataItem's actual origin node, not an unauthenticated payload field. */
    public synchronized JSONObject receive(JSONObject message,String origin,String localNode,long now) throws Exception {
        require(message.optInt("protocol")==1&&origin.equals(message.optString("sender"))
                &&localNode.equals(message.optString("target"))&&!origin.equals(localNode)
                &&message.optString("id").matches("[A-Za-z0-9._:-]{1,128}"));
        JSONObject state=read(),request=state.optJSONObject("request"),current=host.current();
        require(current!=null&&current.optBoolean("active",true)&&current.optString("workoutId").equals(message.optString("workoutId")));
        String kind=message.optString("kind");
        if("request".equals(kind)) {
            require(matchingWorkout(message,current)&&now-message.optLong("createdAt")<=30000&&now>=message.optLong("createdAt")-5000);
            if(WorkoutOwnership193.pending(current)) {
                require(matching(request,message));return state; // Replay the already-frozen offer.
            }
            require(!origin.equals(localNode));
            state=new JSONObject().put("request",message).put("stage","incoming");write(state);return state;
        }
        require(matching(request,message));
        JSONObject next,remote=message.optJSONObject("snapshot");
        if("offered".equals(kind)) {
            require(localNode.equals(request.getString("sender"))&&origin.equals(request.getString("target"))
                    &&now-request.getLong("createdAt")<=30000);
            // A delayed offer after installation must not roll back edits or owner epoch.
            if(WorkoutOwnership193.epoch(current)>request.getLong("epoch"))return state;
            next=WorkoutOwnership193.accept(current,remote,localNode,origin);
            state.put("stage","accepted").put("snapshot",next)
                    .put("outgoing",envelope("accepted",request,next,localNode,origin));
        } else if("accepted".equals(kind)) {
            require(localNode.equals(request.getString("target"))&&origin.equals(request.getString("sender")));
            next=WorkoutOwnership193.commit(state.getJSONObject("snapshot"),remote,localNode,origin);
            state.put("stage","committed").put("snapshot",next)
                    .put("outgoing",envelope("committed",request,next,localNode,origin));
        } else if("committed".equals(kind)) {
            require(localNode.equals(request.getString("sender"))&&origin.equals(request.getString("target")));
            next=WorkoutOwnership193.finish(current,remote,localNode,origin);
            state.put("stage","installed").put("snapshot",next)
                    .put("outgoing",envelope("installed",request,null,localNode,origin));
        } else if("installed".equals(kind)) {
            require("committed".equals(state.optString("stage"))&&localNode.equals(request.getString("target"))
                    &&origin.equals(request.getString("sender")));
            state.remove("outgoing");state.put("stage","settled");write(state);return state;
        } else throw new IllegalArgumentException("Unsupported ownership message");
        write(state);
        // Replayed accepts resend the immutable commit, but cannot roll the source's
        // subsequently refreshed canonical snapshot back to the frozen offer.
        if(!"accepted".equals(kind)||WorkoutOwnership193.epoch(current)<WorkoutOwnership193.epoch(next)
                ||WorkoutOwnership193.pending(current))host.install(next);
        return state;
    }
    public synchronized void installedReceiptPublished(String id) throws Exception {
        JSONObject state=read(),outgoing=state.optJSONObject("outgoing");
        if(outgoing!=null&&"installed".equals(outgoing.optString("kind"))&&id.equals(outgoing.optString("id"))) {
            state.remove("outgoing");write(state);
        }
    }
    /** Recover the crash window after journal commit and before the host's snapshot write. */
    public synchronized void recover() throws Exception {
        JSONObject saved=read().optJSONObject("snapshot"),current=host.current();
        if(saved==null||!saved.optBoolean("active",true))return;
        if(current==null){host.install(saved);return;}
        if(!saved.optString("workoutId").equals(current.optString("workoutId")))return;
        if(WorkoutOwnership193.epoch(current)>WorkoutOwnership193.epoch(saved))return;
        JSONObject sh=WorkoutOwnership193.transfer(saved),ch=WorkoutOwnership193.transfer(current);
        if(WorkoutOwnership193.epoch(current)<WorkoutOwnership193.epoch(saved)
                || sh!=null&&(ch==null||!sh.optString("id").equals(ch.optString("id"))
                ||"accepted".equals(sh.optString("state"))&&"offered".equals(ch.optString("state"))))host.install(saved);
    }
    public synchronized void close(String workoutId) throws Exception {
        JSONObject state=read(),saved=state.optJSONObject("snapshot"),request=state.optJSONObject("request");
        if(saved!=null&&workoutId.equals(saved.optString("workoutId")))saved.put("active",false);
        if(saved!=null&&workoutId.equals(saved.optString("workoutId"))||request!=null&&workoutId.equals(request.optString("workoutId"))) {
            state.remove("outgoing");state.remove("request");state.put("stage","closed");write(state);
        }
    }
}
