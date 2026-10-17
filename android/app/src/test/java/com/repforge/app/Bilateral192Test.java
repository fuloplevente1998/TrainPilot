package com.repforge.app;

import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=33,manifest=Config.NONE)
public class Bilateral192Test {
    private IntervalTimeline.Step row(String id,String first,int left,int right,int swap,int rest,int rounds){return new IntervalTimeline.Step("side-plank",id,"Side plank",30,rest,rounds,"",new Bilateral192.Config(first,left,right,swap,rest));}
    private JSONArray rows(IntervalTimeline.Step... steps)throws Exception{JSONArray out=new JSONArray();for(IntervalTimeline.Step s:steps)out.put(new JSONObject().put("exerciseId",s.exerciseId).put("setId",s.setId).put("work",s.work).put("rest",s.rest).put("rounds",s.rounds).put("bilateral192",s.bilateral.json()));return out;}
    @Test public void bothOrdersHaveExactBoundariesSeparateRestsAndNoTrailingRest()throws Exception{
        for(String first:new String[]{"left","right"}){
            IntervalTimeline.Step a=row("a",first,30,40,10,60,1),b=row("b",first,30,40,10,60,1);IntervalTimeline t=new IntervalTimeline(Arrays.asList(a,b),true);
            int firstMs=a.bilateral.work(first)*1000;String second=a.bilateral.second();
            assertEquals(first,t.at(0).side192);assertEquals("work",t.at(firstMs-1).phase);assertEquals("side",t.at(firstMs).restKind192);assertEquals(second,t.at(firstMs).side192);
            assertEquals("work",t.at(firstMs+10000).phase);assertEquals("set",t.at(80000).restKind192);assertEquals(first,t.at(140000).side192);assertEquals(1,t.at(140000).step);
            assertEquals("complete",t.at(220000).phase);assertEquals(220000,t.at(0).totalMs);
            IntervalTimeLedger ledger=new IntervalTimeLedger(2,0);ledger.sample(t,220000,220000,false,false,false);JSONObject log=ledger.json(rows(a,b));
            assertEquals(2,log.getInt("schema"));assertEquals(140000,log.getLong("workMs"));assertEquals(80000,log.getLong("restMs"));
            JSONObject part=log.getJSONArray("steps").getJSONObject(0);assertEquals(30000,part.getLong("leftWorkMs"));assertEquals(40000,part.getLong("rightWorkMs"));assertEquals(10000,part.getLong("sideRestMs"));assertEquals(60000,part.getLong("setRestMs"));
            JSONObject value=Bilateral192.values(rows(a).getJSONObject(0),part,"clock");assertEquals("30",value.getString("reps"));assertTrue(value.getBoolean("done"));
        }
    }
    @Test public void zeroRestAndMultipleRoundsRemainOneBilateralSet(){
        IntervalTimeline t=new IntervalTimeline(Collections.singletonList(row("a","right",2,3,0,0,2)),true);
        assertEquals("left",t.at(3000).side192);assertEquals(2,t.at(5000).round);assertEquals("right",t.at(5000).side192);assertEquals("complete",t.at(10000).phase);assertEquals(1,t.at(10000).completed);
    }
    @Test public void skippedRestAndManualPauseReceiveOnlyActualTime()throws Exception{
        IntervalTimeline.Step a=row("a","left",3,4,10,60,1);IntervalTimeline t=new IntervalTimeline(Collections.singletonList(a),true);IntervalTimeLedger l=new IntervalTimeLedger(1,0);
        l.sample(t,3000,3000,false,false,false);l.sample(t,3000,7000,true,false,false);l.sample(t,5000,9000,true,false,false);
        l.skipTo(13000);l.sample(t,17000,13000,false,false,false);JSONObject part=l.json(rows(a)).getJSONArray("steps").getJSONObject(0);
        assertEquals(3000,part.getLong("leftWorkMs"));assertEquals(4000,part.getLong("rightWorkMs"));assertEquals(2000,part.getLong("sideRestMs"));assertEquals(4000,part.getLong("pausedMs"));
    }
    @Test public void pausedConfigurationFreezesPastAndCurrentWorkOnly()throws Exception{
        IntervalTimeline.Step a=row("a","left",30,40,10,60,1),b=row("b","left",30,40,10,60,1);IntervalTimeline old=new IntervalTimeline(Arrays.asList(a,b),true);
        Map<String,Long> frozen=old.freezeThrough192(old.at(15000));IntervalTimeline revised=new IntervalTimeline(Arrays.asList(row("a","left",50,45,10,60,1),row("b","left",50,45,10,60,1)),true,0,false,frozen);
        assertEquals(15000,revised.at(15000).remainingMs);assertEquals(250000,revised.at(0).totalMs);assertEquals(30000,revised.targetWork192(0,"left"));assertEquals(50000,revised.targetWork192(1,"left"));
        revised.adjustRest192(revised.at(30000),5000);assertEquals(15000,revised.at(30000).remainingMs);assertEquals("right",revised.at(45000).side192);
        assertEquals("work",revised.at(45000).phase);assertEquals(10000,revised.at(200000).phaseDuration192);
    }
    @Test public void skippedWorkIsPartialAndCannotBecomeACompletedSet()throws Exception{
        IntervalTimeline.Step a=row("a","left",3,4,0,0,1);IntervalTimeline t=new IntervalTimeline(Collections.singletonList(a),true);IntervalTimeLedger l=new IntervalTimeLedger(1,0);
        l.sample(t,1000,1000,true,false,false);l.skipWork(t.at(1000));l.skipTo(3000);l.sample(t,7000,5000,false,false,false);
        JSONObject part=l.json(rows(a)).getJSONArray("steps").getJSONObject(0),v=Bilateral192.values(rows(a).getJSONObject(0),part,"clock");
        assertEquals(1000,part.getLong("leftWorkMs"));assertFalse(v.getBoolean("done"));assertTrue(v.getJSONObject("sideState192").getBoolean("skipped"));
    }
    @Test public void ledgerRestoresWithoutCountingTheUnobservedGapTwice()throws Exception{
        IntervalTimeline.Step a=row("a","right",3,4,2,0,1);IntervalTimeline t=new IntervalTimeline(Collections.singletonList(a),true);IntervalTimeLedger original=new IntervalTimeLedger(1,0);
        original.sample(t,2000,2000,true,false,false);JSONObject saved=original.json(rows(a));IntervalTimeLedger restored=new IntervalTimeLedger(1,90000);restored.restore192(saved,rows(a),90000);
        restored.sample(t,4000,92000,true,false,false);JSONObject part=restored.json(rows(a)).getJSONArray("steps").getJSONObject(0);assertEquals(4000,part.getLong("rightWorkMs"));assertEquals(0,part.getLong("pausedMs"));
    }
}
