package com.repforge.app;

import com.repforge.shared.GpsRouteLedger193;
import com.repforge.shared.GpsRoutePacket193;
import com.repforge.shared.GpsRouteSampler193;
import java.io.File;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class GpsRouteLedger193Test {
    private File directory;
    private GpsRouteLedger193 store;
    @Before public void setup() throws Exception { directory=Files.createTempDirectory("trainpilot-route193-").toFile();store=GpsRouteLedger193.open(directory); }
    @After public void cleanup() throws Exception { try(var paths=Files.walk(directory.toPath())) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception ignored){}}); } }
    private JSONObject grant(long epoch) throws Exception {
        return new JSONObject().put("routeKey",GpsRoutePacket193.key("workout193","set193")).put("workoutId","workout193").put("setId","set193")
            .put("exerciseId","run").put("sourceNode","watch193").put("targetNode","phone193").put("ownerEpoch193",epoch);
    }
    private JSONObject point(int ordinal) throws Exception {
        return new JSONObject().put("latitude",47d+ordinal*.00005).put("longitude",19).put("accuracyMeters",5)
            .put("elapsedMillis",ordinal*4000L).put("epochMillis",1800000000000L+ordinal*4000L).put("segment",1);
    }
    private JSONObject packet(int sequence) throws Exception {
        JSONObject p=grant(1);p.put("schema",1).put("streamId","stream193").put("sequence",sequence).put("closed",false).put("partial",false)
            .put("points",new JSONArray().put(point(sequence)));return p;
    }
    private interface Work { void run() throws Exception; }
    private void rejects(Work work) throws Exception { try { work.run();fail("Expected rejection"); }catch(IllegalArgumentException|IllegalStateException expected){} }
    @Test public void immutableChunkIsStoredBeforeReceiptAndReplayDoesNotDuplicatePoints() throws Exception {
        JSONObject p=packet(1);assertNull(store.accept(p,"watch193","phone193"));store.authorize(grant(1));
        JSONObject receipt=store.accept(p,"watch193","phone193");assertEquals("stored",receipt.getString("status"));
        assertEquals(GpsRouteLedger193.fingerprint(p),receipt.getString("hash"));assertFalse(receipt.has("points"));assertFalse(receipt.has("latitude"));
        assertEquals("duplicate",store.accept(new JSONObject(p.toString()),"watch193","phone193").getString("status"));
        assertEquals(1,store.packets(p.getString("routeKey")).length());
        JSONObject changed=packet(1);changed.getJSONArray("points").getJSONObject(0).put("latitude",48);
        rejects(()->store.accept(changed,"watch193","phone193"));assertEquals(1,store.packets(p.getString("routeKey")).length());
    }
    @Test public void sourceEpochPeerAndStableSetMustMatchLocalConsent() throws Exception {
        store.authorize(grant(1));JSONObject p=packet(1);
        rejects(()->store.accept(p,"otherWatch","phone193"));rejects(()->store.accept(p,"watch193","otherPhone"));
        JSONObject old=packet(1).put("ownerEpoch193",0);assertNull(store.accept(old,"watch193","phone193"));
        JSONObject wrong=packet(1).put("setId","another");rejects(()->store.accept(wrong,"watch193","phone193"));
        rejects(()->store.authorize(grant(1).put("sourceNode","otherWatch")));
        assertEquals(0,store.packets(p.getString("routeKey")).length());
    }
    @Test public void outOfOrderChunksAndClosedTailAreAcceptedWithoutAllowingDataAfterClose() throws Exception {
        store.authorize(grant(1));JSONObject tail=packet(4).put("closed",true).put("points",new JSONArray());
        assertNotNull(store.accept(tail,"watch193","phone193"));assertNotNull(store.accept(packet(3),"watch193","phone193"));
        assertNotNull(store.accept(packet(1),"watch193","phone193"));assertNotNull(store.accept(packet(2),"watch193","phone193"));
        rejects(()->store.accept(packet(5),"watch193","phone193"));
        rejects(()->store.accept(packet(2).put("closed",true),"watch193","phone193"));
        assertEquals(4,store.packets(tail.getString("routeKey")).length());
    }
    @Test public void overlappingPointsAndUnmarkedJumpAcrossChunkBoundaryAreRejected() throws Exception {
        store.authorize(grant(1));store.accept(packet(1),"watch193","phone193");
        JSONObject overlap=packet(2).put("points",new JSONArray().put(point(1)));rejects(()->store.accept(overlap,"watch193","phone193"));
        JSONObject jump=packet(2);jump.getJSONArray("points").getJSONObject(0).put("latitude",48);rejects(()->store.accept(jump,"watch193","phone193"));
        jump.getJSONArray("points").getJSONObject(0).put("segment",2);assertNotNull(store.accept(jump,"watch193","phone193"));
    }
    @Test public void deletionTombstoneSurvivesReopenAndLateEpochsCannotResurrectCoordinates() throws Exception {
        JSONObject p=packet(1);store.authorize(grant(1));store.authorize(grant(3));store.accept(p,"watch193","phone193");
        String key=p.getString("routeKey");store.delete(key);store=GpsRouteLedger193.open(directory);
        assertEquals(0,store.packets(key).length());assertFalse(store.authorize(grant(5)));
        assertEquals("deleted",store.accept(p,"watch193","phone193").getString("status"));
        assertEquals("deleted",store.accept(packet(2).put("ownerEpoch193",3),"watch193","phone193").getString("status"));
        assertEquals(0,store.packets(key).length());
    }
    @Test public void wireLimitsAndNonNumericCoordinatesAreRejectedAndUnknownFieldsAreDropped() throws Exception {
        JSONObject p=packet(1),g=grant(1);p.put("unrelatedHealth",new JSONObject().put("secret",true));
        p.getJSONArray("points").getJSONObject(0).put("extra",42);
        JSONObject clean=GpsRoutePacket193.validate(p,g,"watch193","phone193");assertFalse(clean.has("unrelatedHealth"));assertFalse(clean.getJSONArray("points").getJSONObject(0).has("extra"));
        p.getJSONArray("points").getJSONObject(0).put("latitude","47.1");rejects(()->GpsRoutePacket193.validate(p,g,"watch193","phone193"));
        JSONObject many=packet(1);for(int i=2;i<=33;i++)many.getJSONArray("points").put(point(i));rejects(()->GpsRoutePacket193.validate(many,g,"watch193","phone193"));
        rejects(()->GpsRoutePacket193.validate(packet(1).put("sequence",1.5),g,"watch193","phone193"));
        JSONObject poor=packet(1);poor.getJSONArray("points").getJSONObject(0).put("accuracyMeters",36);rejects(()->GpsRoutePacket193.validate(poor,g,"watch193","phone193"));
    }
    @Test public void failedDurableStorageDoesNotProduceAReceiptOrForgetConsent() throws Exception {
        JSONObject p=packet(1);store.authorize(grant(1));
        File route=new File(directory,"wear_routes193/"+GpsRoutePacket193.digest(p.getString("routeKey"))+".json");assertTrue(route.mkdir());
        Files.write(new File(route,"blocker").toPath(),"occupied".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try { store.accept(p,"watch193","phone193");fail("No receipt without durable chunk"); }catch(java.io.IOException expected){}
        Files.delete(new File(route,"blocker").toPath());Files.delete(route.toPath());
        assertEquals("stored",store.accept(p,"watch193","phone193").getString("status"));
    }
    @Test public void samplerBreaksBadSignalJumpsAndExplicitPausesWithoutCountingTheGap() throws Exception {
        JSONArray rows=new JSONArray();GpsRouteSampler193 sampler=new GpsRouteSampler193(rows::put);
        assertTrue(sampler.add(47,19,5,1000,1800000001000L,2));assertTrue(sampler.add(47.0001,19,5,9000,1800000009000L,2));double measured=sampler.distanceMeters();assertTrue(measured>10&&measured<12);
        assertFalse(sampler.add(48,19,5,10000,1800000010000L,2));assertTrue(sampler.add(48,19,5,14000,1800000014000L,2));assertEquals(measured,sampler.distanceMeters(),.001);
        assertFalse(sampler.add(48.0001,19,50,18000,1800000018000L,2));assertTrue(sampler.add(48.0002,19,5,22000,1800000022000L,2));assertEquals(measured,sampler.distanceMeters(),.001);
        sampler.breakSegment();assertTrue(sampler.add(49,19,5,26000,1800000026000L,2));assertEquals(4,sampler.segmentCount());assertEquals(measured,sampler.distanceMeters(),.001);
        assertTrue(sampler.add(50,19,5,60000,1800000060000L,2));assertEquals(5,sampler.segmentCount());assertEquals(measured,sampler.distanceMeters(),.001);
        assertFalse(sampler.add(50,19,5,60000,1800000060000L,2));assertEquals(6,rows.length());
    }
    @Test public void samplerStorageFailureKeepsCountersUncommittedAndSegmentBudgetIsBounded() throws Exception {
        GpsRouteSampler193 failed=new GpsRouteSampler193(p->{throw new java.io.IOException("full");});
        try { failed.add(47,19,5,1000,1800000001000L,2);fail("Write must fail"); }catch(java.io.IOException expected){}
        assertTrue(failed.partial());assertEquals(0,failed.pointCount());assertEquals(0,failed.distanceMeters(),0);
        GpsRouteSampler193 sampler=new GpsRouteSampler193(p->{});
        for(int i=1;i<=256;i++){sampler.breakSegment();assertTrue(sampler.add(47,19,5,i*4000L,1800000000000L+i*4000L,2));}
        sampler.breakSegment();assertFalse(sampler.add(47,19,5,257*4000L,1800000000000L+257*4000L,2));assertTrue(sampler.partial());assertEquals(256,sampler.pointCount());
    }
    @Test public void mapViewOrdersChunksAndNeverConnectsMissingDataOrDifferentEpochs() throws Exception {
        store.authorize(grant(1));String key=packet(1).getString("routeKey");
        store.accept(packet(3),"watch193","phone193");store.accept(packet(1),"watch193","phone193");
        JSONObject broken=store.view(key);assertTrue(broken.getBoolean("partial"));assertEquals(2,broken.getJSONArray("segments").length());assertEquals(0,broken.getDouble("distanceMeters"),0);
        store.accept(packet(2),"watch193","phone193");store.accept(packet(4).put("closed",true).put("points",new JSONArray()),"watch193","phone193");
        JSONObject joined=store.view(key);assertFalse(joined.getBoolean("partial"));assertEquals(1,joined.getJSONArray("segments").length());assertEquals(3,joined.getInt("pointCount"));
        assertTrue(joined.getDouble("distanceMeters")>10);assertTrue(joined.getDouble("distanceMeters")<12);
        store.authorize(grant(3));JSONObject another=packet(1).put("ownerEpoch193",3).put("streamId","newStream193").put("closed",true);
        another.getJSONArray("points").getJSONObject(0).put("latitude",48).put("epochMillis",1800000200000L);
        store.accept(another,"watch193","phone193");JSONObject combined=store.view(key);
        assertEquals(2,combined.getJSONArray("segments").length());assertEquals(joined.getDouble("distanceMeters"),combined.getDouble("distanceMeters"),0);
        assertEquals(3,combined.getJSONArray("segments").getJSONObject(1).getInt("ownerEpoch193"));
    }
    @Test public void coordinateFreeBindingsAreScopedToOneWorkoutAndDisappearOnDeletion() throws Exception {
        JSONObject p=packet(1);store.authorize(grant(1));assertEquals(0,store.bindings("workout193").length());store.accept(p,"watch193","phone193");
        JSONArray bindings=store.bindings("workout193");assertEquals(1,bindings.length());assertEquals("set193",bindings.getJSONObject(0).getString("setId"));
        assertFalse(bindings.toString().contains("latitude"));assertFalse(bindings.toString().contains("points"));assertEquals(0,store.bindings("otherWorkout").length());
        store.delete(p.getString("routeKey"));assertEquals(0,store.bindings("workout193").length());assertEquals(0,store.view(p.getString("routeKey")).getInt("pointCount"));
    }
    @Test public void rememberedNativeStartProofNeedsTheSameWatchAndCannotReviveADeletedRoute() throws Exception {
        JSONObject pending=grant(0);pending.remove("targetNode");store.rememberSource(pending);JSONObject p=packet(1).put("ownerEpoch193",0);
        rejects(()->store.accept(p,"otherWatch","phone193"));assertEquals(0,store.packets(p.getString("routeKey")).length());
        assertEquals("stored",store.accept(p,"watch193","phone193").getString("status"));assertTrue(store.rememberSource(pending));
        store.delete(p.getString("routeKey"));assertFalse(store.rememberSource(pending));assertEquals("deleted",store.accept(p,"watch193","phone193").getString("status"));
    }
    @Test public void discardedWorkoutRejectsTheFirstLateChunkEvenWhenNoRouteWasKnownAtDeletion() throws Exception {
        store.deleteWorkout("workout193");assertFalse(store.authorize(grant(1)));assertFalse(store.rememberSource(grant(0)));
        JSONObject p=packet(1);assertEquals("deleted",store.accept(p,"watch193","phone193").getString("status"));assertEquals(0,store.packets(p.getString("routeKey")).length());
        rejects(()->store.accept(p,"otherWatch","phone193"));
    }
    @Test public void deletingAWorkoutErasesItsKnownRoutesAndPreservesAnotherWorkout() throws Exception {
        store.authorize(grant(1));JSONObject p=packet(1);store.accept(p,"watch193","phone193");
        JSONObject other=grant(1).put("workoutId","other").put("routeKey",GpsRoutePacket193.key("other","set193"));store.authorize(other);
        JSONObject q=packet(1).put("workoutId","other").put("routeKey",other.getString("routeKey"));store.accept(q,"watch193","phone193");
        store.deleteWorkout("workout193");assertEquals(0,store.bindings("workout193").length());assertEquals(1,store.view(q.getString("routeKey")).getInt("pointCount"));
        assertEquals("deleted",store.accept(p,"watch193","phone193").getString("status"));
    }
}
