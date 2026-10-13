package com.repforge.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
public class IntervalTimelineTest {
 private IntervalTimeline.Step step(String id,int work,int rest,int rounds){return new IntervalTimeline.Step("running",id,"Run",work,rest,rounds,"");}
 @Test public void autoPlanHasInterSetRestButNoFinalRest(){IntervalTimeline t=new IntervalTimeline(Arrays.asList(step("a",2,1,2),step("b",2,1,2)),true);long[] times={0,1999,2000,3000,5000,6000,8000,9000,11000,999999};String[] phases={"work","work","rest","work","rest","work","rest","work","complete","complete"};int[] owners={0,0,0,0,0,1,1,1,1,1};for(int i=0;i<times.length;i++){IntervalTimeline.Phase p=t.at(times[i]);assertEquals(phases[i],p.phase);assertEquals(owners[i],p.step);assertEquals(11000,p.totalMs);}assertEquals(2,t.at(11000).completed);assertEquals(11000,t.at(999999).elapsedMs);}
 @Test public void manualAndZeroRestAreFinite(){IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",2,0,2)),false);assertEquals(2,t.at(2000).round);assertEquals("work",t.at(2000).phase);assertEquals("complete",t.at(4000).phase);assertEquals(0,t.at(-2000).elapsedMs);}
 @Test public void partialSecondsAndLateCallbackHaveOneCurrentPhase(){IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",30,60,3)),false);assertEquals(1,t.at(29999).remainingMs);assertEquals(3,t.at(180000).round);assertEquals("work",t.at(180000).phase);assertEquals("complete",t.at(Long.MAX_VALUE).phase);}
 @Test(expected=IllegalArgumentException.class) public void invalidWorkRejected(){step("a",0,1,2);}
 @Test(expected=IllegalArgumentException.class) public void tooManyOwnersRejected(){ArrayList<IntervalTimeline.Step> rows=new ArrayList<>();for(int i=0;i<101;i++)rows.add(step("a"+i,1,0,1));new IntervalTimeline(rows,true);}
 @Test(expected=IllegalArgumentException.class) public void excessiveTotalRejected(){new IntervalTimeline(Arrays.asList(step("a",3600,3600,20)),true);}
 @Test public void initialRestStartsTimedWorkWithoutAddingRestToMeasuredSet(){IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",2,1,1)),true,30,false);assertEquals("rest",t.at(29999).phase);assertEquals(0,t.at(29999).stepElapsedMs);assertEquals("work",t.at(30000).phase);assertEquals(0,t.at(30000).stepElapsedMs);assertEquals("complete",t.at(32000).phase);assertEquals(2000,t.at(32000).stepElapsedMs);}
 @Test public void repetitionRestNeverMeasuresOrCompletesAWorkSet(){IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",1,30,1)),false,0,true);assertEquals("rest",t.at(0).phase);assertEquals(30000,t.at(0).remainingMs);assertEquals("complete",t.at(30000).phase);assertEquals(0,t.at(30000).stepElapsedMs);assertEquals(30000,t.at(Long.MAX_VALUE).elapsedMs);}
 @Test(expected=IllegalArgumentException.class) public void restCannotHaveMultipleOwners(){new IntervalTimeline(Arrays.asList(step("a",1,30,1),step("b",1,30,1)),false,0,true);}
 @Test public void phaseDurationKeepsLongInitialRestSeparateFromShortIntervalRest(){
  IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",30,15,2)),false,120,false);
  assertEquals(120000,t.phaseDurationMs(t.at(0)));assertEquals(120000,t.phaseDurationMs(t.at(60000)));
  assertEquals(120000,t.phaseDurationMs(t.at(119999)));assertEquals(30000,t.phaseDurationMs(t.at(120000)));
  assertEquals(15000,t.phaseDurationMs(t.at(150000)));assertEquals(0,t.phaseDurationMs(t.at(195000)));
 }
 @Test public void repetitionRestDurationDoesNotShrinkWithRemainingTime(){
  IntervalTimeline t=new IntervalTimeline(Collections.singletonList(step("a",1,120,1)),false,0,true);
  assertEquals(120000,t.phaseDurationMs(t.at(0)));assertEquals(120000,t.phaseDurationMs(t.at(60000)));
 }
}
