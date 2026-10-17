package com.repforge.app;

import android.content.Context;
import android.content.ContextWrapper;
import com.repforge.shared.OwnershipJournal193;
import com.repforge.shared.WorkoutOwnership193;
import java.io.File;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=33,manifest=Config.NONE)
public class OwnershipJournal193Test {
    private final long now=1_800_000_000_000L;
    private Host phone,watch;
    private OwnershipJournal193 source,target;
    private Context phoneContext,watchContext;
    private static class Host implements OwnershipJournal193.Host {
        JSONObject value;boolean fail;
        Host(JSONObject value){this.value=value;}
        public JSONObject current(){return value;}
        public void install(JSONObject next)throws Exception{if(fail)throw new IllegalStateException("disk");value=WorkoutOwnership193.copy(next);}
    }
    private Context context()throws Exception {
        File dir=Files.createTempDirectory("ownership193-").toFile();
        return new ContextWrapper(RuntimeEnvironment.getApplication()) { @Override public File getNoBackupFilesDir(){return dir;} };
    }
    @Before public void open()throws Exception {
        JSONObject value=new JSONObject("{\"active\":true,\"workoutId\":\"workout:193\",\"timerOwner\":\"phone\",\"ownerEpoch193\":0,\"workoutPaused\":false,\"exercises\":[{\"id\":\"plank\",\"sets\":[{\"setId\":\"s1\",\"reps\":\"10\",\"done\":false}]}]}");
        phone=new Host(value);watch=new Host(WorkoutOwnership193.copy(value));
        phoneContext=context();watchContext=context();
        source=new OwnershipJournal193(phoneContext,phone);target=new OwnershipJournal193(watchContext,watch);
    }
    private JSONObject outgoing(OwnershipJournal193 journal)throws Exception {return WorkoutOwnership193.copy(journal.read().getJSONObject("outgoing"));}
    private void offer()throws Exception {
        target.request("watch-a","phone-a",now);
        source.receive(outgoing(target),"watch-a","phone-a",now);
        phone.value.put("workoutPaused",true);
        source.prepare(phone.value,"phone-a",now);
    }
    private JSONObject accept()throws Exception {offer();target.receive(outgoing(source),"phone-a","watch-a",now);return outgoing(target);}
    private JSONObject commit()throws Exception {source.receive(accept(),"watch-a","phone-a",now);return outgoing(source);}
    private void reject(Throwing action)throws Exception {try {action.run();fail("Unsafe transition accepted");}catch(IllegalStateException expected){}}
    private interface Throwing {void run()throws Exception;}

