package com.repforge.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import com.samsung.android.sdk.health.data.HealthDataStore;
import com.samsung.android.sdk.health.data.data.HealthDataPoint;
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
            (proxy,method,args)->{if(method.getName().equals("readDataAsync"))return future;throw new AssertionError("Unexpected SDK call "+method.getName());});
        return new SamsungHealthPlugin.Reader(store,allowed==null?Collections.emptySet():Collections.singleton(Permission.of(allowed,AccessType.READ)),()->{});
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
            .setDuration(Duration.ofHours(1)).setExerciseType(DataType.ExerciseType.PredefinedExerciseType.values()[0])
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
}
