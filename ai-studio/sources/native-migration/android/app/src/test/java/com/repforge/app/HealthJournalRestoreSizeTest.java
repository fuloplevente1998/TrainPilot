package com.repforge.app;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import java.util.Arrays;
import static org.junit.Assert.*;

/** Native SQLite/CursorWindow: aggregate snapshots exceed a window, individual records do not. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class HealthJournalRestoreSizeTest {
    private Context context;
    private HealthJournalStore store;
    private final long now=1760000000000L;
    @Before public void setup() {
        context=RuntimeEnvironment.getApplication();context.deleteDatabase("health-journal.db");store=new HealthJournalStore(context);
    }
    @After public void cleanup(){store.close();}
    private void reopen(){store.close();store=new HealthJournalStore(context);}
    private JSONObject snapshot(String prefix)throws Exception {
        char[] chars=new char[768*1024];Arrays.fill(chars,'x');
        // Put supplementary characters across many possible chunk boundaries.
        String payload=new String(chars).replace("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\uD83D\uDE80");
        JSONArray records=new JSONArray();
        for(int i=0;i<4;i++)records.put(new JSONObject().put("channel",HealthJournalStore.HC).put("source","com.samsung.android.app.shealth")
            .put("type","StepsRecord").put("recordId",prefix+i).put("lastModifiedMs",now+i).put("startMs",now+i).put("endMs",now+1000+i)
            .put("day",HealthJournalStore.day(now)).put("data",new JSONObject().put("value",i+1).put("payload",payload)));
        JSONObject result=new JSONObject().put("schemaVersion",1).put("records",records).put("days",new JSONArray()).put("deletions",new JSONArray()).put("syncMeta",new JSONObject());
        assertTrue(result.toString().length()>3*1024*1024);return result;
    }
    private void assertSnapshot(JSONObject expected)throws Exception {
        JSONArray want=expected.getJSONArray("records"),actual=store.exportSnapshot().getJSONArray("records");assertEquals(want.length(),actual.length());
        for(int i=0;i<want.length();i++) {
            assertEquals(want.getJSONObject(i).getString("recordId"),actual.getJSONObject(i).getString("recordId"));
            assertEquals(want.getJSONObject(i).getJSONObject("data").getString("payload"),actual.getJSONObject(i).getJSONObject("data").getString("payload"));
        }
    }
    private long parts(){try(Cursor c=store.getReadableDatabase().rawQuery("SELECT count(*) FROM meta_parts",null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    private void seedLegacyPending(JSONObject old,JSONObject next)throws Exception {
        store.importRecords(old.getJSONArray("records"),store.generation());SQLiteDatabase db=store.getWritableDatabase();
        db.execSQL("DROP TABLE meta_parts");db.setVersion(1);
        db.execSQL("INSERT OR REPLACE INTO meta(key,value) VALUES(?,?)",new Object[]{"restore_old",old.toString()});
        db.execSQL("INSERT OR REPLACE INTO meta(key,value) VALUES(?,?)",new Object[]{"restore_next",next.toString()});
        db.execSQL("INSERT OR REPLACE INTO meta(key,value) VALUES('restore_pending','legacy-token')");
        // Reproduce the reported Android exception with an unbounded old query.
        try(SQLiteCursor cursor=(SQLiteCursor)db.rawQuery("SELECT value FROM meta WHERE key='restore_next'",null)) {
            cursor.setWindow(new CursorWindow("regression",256*1024));
            try{cursor.moveToFirst();fail("Unbounded snapshot must exceed CursorWindow");}catch(SQLiteBlobTooBigException expected){}
        }
        reopen();assertEquals(2,store.getReadableDatabase().getVersion());assertEquals("legacy-token",store.pendingRestore());
    }
    @Test public void largeSnapshotsSurviveRestartRollbackCommitAndChunkCleanup()throws Exception {
        JSONObject old=snapshot("old"),next=snapshot("next");store.importRecords(old.getJSONArray("records"),store.generation());
        String token=store.prepareRestore(next);assertTrue(parts()>100);reopen();assertEquals(token,store.pendingRestore());
        store.installRestore(token);assertSnapshot(next);reopen();store.finishRestore(token,false);assertSnapshot(old);assertEquals(0,parts());assertEquals("",store.pendingRestore());
        token=store.prepareRestore(next);reopen();store.installRestore(token);reopen();store.finishRestore(token,true);assertSnapshot(next);assertEquals(0,parts());
        store.prepareRestore(old);assertTrue(parts()>0);store.erase();assertEquals(0,parts());assertEquals("",store.pendingRestore());
    }
    @Test public void existingOversizedV1PendingSnapshotCanInstallAndCommit()throws Exception {
        JSONObject old=snapshot("old"),next=snapshot("next");seedLegacyPending(old,next);
        store.installRestore("legacy-token");assertSnapshot(next);reopen();store.finishRestore("legacy-token",true);assertEquals("",store.pendingRestore());assertEquals(0,parts());
    }
    @Test public void existingOversizedV1PendingSnapshotCanRecoverByRollback()throws Exception {
        JSONObject old=snapshot("old"),next=snapshot("next");seedLegacyPending(old,next);
        store.finishRestore("legacy-token",false);assertSnapshot(old);assertEquals("",store.pendingRestore());assertEquals(0,parts());
        // Recovery leaves the new store ready to retry the original backup.
        String token=store.prepareRestore(next);store.installRestore(token);store.finishRestore(token,true);assertSnapshot(next);assertEquals(0,parts());
    }
}
