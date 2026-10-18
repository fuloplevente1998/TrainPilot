package com.repforge.app;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import com.repforge.shared.*;
import java.io.File;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class PhoneGpsRoute193Test {
    private Context context;private File directory;
    @Before public void setup() throws Exception {
        directory=Files.createTempDirectory("phone-route193-").toFile();context=new ContextWrapper(RuntimeEnvironment.getApplication()) { @Override public File getNoBackupFilesDir(){return directory;} };
        context.getSharedPreferences("trainpilot_wear_sync",0).edit().clear().commit();context.getSharedPreferences("trainpilot_wear_commands",0).edit().clear().commit();
    }
    @After public void cleanup() throws Exception { try(var paths=Files.walk(directory.toPath())) { paths.sorted(java.util.Comparator.reverseOrder()).forEach(p->{try{Files.delete(p);}catch(Exception ignored){}}); } }
    private JSONObject snapshot() throws Exception { return new JSONObject("{\"schema\":1,\"active\":true,\"workoutId\":\"workout193\",\"ownershipProtocol193\":1,\"ownerEpoch193\":0,\"timerOwner\":\"watch\",\"exercises\":[{\"id\":\"run\",\"measurementType\":\"distance\",\"repUnit\":\"km\",\"sets\":[{\"setId\":\"set193\",\"done\":false}]}]}"); }
    private JSONObject start() throws Exception { return new JSONObject().put("schema",1).put("commandId","start193").put("workoutId","workout193").put("sequence",1).put("action","startQuickWorkout").put("finalSnapshot",snapshot()); }
    private void rememberStart(boolean ack) throws Exception {
        WearCommandQueueStore.enqueue(context,start(),"wear://watch193/trainpilot/workout-command/start193");
        if(ack)WearCommandQueueStore.ack(context,WearCommandQueueStore.pending(context).getJSONObject(0),"applied");
        ActiveWorkoutStore.saveOwned(context,snapshot());
    }
    private JSONObject command() throws Exception {
        JSONObject packet=new JSONObject().put("schema",1).put("routeKey",GpsRoutePacket193.key("workout193","set193")).put("workoutId","workout193").put("exerciseId","run")
            .put("setId","set193").put("sourceNode","watch193").put("targetNode","phone193").put("ownerEpoch193",0).put("streamId","stream193").put("sequence",1).put("closed",true).put("partial",false)
            .put("points",new JSONArray().put(new JSONObject().put("latitude",47).put("longitude",19).put("accuracyMeters",5).put("elapsedMillis",4000).put("epochMillis",1800000004000L).put("segment",1)));
        return GpsRouteSpool193.pointer(packet).put("sequence",2).put("gpsRoutePacket193",packet);
    }
    @Test public void nativeStartOriginSurvivesAckAndIsNotTakenFromTheWebView() throws Exception {
        JSONObject forged=snapshot().put("watchSourceNode193","untrusted");ActiveWorkoutStore.saveOwned(context,forged);assertFalse(ActiveWorkoutStore.current(context).has("watchSourceNode193"));
        rememberStart(true);assertEquals("watch193",ActiveWorkoutStore.current(context).getString("watchSourceNode193"));
        JSONObject next=snapshot().put("watchSourceNode193","untrusted");ActiveWorkoutStore.saveOwned(context,next);assertEquals("watch193",ActiveWorkoutStore.current(context).getString("watchSourceNode193"));
    }
    @Test public void receivedRouteIsPrivateAndReceiptDoesNotContainCoordinates() throws Exception {
        rememberStart(true);JSONObject command=command(),receipt=PhoneGpsRoute193.accept(context,command,"watch193","phone193");assertNotNull(receipt);assertFalse(receipt.has("gpsRoutePacket193"));assertFalse(receipt.toString().contains("latitude"));
        assertTrue(WearCommandQueueStore.ack(context,receipt,"applied"));assertEquals(0,WearCommandQueueStore.pending(context).length());
        assertFalse(context.getSharedPreferences("trainpilot_wear_commands",0).getAll().toString().contains("latitude"));
        assertEquals(1,GpsRouteLedger193.open(directory).view(command.getJSONObject("route193").getString("routeKey")).getInt("pointCount"));
        JSONObject saved=WearCommandQueueStore.receipt(context,receipt.getString("commandId"));assertTrue(saved.has("route193"));assertFalse(saved.toString().contains("latitude"));
    }
    @Test public void genericQueueNeverPersistsCoordinatePacketsEvenWhenTheyHaveTheWrongAction() throws Exception {
        JSONObject command=command();WearCommandQueueStore.enqueue(context,command,"wear://watch193/test");assertEquals(0,WearCommandQueueStore.pending(context).length());
        command.put("action","updateSet");WearCommandQueueStore.enqueue(context,command,"wear://watch193/test");assertEquals(0,WearCommandQueueStore.pending(context).length());
        assertTrue(PhoneGpsRoute193.receive(context,command,Uri.parse("wear://watch193/test")));
    }
    @Test public void nativeConsentIsRequiredAndCommandPointerCannotLieAboutItsPayload() throws Exception {
        JSONObject command=command();assertNull(PhoneGpsRoute193.accept(context,command,"watch193","phone193"));rememberStart(false);
        command.getJSONObject("route193").put("hash","wrong");try{PhoneGpsRoute193.accept(context,command,"watch193","phone193");fail("Forged pointer");}catch(IllegalArgumentException expected){}
        command=command();try{PhoneGpsRoute193.accept(context,command,"otherWatch","phone193");fail("Wrong watch");}catch(IllegalArgumentException expected){}
        command=command().put("ownerEpoch193",.5);try{PhoneGpsRoute193.accept(context,command,"watch193","phone193");fail("Fractional epoch");}catch(IllegalArgumentException expected){}
        assertEquals(0,GpsRouteLedger193.open(directory).bindings("workout193").length());
    }
    @Test public void finishedWorkoutCanReceiveLateAuthorizedCoordinatesWithoutReopeningExecution() throws Exception {
        rememberStart(true);context.getSharedPreferences("trainpilot_wear_sync",0).edit().clear().commit();
        assertNotNull(PhoneGpsRoute193.accept(context,command(),"watch193","phone193"));assertNull(ActiveWorkoutStore.current(context));
        assertEquals(1,GpsRouteLedger193.open(directory).bindings("workout193").length());
    }
    @Test public void nativeOwnershipProofAuthorizesOnlyTheMatchingWatchEpochAndKeepsOldRoutesReadable() throws Exception {
        JSONObject owned=snapshot().put("ownerEpoch193",2).put("handoff193",new JSONObject("{\"protocol\":1,\"id\":\"transfer193\",\"epoch\":2,\"state\":\"committed\",\"source\":\"phone\",\"target\":\"watch\",\"sourceNode\":\"phone193\",\"targetNode\":\"watch193\"}"));
        PhoneGpsRoute193.authorize(context,owned);assertNull(PhoneGpsRoute193.accept(context,command(),"watch193","phone193"));
        JSONObject packet=command().getJSONObject("gpsRoutePacket193").put("ownerEpoch193",2),message=GpsRouteSpool193.pointer(packet).put("sequence",10).put("gpsRoutePacket193",packet);
        assertNotNull(PhoneGpsRoute193.accept(context,message,"watch193","phone193"));
        GpsRouteLedger193.open(directory).delete(packet.getString("routeKey"));JSONObject deleted=PhoneGpsRoute193.accept(context,message,"watch193","phone193");assertTrue(deleted.getBoolean("routeDeleted193"));
    }
    @Test public void confirmedDiscardFencesFutureGpsEvenBeforeTheFirstCoordinateArrives() throws Exception {
        JSONObject discard=start().put("action","discardWorkout");assertTrue(WearCommandQueueStore.ack(context,discard,"discarded"));
        JSONObject dropped=PhoneGpsRoute193.accept(context,command(),"watch193","phone193");assertTrue(dropped.getBoolean("routeDeleted193"));
        assertEquals(0,GpsRouteLedger193.open(directory).bindings("workout193").length());
    }
}
