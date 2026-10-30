package com.repforge.app;

import com.repforge.shared.GpsRoutePacket193;
import com.repforge.shared.GpsRouteLedger193;
import com.repforge.shared.GpsRouteSpool193;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class GpsRouteSpool193Test {
    private File directory;private GpsRouteSpool193 spool;private boolean available;
    private final LinkedHashMap<String,JSONObject> queued=new LinkedHashMap<>();
    private final GpsRouteSpool193.Queue queue=p->{if(!available)return false;queued.put(p.getString("commandId"),new JSONObject(p.toString()));return true;};
    @Before public void setup() throws Exception { directory=Files.createTempDirectory("trainpilot-spool193-").toFile();spool=GpsRouteSpool193.open(directory);available=true; }
    @After public void cleanup() throws Exception { try(var paths=Files.walk(directory.toPath())) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception ignored){}}); } }
    private JSONObject meta() throws Exception {
        return new JSONObject().put("routeKey",GpsRoutePacket193.key("workout193","set193")).put("workoutId","workout193").put("setId","set193")
            .put("exerciseId","run").put("sourceNode","watch193").put("targetNode","phone193").put("ownerEpoch193",1);
    }
    private JSONObject point(int ordinal) throws Exception {
        return new JSONObject().put("latitude",47d+ordinal*.00005).put("longitude",19).put("accuracyMeters",5)
            .put("elapsedMillis",ordinal*4000L).put("epochMillis",1800000000000L+ordinal*4000L).put("segment",1);
    }
    @Test public void partialChunkIsDurableButCoordinatesNeverEnterTheCommandPointer() throws Exception {
        spool.begin(meta(),queue);spool.append(point(1),queue);spool.append(point(2),queue);assertEquals(0,queued.size());
        assertEquals(2,GpsRouteSpool193.open(directory).active().getJSONArray("points").length());spool.close(false,queue);
        assertEquals(1,queued.size());JSONObject pointer=queued.values().iterator().next();assertFalse(pointer.toString().contains("latitude"));assertFalse(pointer.toString().contains("points"));
        assertTrue(spool.ready(pointer));JSONObject packet=spool.payload(pointer.getString("commandId"));assertEquals(2,packet.getJSONArray("points").length());
        assertTrue(packet.getBoolean("closed"));assertFalse(packet.getBoolean("partial"));assertEquals(GpsRouteLedger193.fingerprint(packet),pointer.getJSONObject("route193").getString("hash"));assertFalse(spool.active().has("meta"));
    }
    @Test public void fullChunksAndFinalTailHaveIndependentImmutableIdentities() throws Exception {
        spool.begin(meta(),queue);for(int i=1;i<=35;i++)spool.append(point(i),queue);assertEquals(1,queued.size());assertEquals(3,spool.active().getJSONArray("points").length());spool.close(false,queue);
        assertEquals(2,queued.size());int ordinal=0;
        for(JSONObject pointer:queued.values()) { JSONObject packet=spool.payload(pointer.getString("commandId"));ordinal++;assertEquals(ordinal,packet.getInt("sequence"));assertEquals(ordinal==2,packet.getBoolean("closed"));assertEquals(ordinal==1?32:3,packet.getJSONArray("points").length()); }
    }
    @Test public void failedOutboxWriteKeepsClosingIntentAndRetryUsesTheExactPacket() throws Exception {
        spool.begin(meta(),queue);spool.append(point(1),queue);available=false;
        try{spool.close(false,queue);fail("Queue commit must be required");}catch(IllegalStateException expected){}
        assertTrue(spool.active().getBoolean("closing"));assertFalse(spool.active().getBoolean("partial"));String stream=spool.active().getString("streamId");
        available=true;spool.recover(queue);assertEquals(1,queued.size());JSONObject p=spool.payload(queued.keySet().iterator().next());assertEquals(stream,p.getString("streamId"));assertFalse(p.getBoolean("partial"));assertTrue(p.getBoolean("closed"));assertFalse(spool.active().has("meta"));
    }
    @Test public void crashAfterFullChunkCommitCannotTurnItIntoAConflictingTerminalPacket() throws Exception {
        spool.begin(meta(),queue);for(int i=1;i<32;i++)spool.append(point(i),queue);available=false;
        try{spool.append(point(32),queue);fail("Queue is unavailable");}catch(IllegalStateException expected){}
        assertEquals(32,spool.active().getJSONArray("points").length());available=true;spool.recover(queue);assertEquals(2,queued.size());
        for(JSONObject pointer:queued.values()) { JSONObject p=spool.payload(pointer.getString("commandId"));if(p.getInt("sequence")==1){assertFalse(p.getBoolean("closed"));assertEquals(32,p.getJSONArray("points").length());}else {assertTrue(p.getBoolean("closed"));assertTrue(p.getBoolean("partial"));assertEquals(0,p.getJSONArray("points").length());} }
        assertFalse(spool.active().has("meta"));
    }
    @Test public void emptyFixlessRecordingDoesNotCreateAnArtificialRouteOrConsumeAnOutboxSlot() throws Exception {
        spool.begin(meta(),queue);spool.close(true,queue);assertEquals(0,queued.size());assertFalse(spool.active().has("meta"));
    }
    @Test public void acknowledgedCoordinatesAreErasedAndTheNextRunUsesANewMonotonicStream() throws Exception {
        spool.begin(meta(),queue);spool.append(point(1),queue);spool.close(false,queue);String id=queued.keySet().iterator().next(),stream=spool.payload(id).getString("streamId");
        spool.acknowledged(id);assertNull(spool.payload(id));assertFalse(spool.ready(queued.get(id)));
        spool.begin(meta(),queue);assertNotEquals(stream,spool.active().getString("streamId"));assertEquals(1,spool.active().getInt("basePoints"));assertEquals(1,spool.active().getInt("baseSegments"));
        spool.append(point(1),queue);spool.close(false,queue); // A fresh stream may have a fresh elapsed-time anchor.
    }
    @Test public void newWorkoutCannotReplaceAnUncommittedClosingSource() throws Exception {
        spool.begin(meta(),queue);spool.append(point(1),queue);available=false;
        try{spool.begin(meta().put("workoutId","other"),queue);fail("Do not replace the source");}catch(IllegalStateException expected){}
        assertEquals("workout193",spool.active().getJSONObject("meta").getString("workoutId"));assertEquals(1,spool.active().getInt("accepted"));
    }
}
