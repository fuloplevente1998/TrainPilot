package com.repforge.app;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class SamsungHealthJournalTest {
    private HealthJournalStore store;
    private final String day = "2026-10-07";
    @Before public void setup() {
        RuntimeEnvironment.getApplication().deleteDatabase("health-journal.db");
        store = new HealthJournalStore(RuntimeEnvironment.getApplication());
    }
    @After public void cleanup() { store.close(); }
    private JSONObject projected() throws Exception { return store.projection().getJSONObject("days").getJSONObject(day); }
    private void seed() throws Exception {
        store.healthDay(day, new JSONObject().put("steps",314).put("activeCalories",26)
            .put("hrvRmssdMs",35).put("hrvTime","2026-10-07T05:00:00Z"), store.generation());
        store.samsungDay(day, new JSONObject().put("steps",6594).put("activeCalories",392)
            .put("totalCalories",2423).put("bodyFatPercent",18.4).put("skeletalMuscleMassKg",28.2)
            .put("hrvRmssdMs",JSONObject.NULL), new JSONArray(), store.generation());
    }
    @Test public void samsungValuesReplaceRatherThanAddAndHrvFallbackKeepsProvenance() throws Exception {
        seed(); assertEquals(314,projected().getInt("steps"));
        store.setPreferences(new JSONObject().put("provider",HealthJournalStore.SAMSUNG));
        JSONObject selected=projected(); assertEquals(6594,selected.getInt("steps"));
        assertEquals(392,selected.getInt("activeCalories")); assertEquals(2423,selected.getInt("totalCalories"));
        assertEquals(18.4,selected.getDouble("bodyFatPercent"),0.001);
        assertEquals(28.2,selected.getDouble("skeletalMuscleMassKg"),0.001);
        assertEquals(35,selected.getInt("hrvRmssdMs"));
        assertEquals(HealthJournalStore.HC,selected.getJSONObject("metricProviders").getString("hrvRmssdMs"));
        assertEquals(HealthJournalStore.SAMSUNG,selected.getJSONObject("metricProviders").getString("activeCalories"));
        JSONObject page=store.page(day,day,"journal","",0,0,30);
        assertEquals(1,page.getJSONArray("days").length());
        assertEquals(HealthJournalStore.SAMSUNG,page.getJSONArray("days").getJSONObject(0).getString("channel"));
    }
    @Test public void zeroIsAReadingAndMissingHrvIsNotInvented() throws Exception {
        store.healthDay(day,new JSONObject().put("steps",100),store.generation());
        store.samsungDay(day,new JSONObject().put("steps",0),new JSONArray(),store.generation());
        store.setPreferences(new JSONObject().put("provider",HealthJournalStore.SAMSUNG));
        assertEquals(0,projected().getInt("steps")); assertTrue(projected().isNull("hrvRmssdMs"));
    }
    @Test public void providerSurvivesRetirementRestartAndSnapshotRestore() throws Exception {
        seed();store.setPreferences(new JSONObject().put("provider",HealthJournalStore.SAMSUNG));
        store.retireWatchUi();store.close();store=new HealthJournalStore(RuntimeEnvironment.getApplication());
        assertEquals(HealthJournalStore.SAMSUNG,store.preferences().getString("provider"));
        JSONObject saved=store.exportSnapshot();String token=store.prepareRestore(saved);
        store.installRestore(token);store.finishRestore(token,true);
        assertEquals(392,projected().getInt("activeCalories"));
        store.setPreferences(new JSONObject().put("provider",HealthJournalStore.HC));
        assertEquals(26,projected().getInt("activeCalories"));
    }
    @Test public void restoreAndProviderChangeRejectOldReadsAndFailedFieldsKeepPreviousValues() throws Exception {
        seed();long old=store.generation();store.setPreferences(new JSONObject().put("provider",HealthJournalStore.SAMSUNG));
        try{store.samsungDay(day,new JSONObject().put("steps",9),new JSONArray(),old);fail();}catch(IllegalStateException expected){}
        store.samsungDay(day,new JSONObject().put("steps",7000).put("failedFields",new JSONArray().put("activeCalories")),new JSONArray(),store.generation());
        assertEquals(7000,projected().getInt("steps"));assertEquals(392,projected().getInt("activeCalories"));assertTrue(projected().getBoolean("partial"));
        String token=store.prepareRestore(store.exportSnapshot());
        try{store.samsungDay(day,new JSONObject(),new JSONArray(),store.generation());fail();}catch(IllegalStateException expected){}
        store.finishRestore(token,false);
    }
    @Test public void samsungOnlyDaysStayHiddenForHealthConnectUsers() throws Exception {
        store.samsungDay(day,new JSONObject().put("steps",42),new JSONArray(),store.generation());
        assertFalse(store.projection().getJSONObject("days").has(day));
        assertEquals(0,store.page(day,day,"journal","",0,0,30).getJSONArray("days").length());
    }
    @Test public void stepOriginFollowsTheActualMetricProviderEvenWithSamsungFallback() throws Exception {
        JSONObject hc=new JSONObject().put("steps",314).put("activityOrigin","com.android.healthconnect.phone");
        JSONObject samsung=new JSONObject().put("steps",JSONObject.NULL).put("activityOrigin","com.sec.android.app.shealth");
        JSONObject value=HealthProviderProjection.day(hc,samsung,null,"samsung_health");
        assertEquals("health_connect",value.getString("stepsSource"));
        assertEquals("com.android.healthconnect.phone",value.getString("stepsOrigin"));
        samsung.put("steps",0);value=HealthProviderProjection.day(hc,samsung,null,"samsung_health");
        assertEquals("samsung_health",value.getString("stepsSource"));
        assertEquals("com.sec.android.app.shealth",value.getString("stepsOrigin"));
    }
}