    @Test public void twoDurableJournalsTransferOnlyAfterCommitAndReceipt()throws Exception {
        JSONObject commit=commit();
        assertTrue(WorkoutOwnership193.pending(watch.value));assertFalse(WorkoutOwnership193.owns(watch.value,"watch"));
        target.receive(commit,"phone-a","watch-a",now);
        assertTrue(WorkoutOwnership193.owns(watch.value,"watch"));assertTrue(watch.value.getBoolean("workoutPaused"));
        JSONObject receipt=outgoing(target);source.receive(receipt,"watch-a","phone-a",now);
        assertEquals("settled",source.read().getString("stage"));assertFalse(source.read().has("outgoing"));
        target.installedReceiptPublished(receipt.getString("id"));assertFalse(target.read().has("outgoing"));
        assertEquals(1,watch.value.getLong("ownerEpoch193"));
    }
    @Test public void crashAfterJournalBeforeSnapshotInstallRecoversBeforeAcknowledgement()throws Exception {
        offer();JSONObject offered=outgoing(source);watch.fail=true;
        reject(()->target.receive(offered,"phone-a","watch-a",now));
        assertEquals("accepted",target.read().getString("stage"));assertFalse(WorkoutOwnership193.pending(watch.value));
        watch.fail=false;target=new OwnershipJournal193(watchContext,watch);target.recover();
        assertTrue(WorkoutOwnership193.pending(watch.value));assertEquals("accepted",outgoing(target).getString("kind"));
    }
    @Test public void lostAcceptAndCommitReplayPreservesBothDevicesLaterValues()throws Exception {
        JSONObject accepted=accept();source.receive(accepted,"watch-a","phone-a",now);JSONObject committed=outgoing(source);
        target.receive(committed,"phone-a","watch-a",now);
        watch.value.put("workoutPaused",false);
        watch.value.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","25");
        phone.value.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).put("reps","25");
        source.receive(accepted,"watch-a","phone-a",now+1000);
        assertEquals("25",phone.value.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
        assertEquals(committed.toString(),outgoing(source).toString());
        target.receive(outgoing(source),"phone-a","watch-a",now+1000);
        assertEquals("25",watch.value.getJSONArray("exercises").getJSONObject(0).getJSONArray("sets").getJSONObject(0).getString("reps"));
        assertFalse(watch.value.getBoolean("workoutPaused"));
    }
    @Test public void durableConsentRecoversAMissingSnapshotButClosedJournalNeverDoes()throws Exception {
        offer();target.receive(outgoing(source),"phone-a","watch-a",now);
        watch.value=null;target=new OwnershipJournal193(watchContext,watch);target.recover();
        assertNotNull(watch.value);assertTrue(WorkoutOwnership193.pending(watch.value));
        target.close("workout:193");watch.value=null;target.recover();assertNull(watch.value);
    }
    @Test public void mismatchedNodesStaleRequestsAndUnrequestedOffersCannotChangeState()throws Exception {
        target.request("watch-a","phone-a",now);JSONObject request=outgoing(target);
        reject(()->source.receive(request,"different-node","phone-a",now));
        reject(()->source.receive(request,"watch-a","phone-a",now+31000));
        assertEquals("{}",source.read().toString());
        offer();JSONObject offer=outgoing(source);offer.put("id","unrequested");
        reject(()->target.receive(offer,"phone-a","watch-a",now));
        assertEquals("requested",target.read().getString("stage"));
    }
    @Test public void expiredOfferCanRetrySameIdWithoutUnfreezingSource()throws Exception {
        offer();JSONObject oldRequest=outgoing(target),offered=outgoing(source);
        reject(()->target.receive(offered,"phone-a","watch-a",now+31000));
        target.request("watch-a","phone-a",now+32000);
        assertEquals(oldRequest.getString("id"),outgoing(target).getString("id"));
        source.receive(outgoing(target),"watch-a","phone-a",now+32000);
        assertTrue(phone.value.getBoolean("workoutPaused"));
        target.receive(outgoing(source),"phone-a","watch-a",now+32000);
        assertEquals("accepted",target.read().getString("stage"));
    }
    @Test public void explicitCloseStopsReplayAndLateMessagesCannotReviveWorkout()throws Exception {
        JSONObject accepted=accept();source.close("workout:193");phone.value.put("active",false);
        reject(()->source.receive(accepted,"watch-a","phone-a",now));
        assertFalse(source.read().has("outgoing"));assertEquals("closed",source.read().getString("stage"));
    }
    @Test public void reverseHandoffIncrementsEpochAndStaysPaused()throws Exception {
        target.receive(commit(),"phone-a","watch-a",now);source.receive(outgoing(target),"watch-a","phone-a",now);
        source.request("phone-a","watch-a",now+1000);
        target.receive(outgoing(source),"phone-a","watch-a",now+1000);
        watch.value.put("workoutPaused",true);target.prepare(watch.value,"watch-a",now+1000);
        source.receive(outgoing(target),"watch-a","phone-a",now+1000);
        target.receive(outgoing(source),"phone-a","watch-a",now+1000);
        source.receive(outgoing(target),"watch-a","phone-a",now+1000);
        assertTrue(WorkoutOwnership193.owns(phone.value,"phone"));assertEquals(2,phone.value.getLong("ownerEpoch193"));
        assertTrue(phone.value.getBoolean("workoutPaused"));
    }
}
