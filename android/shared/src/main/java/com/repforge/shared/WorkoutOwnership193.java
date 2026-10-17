package com.repforge.shared;

import org.json.JSONArray;
import org.json.JSONObject;

/** Pure, fenced ownership transitions. Persist each result before sending it to the peer. */
public final class WorkoutOwnership193 {
    public static final String PATH = "/trainpilot/ownership193/";
    public static final int PROTOCOL = 1;
    private WorkoutOwnership193() {}

    public static JSONObject copy(JSONObject value) throws Exception { return new JSONObject(value.toString()); }
    public static long epoch(JSONObject value) { return value == null ? 0 : value.optLong("ownerEpoch193"); }
    public static JSONObject transfer(JSONObject value) { return value == null ? null : value.optJSONObject("handoff193"); }
    public static boolean pending(JSONObject value) {
        JSONObject h = transfer(value);
        return h != null && !"committed".equals(h.optString("state"));
    }
    public static boolean owns(JSONObject value, String device) {
        return value != null && value.optBoolean("active",true) && !pending(value)
                && device.equals(value.optString("timerOwner","phone"));
    }
    private static void require(boolean value) { if (!value) throw new IllegalStateException("Ownership changed; retry the same transfer."); }
    private static boolean id(String value) { return value != null && value.matches("[A-Za-z0-9._:-]{1,128}"); }
    private static boolean same(JSONObject a, JSONObject b) {
        return a != null && b != null && a.optInt("protocol")==PROTOCOL && b.optInt("protocol")==PROTOCOL
                && a.optLong("epoch") == b.optLong("epoch")
                && a.optString("id").equals(b.optString("id")) && a.optString("source").equals(b.optString("source"))
                && a.optString("target").equals(b.optString("target")) && a.optString("sourceNode").equals(b.optString("sourceNode"))
                && a.optString("targetNode").equals(b.optString("targetNode"));
    }
    public static JSONObject offer(JSONObject current, String source, String sourceNode, String targetNode,
                                   String transferId, long expectedEpoch, long now) throws Exception {
        require(("phone".equals(source)||"watch".equals(source)) && owns(current,source)
                && !current.optString("workoutId").isEmpty() && current.optString("workoutId").length()<=512
                && epoch(current)==expectedEpoch && expectedEpoch>=0 && id(sourceNode) && id(targetNode)
                && !sourceNode.equals(targetNode) && id(transferId) && now>0 && expectedEpoch<Long.MAX_VALUE
                && current.optLong("pauseRevision")>=0 && current.optLong("pauseRevision")<Long.MAX_VALUE
                && (transfer(current)==null || !transferId.equals(transfer(current).optString("id"))));
        String target = "phone".equals(source) ? "watch" : "phone";
        JSONObject result=copy(current);
        result.put("ownershipProtocol193",PROTOCOL).put("ownerEpoch193",expectedEpoch)
                .put("workoutPaused",true).put("pauseRevision",current.optLong("pauseRevision")+1)
                .put("pausedRestMillis",Math.max(current.optLong("pausedRestMillis"),Math.max(0,current.optLong("restEndAt")-now)))
                .put("restEndAt",0);
        JSONArray intervals=result.optJSONArray("activeIntervals");
        if(intervals!=null)for(int i=0;i<intervals.length();i++) {
            JSONObject interval=intervals.optJSONObject(i);
            if(interval!=null && (interval.isNull("end") || interval.optString("end").isEmpty()))
                interval.put("end",java.time.Instant.ofEpochMilli(now).toString());
        }
        JSONObject automatic=result.optJSONObject("automation");
        if(automatic!=null) {
            long remaining=automatic.optLong("remainingMs");
            if(!automatic.optBoolean("paused") && automatic.optLong("deadlineAt")>0)
                remaining=Math.max(0,automatic.optLong("deadlineAt")-now);
            automatic.put("paused",true).put("deadlineAt",0).put("remainingMs",remaining);
        }
        result.put("handoff193",new JSONObject().put("protocol",PROTOCOL).put("id",transferId)
                .put("source",source).put("target",target).put("sourceNode",sourceNode).put("targetNode",targetNode)
                .put("epoch",expectedEpoch+1).put("state","offered").put("createdAt",now));
        return result;
    }
    /** The target persists this paused snapshot before acknowledging the offer. */
    public static JSONObject accept(JSONObject current, JSONObject offer, String localNode, String peerNode) throws Exception {
        JSONObject h=transfer(offer);
        require(h!=null && h.optInt("protocol")==PROTOCOL && "offered".equals(h.optString("state"))
                && localNode.equals(h.optString("targetNode")) && peerNode.equals(h.optString("sourceNode"))
                && id(h.optString("id")) && offer.optBoolean("active",true) && offer.optBoolean("workoutPaused")
                && !offer.optString("workoutId").isEmpty() && offer.optString("workoutId").length()<=512
                && epoch(offer)>=0 && h.optLong("epoch")>0 && h.optLong("epoch")==epoch(offer)+1
                && ("phone".equals(h.optString("source"))||"watch".equals(h.optString("source")))
                && h.optString("source").equals(offer.optString("timerOwner"))
                && ("phone".equals(h.optString("source"))?"watch":"phone").equals(h.optString("target"))
                && id(h.optString("sourceNode")) && id(h.optString("targetNode"))
                && !localNode.equals(peerNode));
        if(current!=null) {
            require(current.optString("workoutId").equals(offer.optString("workoutId")));
            if(same(transfer(current),h) && "accepted".equals(transfer(current).optString("state")))return copy(current);
            require(!pending(current) && epoch(current)<h.optLong("epoch")
                    && current.optString("timerOwner","phone").equals(h.optString("source")));
        }
        JSONObject result=copy(offer);
        transfer(result).put("state","accepted");
        return result;
    }
    /** Only the frozen source can commit, and duplicate accepts replay the same commit. */
    public static JSONObject commit(JSONObject frozen, JSONObject accepted, String localNode, String peerNode) throws Exception {
        JSONObject h=transfer(frozen),a=transfer(accepted);
        require(same(h,a) && frozen.optString("workoutId").equals(accepted.optString("workoutId"))
                && "accepted".equals(a.optString("state")) && localNode.equals(h.optString("sourceNode"))
                && peerNode.equals(h.optString("targetNode")) && h.optInt("protocol")==PROTOCOL && h.optLong("epoch")>0);
        if("committed".equals(h.optString("state"))) {
            require(epoch(frozen)==h.optLong("epoch") && h.optString("target").equals(frozen.optString("timerOwner")));
            return copy(frozen);
        }
        require("offered".equals(h.optString("state")) && frozen.optBoolean("workoutPaused")
                && epoch(frozen)==h.optLong("epoch")-1 && h.optString("source").equals(frozen.optString("timerOwner")));
        JSONObject result=copy(frozen);
        result.put("ownerEpoch193",h.getLong("epoch")).put("timerOwner",h.getString("target"));
        result.remove("nativeExecutionProtocol");result.remove("generation");result.remove("executionRevision");
        JSONObject automatic=result.optJSONObject("automation");
        if(automatic!=null)automatic.put("owner",h.getString("target"));
        transfer(result).put("state","committed");
        return result;
    }
    /** A commit is usable only by the target that already saved this exact offer. */
    public static JSONObject finish(JSONObject accepted, JSONObject committed, String localNode, String peerNode) throws Exception {
        JSONObject h=transfer(accepted),c=transfer(committed);
        require(same(h,c) && accepted.optString("workoutId").equals(committed.optString("workoutId"))
                && ("accepted".equals(h.optString("state")) || "committed".equals(h.optString("state")))
                && "committed".equals(c.optString("state")) && localNode.equals(c.optString("targetNode"))
                && peerNode.equals(c.optString("sourceNode")) && epoch(committed)==c.optLong("epoch")
                && epoch(accepted)==c.optLong("epoch")-("accepted".equals(h.optString("state"))?1:0)
                && c.optString("target").equals(committed.optString("timerOwner"))
                && committed.optBoolean("active",true) && committed.optBoolean("workoutPaused"));
        // A duplicate commit must never erase the target's subsequent offline edits.
        if("committed".equals(h.optString("state")))return copy(accepted);
        return copy(committed);
    }
    public static boolean stale(JSONObject current, JSONObject incoming) {
        if(current==null)return false;
        if(!current.optString("workoutId").equals(incoming.optString("workoutId")))
            return current.optBoolean("active",true) && (pending(current)||epoch(current)>0);
        return epoch(incoming)<epoch(current)
                || epoch(incoming)==epoch(current) && !current.optString("timerOwner","phone").equals(incoming.optString("timerOwner","phone"))
                || pending(current) && !same(transfer(current),transfer(incoming));
    }
}
