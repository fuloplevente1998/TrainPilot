package com.repforge.app;

import android.health.connect.datatypes.*;
import android.health.connect.datatypes.Record;
import androidx.annotation.RequiresApi;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;

/** Raw, source-attributed HC records. Daily HC aggregates are stored separately. */
@RequiresApi(34)
final class HealthConnectJournal {
    static JSONArray serialize(List<? extends Record> records,String ownPackage) throws Exception {
        JSONArray out=new JSONArray();for(Record r:records)if(!r.getMetadata().getDataOrigin().getPackageName().equals(ownPackage))out.put(serialize(r));return out;
    }
    private static void zone(JSONObject out,String key,ZoneOffset offset)throws Exception {if(offset!=null)out.put(key,offset.getTotalSeconds());}
    private static boolean asleep(int type) {return type==SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING||type==SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT||type==SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP||type==SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM;}
    static JSONObject serialize(Record r) throws Exception {
        long start,end;
        JSONObject d=new JSONObject(),out=new JSONObject().put("channel",HealthJournalStore.HC).put("source",r.getMetadata().getDataOrigin().getPackageName()).put("type",r.getClass().getSimpleName()).put("recordId",r.getMetadata().getId()).put("lastModifiedMs",r.getMetadata().getLastModifiedTime().toEpochMilli());
        if(r instanceof IntervalRecord){IntervalRecord x=(IntervalRecord)r;start=x.getStartTime().toEpochMilli();end=x.getEndTime().toEpochMilli();zone(out,"startZoneOffsetSeconds",x.getStartZoneOffset());zone(out,"endZoneOffsetSeconds",x.getEndZoneOffset());}
        else if(r instanceof InstantRecord){InstantRecord x=(InstantRecord)r;start=end=x.getTime().toEpochMilli();zone(out,"zoneOffsetSeconds",x.getZoneOffset());}
        else throw new IllegalArgumentException("Unsupported HC record");
        out.put("startMs",start).put("endMs",end).put("day",HealthJournalStore.day(r instanceof SleepSessionRecord?end:start));
        if(r instanceof StepsRecord){d.put("value",((StepsRecord)r).getCount()).put("unit","steps");}
        else if(r instanceof ActiveCaloriesBurnedRecord){d.put("value",((ActiveCaloriesBurnedRecord)r).getEnergy().getInCalories()/1000.0).put("unit","kcal").put("energyType","active");}
        else if(r instanceof TotalCaloriesBurnedRecord){d.put("value",((TotalCaloriesBurnedRecord)r).getEnergy().getInCalories()/1000.0).put("unit","kcal").put("energyType","total");}
        else if(r instanceof DistanceRecord){d.put("value",((DistanceRecord)r).getDistance().getInMeters()).put("unit","m");}
        else if(r instanceof RestingHeartRateRecord){d.put("value",((RestingHeartRateRecord)r).getBeatsPerMinute()).put("unit","bpm");}
        else if(r instanceof HeartRateVariabilityRmssdRecord){d.put("value",((HeartRateVariabilityRmssdRecord)r).getHeartRateVariabilityMillis()).put("unit","ms");}
        else if(r instanceof WeightRecord){d.put("value",((WeightRecord)r).getWeight().getInGrams()/1000.0).put("unit","kg");}
        else if(r instanceof BodyFatRecord){d.put("value",((BodyFatRecord)r).getPercentage().getValue()).put("unit","%");}
        else if(r instanceof OxygenSaturationRecord){d.put("value",((OxygenSaturationRecord)r).getPercentage().getValue()).put("unit","%");}
        else if(r instanceof Vo2MaxRecord){Vo2MaxRecord x=(Vo2MaxRecord)r;d.put("value",x.getVo2MillilitersPerMinuteKilogram()).put("unit","ml/kg/min").put("method",x.getMeasurementMethod());}
        else if(r instanceof BloodPressureRecord){BloodPressureRecord x=(BloodPressureRecord)r;d.put("systolic",x.getSystolic().getInMillimetersOfMercury()).put("diastolic",x.getDiastolic().getInMillimetersOfMercury()).put("unit","mmHg");}
        else if(r instanceof BloodGlucoseRecord){d.put("value",((BloodGlucoseRecord)r).getLevel().getInMillimolesPerLiter()).put("unit","mmol/L");}
        else if(r instanceof RespiratoryRateRecord){d.put("value",((RespiratoryRateRecord)r).getRate()).put("unit","/min");}
        else if(r instanceof HeartRateRecord){JSONArray samples=new JSONArray();for(HeartRateRecord.HeartRateSample s:((HeartRateRecord)r).getSamples())samples.put(new JSONObject().put("timeMs",s.getTime().toEpochMilli()).put("value",s.getBeatsPerMinute()));d.put("samples",samples).put("unit","bpm");}
        else if(r instanceof SpeedRecord){JSONArray samples=new JSONArray();for(SpeedRecord.SpeedRecordSample s:((SpeedRecord)r).getSamples())samples.put(new JSONObject().put("timeMs",s.getTime().toEpochMilli()).put("value",s.getSpeed().getInMetersPerSecond()));d.put("samples",samples).put("unit","m/s");}
        else if(r instanceof SleepSessionRecord){SleepSessionRecord x=(SleepSessionRecord)r;JSONArray stages=new JSONArray();List<long[]> intervals=new ArrayList<>();
            for(SleepSessionRecord.Stage s:x.getStages()){long a=Math.max(start,s.getStartTime().toEpochMilli()),b=Math.min(end,s.getEndTime().toEpochMilli());stages.put(new JSONObject().put("startMs",a).put("endMs",b).put("type",s.getType()).put("isAsleep",asleep(s.getType())));if(asleep(s.getType())&&b>a)intervals.add(new long[]{a,b});}
            intervals.sort(Comparator.comparingLong(a->a[0]));long total=0,a=-1,b=-1;for(long[] span:intervals){if(a<0){a=span[0];b=span[1];}else if(span[0]<=b)b=Math.max(b,span[1]);else{total+=b-a;a=span[0];b=span[1];}}if(a>=0)total+=b-a;
            // A stage-less session is explicitly a session duration, not measured sleep.
            d.put("value",(x.getStages().isEmpty()?end-start:total)/60000.0).put("unit","min").put("durationKind",x.getStages().isEmpty()?"session":"asleep_stages").put("stages",stages);
        }
        else if(r instanceof ExerciseSessionRecord){ExerciseSessionRecord x=(ExerciseSessionRecord)r;d.put("value",Duration.between(x.getStartTime(),x.getEndTime()).toMillis()/60000.0).put("unit","min").put("exerciseType",x.getExerciseType());}
        else throw new IllegalArgumentException("Unsupported HC record type");
        return out.put("data",d);
    }
}
