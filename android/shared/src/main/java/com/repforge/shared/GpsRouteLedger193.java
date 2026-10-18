package com.repforge.shared;

import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private route chunks and consent history. The caller supplies a no-backup directory. */
public final class GpsRouteLedger193 {
    public static final int MAX_ROUTES=512, MAX_GRANTS=128;
    private static final int ROUTE_BYTES=2*1024*1024, INDEX_BYTES=1024*1024;
    private final File directory;
    private static final java.util.Map<String,GpsRouteLedger193> instances=new java.util.HashMap<>();
    public static synchronized GpsRouteLedger193 open(File noBackupDirectory) throws Exception {
        String key=noBackupDirectory.getCanonicalPath();
        if(!instances.containsKey(key))instances.put(key,new GpsRouteLedger193(new File(key)));
        return instances.get(key);
    }
    private GpsRouteLedger193(File noBackupDirectory) { directory=new File(noBackupDirectory,"wear_routes193"); }
    private static void require(boolean value) { if(!value)throw new IllegalStateException("Route storage changed or budget reached"); }
    private AtomicFile index() { return new AtomicFile(new File(directory,"index.json")); }
    private AtomicFile route(String key) throws Exception { return new AtomicFile(new File(directory,GpsRoutePacket193.digest(key)+".json")); }
    private JSONObject read(AtomicFile file,int budget) throws Exception {
        if(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists())return new JSONObject();
        require(file.getBaseFile().length()<=budget&&new File(file.getBaseFile()+".bak").length()<=budget);
        byte[] bytes=file.readFully();require(bytes.length<=budget);return new JSONObject(new String(bytes,StandardCharsets.UTF_8));
    }
    private void write(AtomicFile file,JSONObject value,int budget) throws Exception {
        byte[] bytes=value.toString().getBytes(StandardCharsets.UTF_8);require(bytes.length<=budget);
        require(directory.isDirectory()||directory.mkdirs());FileOutputStream out=null;
        try {
            out=file.startWrite();out.write(bytes);out.getFD().sync();file.finishWrite(out);
            if(!file.getBaseFile().isFile()||!java.util.Arrays.equals(bytes,file.readFully()))throw new java.io.IOException("Route commit was not durable");
        }
        catch(Exception error) { if(out!=null)file.failWrite(out);throw error; }
    }
    private JSONObject routes(JSONObject index) throws Exception {
        if(!index.has("routes"))index.put("routes",new JSONObject());return index.getJSONObject("routes");
    }
    /** Only the native ownership adapter may add grants; transport input cannot authorize itself. */
    public synchronized boolean authorize(JSONObject grant) throws Exception {
        String key=grant.getString("routeKey"),workout=grant.getString("workoutId"),set=grant.getString("setId");
        require(key.equals(GpsRoutePacket193.key(workout,set))&&GpsRoutePacket193.id(grant.getString("sourceNode"))
            &&GpsRoutePacket193.id(grant.getString("targetNode"))&&!grant.getString("sourceNode").equals(grant.getString("targetNode"))
            &&grant.getString("exerciseId").length()>0&&grant.getString("exerciseId").length()<=200&&grant.getLong("ownerEpoch193")>=0);
        JSONObject index=read(index(),INDEX_BYTES),routes=routes(index),entry=routes.optJSONObject(key);
        if(entry!=null&&entry.optBoolean("deleted"))return false;
        if(entry==null) { require(routes.length()<MAX_ROUTES);entry=new JSONObject().put("grants",new JSONArray());routes.put(key,entry); }
        JSONArray grants=entry.getJSONArray("grants");
        for(int i=0;i<grants.length();i++) {
            JSONObject old=grants.getJSONObject(i);
            if(old.getLong("ownerEpoch193")==grant.getLong("ownerEpoch193")) {
                for(String field:new String[]{"routeKey","workoutId","exerciseId","setId","sourceNode","targetNode"})require(old.getString(field).equals(grant.getString(field)));
                return true;
            }
        }
        require(grants.length()<MAX_GRANTS);JSONObject clean=new JSONObject();
        for(String field:new String[]{"routeKey","workoutId","exerciseId","setId","sourceNode","targetNode","ownerEpoch193"})clean.put(field,grant.get(field));
        grants.put(clean);write(index(),index,INDEX_BYTES);return true;
    }
    public static String fingerprint(JSONObject packet) throws Exception {
        JSONArray canonical=new JSONArray();
        for(String field:new String[]{"schema","routeKey","workoutId","exerciseId","setId","sourceNode","targetNode","ownerEpoch193","streamId","sequence","closed","partial"})canonical.put(packet.get(field));
        JSONArray points=new JSONArray(),raw=packet.getJSONArray("points");
        for(int i=0;i<raw.length();i++) {
            JSONArray row=new JSONArray();for(String field:new String[]{"latitude","longitude","accuracyMeters","elapsedMillis","epochMillis","segment"})row.put(raw.getJSONObject(i).get(field));points.put(row);
        }
        return GpsRoutePacket193.digest(canonical.put(points).toString());
    }
    /** Returns null without consent. A receipt is produced only after durable storage (or a tombstone). */
    public synchronized JSONObject accept(JSONObject input,String origin,String localNode) throws Exception {
        JSONObject entry=routes(read(index(),INDEX_BYTES)).optJSONObject(input.optString("routeKey"));if(entry==null)return null;
        JSONObject grant=null;JSONArray grants=entry.optJSONArray("grants");
        if(grants!=null)for(int i=0;i<grants.length();i++)if(grants.getJSONObject(i).optLong("ownerEpoch193",-1)==input.optLong("ownerEpoch193",-2))grant=grants.getJSONObject(i);
        if(grant==null)return null;
        JSONObject packet=GpsRoutePacket193.validate(input,grant,origin,localNode);
        String hash=fingerprint(packet),id=packet.getString("streamId")+":"+packet.getLong("sequence");
        String status=entry.optBoolean("deleted")?"deleted":"stored";
        if(!entry.optBoolean("deleted")) {
            AtomicFile file=route(packet.getString("routeKey"));JSONObject track=read(file,ROUTE_BYTES);
            if(!track.has("chunks"))track.put("chunks",new JSONObject());JSONObject chunks=track.getJSONObject("chunks"),old=chunks.optJSONObject(id);
            if(old!=null) { require(hash.equals(old.getString("hash")));status="duplicate"; }
            else {
                int points=packet.getJSONArray("points").length();
                require(chunks.length()<GpsRoutePacket193.MAX_POINTS+GpsRoutePacket193.MAX_SEGMENTS);
                java.util.Set<String> streams=new java.util.HashSet<>(),segments=new java.util.HashSet<>();
                for(int i=0;i<packet.getJSONArray("points").length();i++)segments.add(packet.getString("streamId")+":"+packet.getJSONArray("points").getJSONObject(i).getInt("segment"));
                for(Iterator<String> keys=chunks.keys();keys.hasNext();) {
                    JSONObject previous=chunks.getJSONObject(keys.next()).getJSONObject("packet");streams.add(previous.getString("streamId"));points+=previous.getJSONArray("points").length();
                    for(int i=0;i<previous.getJSONArray("points").length();i++)segments.add(previous.getString("streamId")+":"+previous.getJSONArray("points").getJSONObject(i).getInt("segment"));
                    if(previous.getString("streamId").equals(packet.getString("streamId"))) {
                        require(previous.getLong("ownerEpoch193")==packet.getLong("ownerEpoch193")&&previous.getString("sourceNode").equals(origin));
                        require(!previous.getBoolean("closed")||previous.getLong("sequence")>packet.getLong("sequence"));
                        require(!packet.getBoolean("closed")||packet.getLong("sequence")>previous.getLong("sequence"));
                        JSONObject earlier=previous.getLong("sequence")<packet.getLong("sequence")?previous:packet,later=earlier==previous?packet:previous;
                        JSONArray a=earlier.getJSONArray("points"),b=later.getJSONArray("points");
                        if(a.length()>0&&b.length()>0) {
                            JSONObject last=a.getJSONObject(a.length()-1),first=b.getJSONObject(0);
                            long gap=first.getLong("elapsedMillis")-last.getLong("elapsedMillis");
                            require(gap>0&&first.getLong("epochMillis")>last.getLong("epochMillis")&&first.getInt("segment")>=last.getInt("segment"));
                            if(later.getLong("sequence")==earlier.getLong("sequence")+1&&first.getInt("segment")==last.getInt("segment"))
                                require(gap<=30000&&GpsRoutePacket193.distance(last.getDouble("latitude"),last.getDouble("longitude"),first.getDouble("latitude"),first.getDouble("longitude"))/(gap/1000d)<=20);
                        }
                    }
                }
                streams.add(packet.getString("streamId"));require(streams.size()<=GpsRoutePacket193.MAX_SEGMENTS&&segments.size()<=GpsRoutePacket193.MAX_SEGMENTS&&points<=GpsRoutePacket193.MAX_POINTS);
                chunks.put(id,new JSONObject().put("hash",hash).put("packet",packet));write(file,track,ROUTE_BYTES);
            }
        }
        return new JSONObject().put("schema",1).put("routeKey",packet.getString("routeKey")).put("streamId",packet.getString("streamId"))
            .put("sequence",packet.getLong("sequence")).put("ownerEpoch193",packet.getLong("ownerEpoch193"))
            .put("sourceNode",localNode).put("targetNode",origin).put("hash",hash).put("status",status);
    }
    /** Coordinate-bearing data is read only by the explicit map bridge or private route transport. */
    public synchronized JSONArray packets(String key) throws Exception {
        JSONObject entry=routes(read(index(),INDEX_BYTES)).optJSONObject(key);
        if(entry==null||entry.optBoolean("deleted"))return new JSONArray();
        JSONObject chunks=read(route(key),ROUTE_BYTES).optJSONObject("chunks");JSONArray result=new JSONArray();
        if(chunks!=null)for(Iterator<String> keys=chunks.keys();keys.hasNext();)result.put(chunks.getJSONObject(keys.next()).getJSONObject("packet"));return result;
    }
    /** Tombstone is durable before erasure. Late or repeated packets cannot recreate the track. */
    public synchronized void delete(String key) throws Exception {
        require(key!=null&&key.matches("wear193:[a-f0-9]{64}"));
        JSONObject index=read(index(),INDEX_BYTES),routes=routes(index),entry=routes.optJSONObject(key);
        if(entry==null) { require(routes.length()<MAX_ROUTES);entry=new JSONObject().put("grants",new JSONArray());routes.put(key,entry); }
        entry.put("deleted",true);write(index(),index,INDEX_BYTES);AtomicFile file=route(key);file.delete();
        require(!file.getBaseFile().exists()&&!new File(file.getBaseFile()+".bak").exists());
    }
}
