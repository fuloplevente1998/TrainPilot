package com.repforge.app;

import android.content.Context;
import com.repforge.shared.OwnershipJournal193;
import com.repforge.shared.WorkoutOwnership193;
import java.nio.file.Files;
import java.io.File;
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
public class PhoneOwnership193Test {
    private Context context;
    private JSONObject original;
    private OwnershipJournal193 journal;
    @Before public void open()throws Exception {
        context=RuntimeEnvironment.getApplication();
        original=new JSONObject("{\"active\":true,\"workoutId\":\"w193\",\"timerOwner\":\"phone\",\"ownerEpoch193\":0,\"workoutPaused\":true,\"exercises\":[]}");
        ActiveWorkoutStore.saveOwned(context,original);journal=PhoneOwnership193.journal(context);
    }
    private JSONObject offer()throws Exception {
        long now=System.currentTimeMillis();
        JSONObject request=new JSONObject().put("protocol",1).put("kind","request").put("id","transfer-193")
                .put("workoutId","w193").put("epoch",0).put("sender","watch-a").put("target","phone-a").put("createdAt",now);
        journal.receive(request,"watch-a","phone-a",now);journal.prepare(original,"phone-a",now);
        return journal.read().getJSONObject("outgoing");
    }
    private JSONObject command(long epoch)throws Exception {return new JSONObject().put("workoutId","w193").put("action","completeSet").put("ownerEpoch193",epoch);}
    @Test public void frozenPhoneCannotPublishOrRestartAndHealthSummaryStillArrives()throws Exception {
        offer();
        assertFalse(PhoneOwnership193.phoneAllowed(context,"w193"));
        assertFalse(PhoneOwnership193.publicationAllowed(context,original));
        assertFalse(PhoneOwnership193.commandAllowed(context,command(0)));
        assertTrue(PhoneOwnership193.commandAllowed(context,command(0).put("action","healthSummary")));
        assertTrue(PhoneOwnership193.phoneAllowed(context,"another-workout"));
    }
    @Test public void committedWatchRejectsOldEpochAndExplicitClosureCannotBeRepublished()throws Exception {
        JSONObject envelope=offer(),offered=envelope.getJSONObject("snapshot");
        JSONObject accepted=WorkoutOwnership193.accept(original,offered,"watch-a","phone-a");
        envelope.put("kind","accepted").put("sender","watch-a").put("target","phone-a").put("snapshot",accepted);
        journal.receive(envelope,"watch-a","phone-a",System.currentTimeMillis());
        assertFalse(PhoneOwnership193.commandAllowed(context,command(0)));
        assertTrue(PhoneOwnership193.commandAllowed(context,command(1)));
        assertFalse(PhoneOwnership193.publicationAllowed(context,original));
        JSONObject current=ActiveWorkoutStore.current(context);
        assertEquals("watch",current.getString("timerOwner"));assertTrue(PhoneOwnership193.publicationAllowed(context,current));
        JSONObject closed=ActiveWorkoutStore.clear(context);assertEquals(1,closed.getLong("ownerEpoch193"));
        assertEquals("w193",closed.getString("workoutId"));assertFalse(PhoneOwnership193.publicationAllowed(context,current));
    }
    @Test public void unreadableJournalIsRetryableAndNotAcknowledgedAsStale()throws Exception {
        File file=new File(context.getNoBackupFilesDir(),"ownership193.json");Files.writeString(file.toPath(),"{");
        assertFalse(PhoneOwnership193.phoneAllowed(context,"w193"));
        try {PhoneOwnership193.commandAllowed(context,command(0));fail("Corrupt storage was treated as a stale command");}
        catch(org.json.JSONException expected) {}
    }
    @Test public void pluginWorkerClosureWaitingForJournalDoesNotLockOutSnapshotReaders()throws Exception {
        java.util.concurrent.ExecutorService probe=java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        Thread worker=new Thread(()->{try{ActiveWorkoutStore.clear(context);}catch(Throwable failure){error.set(failure);}});
        worker.setDaemon(true);
        try {
            synchronized(journal) {
                worker.start();long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
                while(worker.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.yield();
                assertEquals(Thread.State.BLOCKED,worker.getState());
                assertNotNull(probe.submit(()->ActiveWorkoutStore.current(context)).get(2,java.util.concurrent.TimeUnit.SECONDS));
            }
        } finally {worker.join(2000);probe.shutdownNow();}
        assertFalse(worker.isAlive());assertNull(error.get());
    }
}
