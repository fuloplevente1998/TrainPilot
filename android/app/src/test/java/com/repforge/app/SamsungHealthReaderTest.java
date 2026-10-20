package com.repforge.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import com.samsung.android.sdk.health.data.HealthDataStore;
import com.samsung.android.sdk.health.data.data.HealthDataPoint;
import com.samsung.android.sdk.health.data.data.AggregatedData;
import com.samsung.android.sdk.health.data.data.entries.ExerciseSession;
import com.samsung.android.sdk.health.data.permission.*;
import com.samsung.android.sdk.health.data.request.*;
import com.samsung.android.sdk.health.data.response.*;
import com.getcapacitor.JSObject;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

/** Exercise the uploaded SDK's real builders and fields, without Samsung service access. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class SamsungHealthReaderTest {
    private final List<ReadDataRequest> energyRequests = new ArrayList<>();
    private final Instant start=Instant.parse("2026-10-07T22:00:00Z"),end=start.plusSeconds(3600);
    private DataResponse<HealthDataPoint> response(String token,List<HealthDataPoint> rows) throws Exception {
        Constructor<DataResponse> constructor=DataResponse.class.getDeclaredConstructor(String.class,ArrayList.class);
        constructor.setAccessible(true);
        return constructor.newInstance(token,new ArrayList<>(rows));
    }
    private SamsungHealthPlugin.Reader reader(DataType allowed,List<HealthDataPoint> rows,String token) throws Exception {
        DataResponse<HealthDataPoint> data=response(token,rows);
        AsyncSingleFuture future=(AsyncSingleFuture)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{AsyncSingleFuture.class},
            (proxy,method,args)->method.getName().equals("get")?data:method.getName().equals("isDone")?true:null);
        HealthDataStore store=(HealthDataStore)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HealthDataStore.class},
            (proxy,method,args)->{
                if(method.getName().equals("readDataAsync")) {
                    ReadDataRequest request = (ReadDataRequest) args[0];
                    if (DataTypes.ENERGY_SCORE.equals(request.getDataType())) {
                        LocalTimeFilter filter = request.getLocalTimeFilter();
                        assertNotNull("Validate the final SDK query, not just the source builder", filter);
                        assertTrue("Samsung rejects zero-length daily queries with 1001", filter.getStartTime().isBefore(filter.getEndTime()));
                        assertTrue(filter.isInclusiveStart()); assertFalse(filter.isInclusiveEnd());
                        energyRequests.add(request);
                    }
                    return future;
                }
                throw new AssertionError("Unexpected SDK call "+method.getName());
            });
        return new SamsungHealthPlugin.Reader(store,allowed==null?Collections.emptySet():Collections.singleton(Permission.of(allowed,AccessType.READ)),()->{});
    }
    @Test public void energyPermissionScopeDoesNotDependOnOtherHealthTypes() {
        assertEquals(Collections.singleton(Permission.of(DataTypes.ENERGY_SCORE, AccessType.READ)), SamsungHealthPlugin.energyPermission());
    }
    @Test public void sdkAccessErrorSurvivesAsyncAndPlatformCauseWrapping() {
        Throwable sdkError = new com.samsung.android.sdk.health.data.error.HealthDataException(
            2003, "blocked", new IllegalStateException("platform cause")) {};
        assertEquals("SAMSUNG_HEALTH_2003", SamsungHealthPlugin.errorCode(new java.util.concurrent.ExecutionException(sdkError)));
        assertEquals("SAMSUNG_HEALTH_UNAVAILABLE", SamsungHealthPlugin.errorCode(new IllegalStateException("unavailable")));
    }
    @Test public void energyScoreIsReadWhenAllowed() throws Exception {
        HealthDataPoint point = new HealthDataPoint.Builder()
            .setStartTime(start, ZoneOffset.UTC)
            .addFieldData(DataType.EnergyScoreType.ENERGY_SCORE, 83f)
            .build();
        JSObject out = reader(DataTypes.ENERGY_SCORE, Collections.singletonList(point), null).daily(start, end);
        assertEquals(83.0, out.getDouble("energyScore"), 0.001);
        assertEquals("com.sec.android.app.shealth", out.getString("energyScoreSource"));
    }
    @Test public void independentEnergyReadExcludesTheNextMidnightAndDoesNotReadOtherMetrics() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Budapest"));
            Instant midnight = Instant.parse("2026-10-07T22:00:00Z");
            Instant next = midnight.plusSeconds(86400);
            HealthDataPoint today = new HealthDataPoint.Builder().setStartTime(midnight, ZoneOffset.ofHours(2))
                .addFieldData(DataType.EnergyScoreType.ENERGY_SCORE, 83f).build();
            HealthDataPoint tomorrow = new HealthDataPoint.Builder().setStartTime(next, ZoneOffset.ofHours(2))
                .addFieldData(DataType.EnergyScoreType.ENERGY_SCORE, 99f).build();
            JSObject out = reader(DataTypes.ENERGY_SCORE, Arrays.asList(today, tomorrow), null).energyDay(midnight, next);
            assertEquals(83.0, out.getDouble("energyScore"), 0.001);
            assertEquals(midnight.toString(), out.getString("energyScoreTime"));
            assertFalse(out.has("steps")); assertFalse(out.has("sleepSessions"));
        } finally { TimeZone.setDefault(previous); }
    }
    @Test public void dailySdkFilterUsesFollowingMidnightAcrossBothDstTransitions() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        ZoneId zone = ZoneId.of("Europe/Budapest");
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(zone));
            for (String date : Arrays.asList("2026-10-20", "2026-03-29", "2026-10-25")) {
                LocalDate day = LocalDate.parse(date);
                ZonedDateTime from = day.atStartOfDay(zone), to = day.plusDays(1).atStartOfDay(zone);
                HealthDataPoint today = new HealthDataPoint.Builder().setStartTime(from.toInstant(), from.getOffset())
                    .addFieldData(DataType.EnergyScoreType.ENERGY_SCORE, 83f).build();
                HealthDataPoint tomorrow = new HealthDataPoint.Builder().setStartTime(to.toInstant(), to.getOffset())
                    .addFieldData(DataType.EnergyScoreType.ENERGY_SCORE, 99f).build();
                JSObject out = reader(DataTypes.ENERGY_SCORE, Arrays.asList(today, tomorrow), null).energyDay(from.toInstant(), to.toInstant());
                LocalTimeFilter filter = energyRequests.get(energyRequests.size()-1).getLocalTimeFilter();
                assertEquals(day.atStartOfDay(), filter.getStartTime());
                assertEquals(day.plusDays(1).atStartOfDay(), filter.getEndTime());
                assertEquals(83.0, out.getDouble("energyScore"), 0.001);
                assertEquals(0, out.getJSONArray("failedFields").length());
            }
            // The former same-date inclusive filter becomes equal midnights.
            LocalDate day = LocalDate.parse("2026-10-20");
            LocalTimeFilter oldFilter = DataTypes.ENERGY_SCORE.getReadDataRequestBuilder()
                .setLocalDateFilter(LocalDateFilter.of(day, day, true, true)).build().getLocalTimeFilter();
            assertEquals(oldFilter.getStartTime(), oldFilter.getEndTime());
        } finally { TimeZone.setDefault(previous); }
    }
    @Test public void independentEnergyReadDoesNotInventDataWithoutItsOwnPermission() throws Exception {
        JSObject out = reader(DataTypes.BODY_COMPOSITION, Collections.emptyList(), null).energyDay(start, end);
        assertTrue(out.isNull("energyScore"));
        assertFalse(out.getJSONObject("permissions").getBoolean("READ_ENERGY_SCORE"));
        assertFalse(out.has("weightKg"));
    }

    @Test public void bodyFatIsPercentAndMuscleAndWaterUseTheirActualSdkUnits() throws Exception {
        HealthDataPoint point=new HealthDataPoint.Builder().setStartTime(start,ZoneOffset.UTC)
            .addFieldData(DataType.BodyCompositionType.WEIGHT,66f)
            .addFieldData(DataType.BodyCompositionType.BODY_FAT,18.4f)
            .addFieldData(DataType.BodyCompositionType.SKELETAL_MUSCLE_MASS,28.2f)
            .addFieldData(DataType.BodyCompositionType.TOTAL_BODY_WATER,37.5f).build();
        JSObject out=reader(DataTypes.BODY_COMPOSITION,Collections.singletonList(point),null).daily(start,end);
        assertEquals(18.4,out.getDouble("bodyFatPercent"),0.001);
        assertEquals(28.2,out.getDouble("skeletalMuscleMassKg"),0.001);
        assertEquals(37.5,out.getDouble("totalBodyWaterLiters"),0.001);
        assertTrue(out.isNull("hrvRmssdMs"));assertFalse(out.getBoolean("hrvSupported"));
    }
    @Test public void directExerciseCaloriesAreClippedWithoutBorrowingBasalOrDailyEnergy() throws Exception {
        ExerciseSession session=new ExerciseSession.Builder().setStartTime(start).setEndTime(end)
            .setDuration(Duration.ofHours(1)).setExerciseType(DataType.ExerciseType.PredefinedExerciseType.CIRCUIT_TRAINING)
            .setCalories(280f).build();
        HealthDataPoint point=new HealthDataPoint.Builder().setStartTime(start,ZoneOffset.UTC).setEndTime(end,ZoneOffset.UTC)
            .addFieldData(DataType.ExerciseType.SESSIONS,Collections.singletonList(session)).build();
        JSObject out=reader(DataTypes.EXERCISE,Collections.singletonList(point),null).workout(start.plusSeconds(1800),end);
        assertEquals(140,out.getDouble("workoutCalories"),0.001);assertTrue(out.getBoolean("workoutEnergyProrated"));
        assertEquals(30,out.getDouble("exerciseMinutes"),0.001);
        assertTrue(out.isNull("activeCalories"));assertTrue(out.isNull("totalCalories"));
    }
    @Test public void repeatedPaginationTokenIsPartialRatherThanAFakeZeroOrAnInfiniteRead() throws Exception {
        JSObject out=reader(DataTypes.BODY_COMPOSITION,Collections.emptyList(),"repeat").daily(start,end);
        assertTrue(out.getJSONArray("warnings").length()>0);assertTrue(out.getJSONArray("failedFields").toString().contains("bodyFatPercent"));
        assertTrue(out.isNull("bodyFatPercent"));
    }
    @Test public void deniedTypesAreNotReadAndMissingWorkoutEnergyStaysMissing() throws Exception {
        JSObject out=reader(null,Collections.emptyList(),null).workout(start,end);
        assertTrue(out.isNull("workoutCalories"));assertEquals(1,out.getInt("workoutEnergyVersion"));assertEquals(0,out.getJSONArray("warnings").length());
    }
    private SamsungHealthPlugin.Reader stepsReader(Long value) throws Exception {
        Constructor<AggregatedData> bucket=AggregatedData.class.getDeclaredConstructor(Object.class,Instant.class,Instant.class);
        bucket.setAccessible(true);
        Constructor<DataResponse> constructor=DataResponse.class.getDeclaredConstructor(String.class,ArrayList.class);
        constructor.setAccessible(true);
        ArrayList<Object> rows=new ArrayList<>();if(value!=null)rows.add(bucket.newInstance(value,start,end));
        DataResponse data=constructor.newInstance(null,rows);
        AsyncSingleFuture future=(AsyncSingleFuture)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{AsyncSingleFuture.class},
            (proxy,method,args)->method.getName().equals("get")?data:method.getName().equals("isDone")?true:null);
        HealthDataStore store=(HealthDataStore)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{HealthDataStore.class},
            (proxy,method,args)->{if(method.getName().equals("aggregateDataAsync"))return future;throw new AssertionError("Steps must not read other health types: "+method.getName());});
        return new SamsungHealthPlugin.Reader(store,Collections.singleton(Permission.of(DataTypes.STEPS,AccessType.READ)),()->{});
    }
    @Test public void stepsWindowUsesOnlyStepsAndDistinguishesMissingFromZero() throws Exception {
        JSObject value=stepsReader(420L).stepsWindow(start,end);
        assertEquals(420,value.getLong("steps"));assertEquals(start.toString(),value.getString("start"));
        assertEquals(end.toString(),value.getString("end"));assertEquals("samsung_health",value.getString("provider"));
        assertFalse(value.has("activeCalories"));assertEquals(0,stepsReader(0L).stepsWindow(start,end).getLong("steps"));
        assertTrue(stepsReader(null).stepsWindow(start,end).isNull("steps"));
        assertTrue(reader(null,Collections.emptyList(),null).stepsWindow(start,end).isNull("steps"));
    }
}
