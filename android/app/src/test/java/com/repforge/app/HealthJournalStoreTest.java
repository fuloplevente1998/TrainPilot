package com.repforge.app;

import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Executes real SQLite migration, transaction, source selection and recovery paths. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE,sdk=28)
public class HealthJournalStoreTest {
    private HealthJournalStore store;
    private final long now=1760000000000L;
    @Before public void setup(){Context context=RuntimeEnvironment.getApplication();context.deleteDatabase("health-journal.db");store=new HealthJournalStore(context);}
    @After public void cleanup(){store.close();}
    private JSONObject record(String id,long modified,long count)throws Exception {
        return new JSONObject().put("channel",HealthJournalStore.HC).put("source","com.samsung.android.app.shealth").put("type","StepsRecord").put("recordId",id).put("lastModifiedMs",modified).put("startMs",now).put("endMs",now+1000).put("day",HealthJournalStore.day(now)).put("data",new JSONObject().put("value",count).put("unit","steps"));
    }
    private JSONObject page()throws Exception{return store.page("2020-01-01","2030-01-01","","",0,0,50);}
    @Test public void migrationIsOnceAndSurvivesRestart()throws Exception {
        String day=HealthJournalStore.day(now);JSONObject legacy=new JSONObject().put("version",1).put("days",new JSONObject().put(day,new JSONObject().put("steps",0)));
        store.initialize(legacy);store.initialize(new JSONObject().put("days",new JSONObject().put(day,new JSONObject().put("steps",900))));
        assertEquals(0,store.projection().getJSONObject("days").getJSONObject(day).getLong("steps"));
        store.close();store=new HealthJournalStore(RuntimeEnvironment.getApplication());assertEquals(0,store.projection().getJSONObject("days").getJSONObject(day).getLong("steps"));
    }
    @Test public void failedMigrationAndBatchAreAtomic()throws Exception {
        JSONObject days=new JSONObject().put("2025-10-09",new JSONObject().put("steps",4)).put("2025-02-30",new JSONObject());
        try{store.initialize(new JSONObject().put("days",days));fail();}catch(IllegalArgumentException expected){}
        assertEquals(0,store.projection().getJSONObject("days").length());
        store.initialize(new JSONObject().put("days",new JSONObject().put("2025-10-09",new JSONObject().put("steps",3))));
        try{store.importRecords(new JSONArray().put(record("one",now,3)).put(record("bad",now,3).put("endMs",now-1)),store.generation());fail();}catch(IllegalArgumentException expected){}
        assertEquals(0,page().getJSONArray("records").length());
    }
    @Test public void upsertDeletionAndOlderReplayDoNotDuplicate()throws Exception {
        long epoch=store.generation();store.importRecords(new JSONArray().put(record("one",now,12)),epoch);store.importRecords(new JSONArray().put(record("one",now+2,15)),epoch);store.importRecords(new JSONArray().put(record("one",now+1,13)),epoch);
        assertEquals(1,page().getJSONArray("records").length());assertEquals(15,page().getJSONArray("records").getJSONObject(0).getJSONObject("data").getLong("value"));
        store.applyChanges(new JSONArray(),new JSONArray().put(new JSONObject().put("recordId","one").put("deletedMs",now+3)),epoch);
        store.importRecords(new JSONArray().put(record("one",now+2,15)),epoch);assertEquals(0,page().getJSONArray("records").length());
        assertTrue(store.dirtyDays().length()>0);
    }
    @Test public void overlappingSourcesAndRepeatedZeroAreNotAdded()throws Exception {
        String day=HealthJournalStore.day(now);long epoch=store.generation();store.healthDay(day,new JSONObject().put("steps",100).put("activeCalories",50).put("warnings",new JSONArray()),epoch);
        store.setPreferences(new JSONObject().put("logWatchSteps",true));epoch=store.generation();store.watchSteps("watch-id","GT4Pro+",0,now,now+10,epoch);store.watchSteps("watch-id","GT4Pro+",20,now,now+20,epoch);
        assertEquals(100,store.projection().getJSONObject("days").getJSONObject(day).getLong("steps"));
        store.setPreferences(new JSONObject().put("primary",HealthJournalStore.BLE));assertEquals(20,store.projection().getJSONObject("days").getJSONObject(day).getLong("steps"));
        assertEquals(50,store.projection().getJSONObject("days").getJSONObject(day).getLong("activeCalories"));
        assertEquals(1,page().getJSONArray("records").length());assertEquals(2,page().getJSONArray("days").length());
    }
    @Test public void partialReadKeepsPreviousValuesAndMidnightReadIsRejected()throws Exception {
        long epoch=store.generation();String day=HealthJournalStore.day(now);store.healthDay(day,new JSONObject().put("steps",20),epoch);
        store.healthDay(day,new JSONObject().put("steps",JSONObject.NULL).put("warnings",new JSONArray().put("READ_STEPS: unavailable")),epoch);
        assertEquals(20,store.projection().getJSONObject("days").getJSONObject(day).getLong("steps"));
        store.setPreferences(new JSONObject().put("logWatchSteps",true));try{store.watchSteps("watch","GT4Pro+",0,now,now+86400000,epoch);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void restoreValidationRollbackCommitAndEraseInvalidateInFlightReads()throws Exception {
        store.importRecords(new JSONArray().put(record("one",now,12)),store.generation());JSONObject old=store.exportSnapshot();
        JSONObject next=new JSONObject().put("schemaVersion",1).put("records",new JSONArray().put(record("two",now+1,19))).put("days",new JSONArray());
        JSONObject bad=new JSONObject(next.toString());bad.getJSONArray("records").getJSONObject(0).put("channel","made_up");
        try{store.prepareRestore(bad);fail();}catch(IllegalArgumentException expected){}assertEquals("one",page().getJSONArray("records").getJSONObject(0).getString("recordId"));
        long epoch=store.generation();String token=store.prepareRestore(next);
        try{store.importRecords(new JSONArray().put(record("late",now+2,8)),epoch);fail();}catch(IllegalStateException expected){}
        store.installRestore(token);store.close();store=new HealthJournalStore(RuntimeEnvironment.getApplication());assertEquals(token,store.pendingRestore());
        store.finishRestore(token,false);assertEquals("one",page().getJSONArray("records").getJSONObject(0).getString("recordId"));
        token=store.prepareRestore(next);store.installRestore(token);store.finishRestore(token,true);assertEquals("two",page().getJSONArray("records").getJSONObject(0).getString("recordId"));
        epoch=store.generation();store.erase();try{store.importRecords(old.getJSONArray("records"),epoch);fail();}catch(IllegalStateException expected){}
        assertEquals(0,page().getJSONArray("records").length());assertEquals(0,store.projection().getJSONObject("days").length());
    }
    @Test public void paginationIsBoundedAndSourceFiltered()throws Exception {
        JSONArray records=new JSONArray();for(int i=0;i<60;i++)records.put(record("id"+i,now+i,i));store.importRecords(records,store.generation());
        JSONObject first=store.page("2020-01-01","2030-01-01",HealthJournalStore.HC,"com.samsung.android.app.shealth",0,0,30);
        assertEquals(30,first.getJSONArray("records").length());long cursor=first.getLong("nextCursor");assertTrue(cursor>0);
        JSONObject second=store.page("2020-01-01","2030-01-01",HealthJournalStore.HC,"com.samsung.android.app.shealth",cursor,first.getLong("nextTime"),30);assertEquals(30,second.getJSONArray("records").length());assertEquals(0,second.getLong("nextCursor"));
        assertNotEquals(first.getJSONArray("records").getJSONObject(0).getString("recordId"),second.getJSONArray("records").getJSONObject(0).getString("recordId"));
    }
    @Test public void completeRecentReconciliationPreservesCrossBoundaryAndOlderRecords()throws Exception {
        long epoch=store.generation();JSONObject contained=record("contained",now,5);
        JSONObject cross=record("cross",now,6).put("startMs",now-1000);
        JSONObject older=record("older",now-40L*86400000,7).put("startMs",now-40L*86400000).put("endMs",now-40L*86400000+1000).put("day",HealthJournalStore.day(now-40L*86400000));
        store.importRecords(new JSONArray().put(contained).put(cross).put(older),epoch);
        store.reconcileRecords(new JSONArray(),"StepsRecord",now,now+2000,now+3000,epoch);
        assertEquals(2,page().getJSONArray("records").length());assertEquals(1,store.dirtyDays().length());
        store.importRecords(new JSONArray().put(contained),epoch);assertEquals(2,page().getJSONArray("records").length());
        store.reconcileRecords(new JSONArray(),"StepsRecord",now-40L*86400000,now-39L*86400000,now+3000,epoch);
        assertEquals(2,page().getJSONArray("records").length());
    }
    @Test public void sleepUsesCompletionDayAndRetainsItsSourceAndStages()throws Exception {
        long end=now+86400000,start=now;String completed=HealthJournalStore.day(end);long epoch=store.generation();
        JSONObject sleep=record("sleep",end,0).put("type","SleepSessionRecord").put("startMs",start).put("endMs",end).put("day",completed).put("data",new JSONObject().put("stages",new JSONArray().put(new JSONObject().put("startMs",start).put("endMs",end).put("type",5).put("isAsleep",true))));
        store.importRecords(new JSONArray().put(sleep),epoch);store.healthDay(completed,new JSONObject().put("steps",0),epoch);
        JSONObject session=store.projection().getJSONObject("days").getJSONObject(completed).getJSONArray("sleepSessions").getJSONObject(0);
        assertEquals("sleep",session.getString("id"));assertEquals(sleep.getString("source"),session.getString("source"));assertEquals(HealthJournalStore.iso(end),session.getString("end"));assertTrue(session.getJSONArray("stages").getJSONObject(0).getBoolean("isAsleep"));
        assertEquals(0,store.page(HealthJournalStore.day(start),HealthJournalStore.day(start),"","",0,0,30).getJSONArray("records").length());
    }
    @Test public void legacyRecoveryIsMigratedAndCannotResurrectAfterErase()throws Exception {
        String day=HealthJournalStore.day(now);JSONObject legacy=new JSONObject().put("days",new JSONObject()).put("recoveryHistory",new JSONArray().put(new JSONObject().put("day",day).put("hrvRmssdMs",35).put("sleepMinutes",420)));
        store.initialize(legacy);JSONObject migrated=store.projection().getJSONObject("days").getJSONObject(day);assertEquals(35,migrated.getLong("hrvRmssdMs"));assertEquals(420,migrated.getJSONObject("legacyRecovery").getLong("sleepMinutes"));
        store.erase();store.initialize(legacy);assertEquals(0,store.projection().getJSONObject("days").length());
    }

    @Test public void perOriginSummariesCannotReplaceMixedDailyProjection()throws Exception {
        String date=HealthJournalStore.day(now);long epoch=store.generation();
        store.healthDay(date,new JSONObject().put("steps",1900).put("source","@aggregate").put("activityOrigin","").put("readAt",HealthJournalStore.iso(now)),epoch);
        store.healthDay(date,new JSONObject().put("steps",2281).put("source",HealthDailySource.SAMSUNG).put("activityOrigin",HealthDailySource.SAMSUNG).put("readAt",HealthJournalStore.iso(now)),epoch);
        assertEquals(1900,store.projection().getJSONObject("days").getJSONObject(date).getLong("steps"));
        JSONObject selected=store.page(date,date,HealthJournalStore.HC,HealthDailySource.SAMSUNG,0,0,30);
        assertEquals(1,selected.getJSONArray("days").length());assertEquals(2281,selected.getJSONArray("days").getJSONObject(0).getLong("steps"));
        assertEquals(1,store.page(date,date,"","",0,0,30).getJSONArray("days").length());
    }
    @Test public void switchingDailySourceInvalidatesInFlightReadsAndMarksCachePartial()throws Exception {
        String date=HealthJournalStore.day(now);long epoch=store.generation();store.healthDay(date,new JSONObject().put("steps",0),epoch);
        store.setPreferences(new JSONObject().put("hcDailySource",HealthDailySource.SAMSUNG));
        assertTrue(store.projection().getJSONObject("days").getJSONObject(date).getBoolean("partial"));
        try{store.healthDay(date,new JSONObject().put("steps",99),epoch);fail();}catch(IllegalStateException expected){}
        store.healthDay(date,new JSONObject().put("steps",2281).put("activityOrigin",HealthDailySource.SAMSUNG),store.generation());
        assertEquals(2281,store.projection().getJSONObject("days").getJSONObject(date).getLong("steps"));
    }
    @Test public void backgroundDisablePreventsLateWatchWritesAndIsNotExported()throws Exception {
        store.setPreferences(new JSONObject().put("logWatchSteps",true).put("bleBackground",true).put("hcBackground",true));long epoch=store.generation();
        store.watchSteps("watch","GT4Pro+",0,now,now,epoch);
        store.setPreferences(new JSONObject().put("logWatchSteps",false));assertFalse(store.preferences().getBoolean("bleBackground"));
        try{store.watchSteps("watch","GT4Pro+",10,now,now+100,epoch);fail();}catch(IllegalStateException expected){}
        JSONObject backup=store.exportSnapshot();assertFalse(backup.toString().contains("bleBackground"));assertFalse(backup.toString().contains("hcBackground"));
        store.erase();assertFalse(store.preferences().getBoolean("hcBackground"));assertFalse(store.preferences().getBoolean("bleBackground"));
    }
    @Test public void sourceSummaryIsInvalidatedWhenItsRecordIsDeleted()throws Exception {
        String date=HealthJournalStore.day(now);long epoch=store.generation();
        store.importRecords(new JSONArray().put(record("one",now,12).put("source",HealthDailySource.SAMSUNG)),epoch);
        store.healthDay(date,new JSONObject().put("source",HealthDailySource.SAMSUNG).put("steps",12).put("readAt",HealthJournalStore.iso(now+100)),epoch);
        assertEquals(1,store.page(date,date,HealthJournalStore.HC,HealthDailySource.SAMSUNG,0,0,30).getJSONArray("days").length());
        store.applyChanges(new JSONArray(),new JSONArray().put(new JSONObject().put("recordId","one").put("deletedMs",now+200)),epoch);
        assertEquals(0,store.page(date,date,HealthJournalStore.HC,HealthDailySource.SAMSUNG,0,0,30).getJSONArray("days").length());
        assertTrue(store.needsSourceDay(date,HealthDailySource.SAMSUNG));
    }
}
