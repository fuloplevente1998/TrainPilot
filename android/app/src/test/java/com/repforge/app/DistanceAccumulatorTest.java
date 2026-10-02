package com.repforge.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class DistanceAccumulatorTest {
 @Test public void measuresSuccessiveSegmentsInMeters(){DistanceAccumulator d=new DistanceAccumulator();assertTrue(d.add(47,19,5,1000,3));assertTrue(d.add(47.0009,19,5,31000,3));assertTrue(d.add(47.0018,19,5,61000,3));assertEquals(200.15,d.totalMeters(),1);}
 @Test public void rejectsBadFixesAndImpossibleJumps(){DistanceAccumulator d=new DistanceAccumulator();d.add(47,19,5,1000,3);assertFalse(d.add(48,19,5,2000,3));assertFalse(d.add(47.0001,19,60,3000,3));assertFalse(d.add(Double.NaN,19,5,4000,3));assertFalse(d.add(91,19,5,4000,3));assertFalse(d.add(47.0001,19,5,1000,3));assertEquals(0,d.totalMeters(),0);assertTrue(d.add(47.0001,19,5,5000,3));assertEquals(11.12,d.totalMeters(),.1);}
 @Test public void stationaryJitterDoesNotBecomeDistance(){DistanceAccumulator d=new DistanceAccumulator();d.add(47,19,8,1000,0);for(int i=1;i<20;i++)d.add(47+(i%2)*.00002,19,8,1000+i*1000,0);assertEquals(0,d.totalMeters(),0);}
 @Test public void signalGapsAreNotInventedRouteSegments(){DistanceAccumulator d=new DistanceAccumulator();d.add(47,19,5,1000,3);d.add(47.0009,19,5,31000,3);double before=d.totalMeters();d.add(47.02,19,5,65000,3);assertEquals(before,d.totalMeters(),0);d.add(47.0201,19,5,69000,3);assertEquals(before+11.12,d.totalMeters(),.2);}
}
