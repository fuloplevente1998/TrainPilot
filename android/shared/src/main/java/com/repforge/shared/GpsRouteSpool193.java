package com.repforge.shared;

import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;

/** Watch-side coordinates stay in no-backup files; the existing command outbox holds pointers only. */
public final class GpsRouteSpool193 {
    public interface Queue { boolean enqueue(JSONObject pointer) throws Exception; }
    private static final java.util.Map<String,GpsRouteSpool193> instances=new java.util.HashMap<>();
    private static final int MAX_PACKETS=512, USAGE_BYTES=1024*1024;
    private final File directory;
    private GpsRouteSpool193(File noBackupDirectory) { directory=new File(noBackupDirectory,"wear_gps_spool193"); }
    public static synchronized GpsRouteSpool193 open(File directory) throws Exception {
        String path=directory.getCanonicalPath();if(!instances.containsKey(path))instances.put(path,new GpsRouteSpool193(new File(path)));return instances.get(path);
    }
    private static void require(boolean value) { if(!value)throw new IllegalStateException("Route recording storage unavailable or full"); }
    private AtomicFile activeFile() { return new AtomicFile(new File(directory,"active.json")); }
    private AtomicFile usageFile() { return new AtomicFile(new File(directory,"usage.json")); }
    private AtomicFile packetFile(String id) { require(id!=null&&id.matches("[a-f0-9]{64}"));return new AtomicFile(new File(directory,id+".packet")); }
    private AtomicFile revokedFile(String key) throws Exception { require(key.matches("wear193:[a-f0-9]{64}"));return new AtomicFile(new File(directory,GpsRoutePacket193.digest(key)+".revoked")); }
    public boolean revoked(String key) throws Exception { AtomicFile file=revokedFile(key);return file.getBaseFile().exists()||new File(file.getBaseFile()+".bak").exists(); }
    private JSONObject read(AtomicFile file,int budget) throws Exception {
        if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return new JSONObject();
        require(file.getBaseFile().length()<=budget&&new File(file.getBaseFile()+".bak").length()<=budget);
        byte[] bytes=file.readFully();require(bytes.length<=budget);return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
    }
    private void write(AtomicFile file,JSONObject value,int budget) throws Exception {
        byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);require(bytes.length<=budget);require(directory.isDirectory()||directory.mkdirs());FileOutputStream out=null;
        try { out=file.startWrite();out.write(bytes);out.getFD().sync();file.finishWrite(out);
            if(!file.getBaseFile().isFile()||!java.util.Arrays.equals(bytes,file.readFully()))throw new java.io.IOException("Route write was not durable");
        }catch(Exception error) { if(out!=null)file.failWrite(out);throw error; }
    }
    public synchronized JSONObject active() throws Exception { return read(activeFile(),GpsRoutePacket193.MAX_PACKET_BYTES+2048); }
    private JSONObject usage() throws Exception { return read(usageFile(),USAGE_BYTES); }
    private static String commandId(JSONObject packet) throws Exception {
        return GpsRoutePacket193.digest(new JSONArray().put(packet.getString("routeKey")).put(packet.getString("streamId")).put(packet.getLong("sequence")).toString());
    }
    private static JSONObject packet(JSONObject active,boolean closed,boolean partial) throws Exception {
        JSONObject packet=new JSONObject(active.getJSONObject("meta").toString());
        packet.put("schema",1).put("streamId",active.getString("streamId")).put("sequence",active.getLong("sequence"))
            .put("closed",closed).put("partial",partial).put("points",new JSONArray(active.getJSONArray("points").toString()));
        return GpsRoutePacket193.validate(packet,active.getJSONObject("meta"),packet.getString("sourceNode"),packet.getString("targetNode"));
    }
    public static JSONObject pointer(JSONObject packet) throws Exception {
        return new JSONObject().put("schema",1).put("action","gpsRouteChunk193").put("commandId",commandId(packet))
            .put("workoutId",packet.getString("workoutId")).put("ownerEpoch193",packet.getLong("ownerEpoch193"))
            .put("createdAt",packet.getJSONArray("points").length()>0?packet.getJSONArray("points").getJSONObject(0).getLong("epochMillis"):System.currentTimeMillis())
            .put("route193",new JSONObject().put("routeKey",packet.getString("routeKey")).put("streamId",packet.getString("streamId"))
                .put("sequence",packet.getLong("sequence")).put("sourceNode",packet.getString("sourceNode")).put("targetNode",packet.getString("targetNode"))
                .put("hash",GpsRouteLedger193.fingerprint(packet)));
    }
    /** Begin only on an explicit permitted foreground action; a previous stream must first be sealed. */
    public synchronized void begin(JSONObject metadata,Queue queue) throws Exception {
        if(active().has("meta"))close(true,queue);
        JSONObject usage=usage();String key=metadata.getString("routeKey");JSONObject route=usage.optJSONObject(key);
        require(!revoked(key)&&(route==null||!route.optBoolean("deleted")));if(route==null)require(usage.length()<GpsRouteLedger193.MAX_ROUTES);
        int points=0,segments=0;JSONObject streams=route==null?new JSONObject():route.optJSONObject("streams");
        if(streams==null)streams=new JSONObject();require(streams.length()<GpsRoutePacket193.MAX_SEGMENTS);
        for(java.util.Iterator<String> keys=streams.keys();keys.hasNext();) { JSONObject prior=streams.getJSONObject(keys.next());points+=prior.getInt("points");segments+=prior.getInt("segments"); }
        require(points<GpsRoutePacket193.MAX_POINTS&&segments<GpsRoutePacket193.MAX_SEGMENTS);
        JSONObject next=new JSONObject().put("meta",new JSONObject(metadata.toString())).put("streamId",java.util.UUID.randomUUID().toString())
            .put("sequence",1).put("points",new JSONArray()).put("accepted",0).put("segments",0).put("basePoints",points).put("baseSegments",segments);
        packet(next,true,false);write(activeFile(),next,GpsRoutePacket193.MAX_PACKET_BYTES+2048);
    }
    /** Called by the real-fix sampler; the point is durable before its measured counters advance. */
    public synchronized void append(JSONObject point,Queue queue) throws Exception {
        JSONObject current=active();require(current.has("meta")&&!current.optBoolean("closing"));
        if(current.getJSONArray("points").length()>=GpsRoutePacket193.CHUNK_POINTS) { flushOpen(queue);current=active(); }
        require(current.getInt("basePoints")+current.getInt("accepted")<GpsRoutePacket193.MAX_POINTS
            &&current.getInt("baseSegments")+point.getInt("segment")<=GpsRoutePacket193.MAX_SEGMENTS);
        current.getJSONArray("points").put(new JSONObject(point.toString()));current.put("accepted",current.getInt("accepted")+1).put("segments",point.getInt("segment"));
        packet(current,false,false);write(activeFile(),current,GpsRoutePacket193.MAX_PACKET_BYTES+2048);
        if(current.getJSONArray("points").length()==GpsRoutePacket193.CHUNK_POINTS)flushOpen(queue);
    }
    private void enqueue(JSONObject packet,Queue queue) throws Exception {
        String id=commandId(packet);AtomicFile file=packetFile(id);JSONObject previous=read(file,GpsRoutePacket193.MAX_PACKET_BYTES+2048);
        if(previous.has("packet"))require(GpsRouteLedger193.fingerprint(previous.getJSONObject("packet")).equals(GpsRouteLedger193.fingerprint(packet)));
        else {
            File[] files=directory.listFiles((dir,name)->name.endsWith(".packet")||name.endsWith(".packet.bak"));require(files!=null&&files.length<MAX_PACKETS);
            write(file,new JSONObject().put("packet",packet).put("pointer",pointer(packet)),GpsRoutePacket193.MAX_PACKET_BYTES+2048);
        }
        require(queue.enqueue(read(file,GpsRoutePacket193.MAX_PACKET_BYTES+2048).getJSONObject("pointer")));
    }
    private void flushOpen(Queue queue) throws Exception {
        JSONObject current=active();require(!current.optBoolean("closing")&&current.getJSONArray("points").length()==GpsRoutePacket193.CHUNK_POINTS);
        enqueue(packet(current,false,false),queue);current.put("points",new JSONArray()).put("sequence",current.getLong("sequence")+1);
        write(activeFile(),current,GpsRoutePacket193.MAX_PACKET_BYTES+2048);
    }
    /** Closing intent, packet, outbox pointer, usage counters, then active clear: every crash is retryable. */
    public synchronized void close(boolean partial,Queue queue) throws Exception {
        JSONObject current=active();if(!current.has("meta"))return;
        if(current.getInt("accepted")==0) { write(activeFile(),new JSONObject(),GpsRoutePacket193.MAX_PACKET_BYTES+2048);return; }
        if(current.getJSONArray("points").length()==GpsRoutePacket193.CHUNK_POINTS) { flushOpen(queue);current=active(); }
        if(!current.optBoolean("closing")) { current.put("closing",true).put("partial",partial);write(activeFile(),current,GpsRoutePacket193.MAX_PACKET_BYTES+2048); }
        enqueue(packet(current,true,current.getBoolean("partial")),queue);
        JSONObject usage=usage();String key=current.getJSONObject("meta").getString("routeKey");JSONObject route=usage.optJSONObject(key);
        if(route==null) { require(usage.length()<GpsRouteLedger193.MAX_ROUTES);route=new JSONObject().put("streams",new JSONObject());usage.put(key,route); }
        require(!route.optBoolean("deleted"));route.getJSONObject("streams").put(current.getString("streamId"),new JSONObject().put("points",current.getInt("accepted")).put("segments",current.getInt("segments")));
        write(usageFile(),usage,USAGE_BYTES);write(activeFile(),new JSONObject(),GpsRoutePacket193.MAX_PACKET_BYTES+2048);
    }
    public synchronized JSONObject payload(String commandId) throws Exception { return read(packetFile(commandId),GpsRoutePacket193.MAX_PACKET_BYTES+2048).optJSONObject("packet"); }
    /** Immutable file, read after commit; no spool lock under the command-outbox lock. */
    public JSONObject wirePayload(String commandId) throws Exception { JSONObject packet=read(packetFile(commandId),GpsRoutePacket193.MAX_PACKET_BYTES+2048).optJSONObject("packet");return packet!=null&&!revoked(packet.getString("routeKey"))?packet:null; }
    public synchronized boolean ready(JSONObject pointer) throws Exception {
        JSONObject packet=payload(pointer.optString("commandId"));return packet!=null&&GpsRouteLedger193.fingerprint(packet).equals(pointer.getJSONObject("route193").getString("hash"));
    }
    public void recover(Queue queue) throws Exception { recover(queue,true); }
    public synchronized void recover(Queue queue,boolean sealActive) throws Exception {
        File[] files=directory.listFiles((dir,name)->name.endsWith(".packet")||name.endsWith(".packet.bak"));
        if(files!=null)for(File file:files) {
            String id=file.getName().substring(0,64);JSONObject stored=read(packetFile(id),GpsRoutePacket193.MAX_PACKET_BYTES+2048);
            if(revoked(stored.getJSONObject("packet").getString("routeKey"))) { acknowledged(id);continue; }
            require(queue.enqueue(stored.getJSONObject("pointer")));
        }
        JSONObject active=active();if(active.has("meta")&&revoked(active.getJSONObject("meta").getString("routeKey")))write(activeFile(),new JSONObject(),GpsRoutePacket193.MAX_PACKET_BYTES+2048);
        if(sealActive)close(true,queue); // No old monotonic anchor is resumed after process death.
    }
    public synchronized void acknowledged(String id) throws Exception {
        AtomicFile file=packetFile(id);file.delete();require(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists());
    }
    /** Phone deletion is durable before local erasure and also fences concurrent outbox sends. */
    public synchronized void revoke(String key) throws Exception {
        write(revokedFile(key),new JSONObject().put("deleted",true),128);
        JSONObject current=active();if(current.has("meta")&&key.equals(current.getJSONObject("meta").getString("routeKey")))write(activeFile(),new JSONObject(),GpsRoutePacket193.MAX_PACKET_BYTES+2048);
        File[] files=directory.listFiles((dir,name)->name.endsWith(".packet")||name.endsWith(".packet.bak"));
        if(files!=null)for(File file:files) { String id=file.getName().substring(0,64);JSONObject packet=payload(id);if(packet!=null&&key.equals(packet.getString("routeKey")))acknowledged(id); }
        JSONObject usage=usage(),entry=usage.optJSONObject(key);if(entry==null) { require(usage.length()<GpsRouteLedger193.MAX_ROUTES);entry=new JSONObject().put("streams",new JSONObject());usage.put(key,entry); }
        entry.put("deleted",true);write(usageFile(),usage,USAGE_BYTES);
    }
}
