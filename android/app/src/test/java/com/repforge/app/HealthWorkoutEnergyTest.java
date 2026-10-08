package com.repforge.app;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HealthWorkoutEnergyTest {
    private static final String SAMSUNG = HealthWorkoutEnergy.SAMSUNG, OWN = "com.repforge.app";
    private static final long MINUTE = 60000, START = 1791412980000L;
    private HealthWorkoutEnergy.Session session(String source, long a, long b) {
        return new HealthWorkoutEnergy.Session(source, START+a*MINUTE, START+b*MINUTE);
    }
    private HealthWorkoutEnergy.EnergyRow energy(String id, String source, long a, long b, double kcal, boolean total) {
        return new HealthWorkoutEnergy.EnergyRow(id, source, START+a*MINUTE, START+b*MINUTE, kcal, total, 1);
    }
    private HealthWorkoutEnergy.Result match(List<HealthWorkoutEnergy.Session> sessions, List<HealthWorkoutEnergy.EnergyRow> rows, long a, long b) {
        return HealthWorkoutEnergy.match(sessions, rows, START+a*MINUTE, START+b*MINUTE, OWN);
    }
    @Test public void samsungExerciseTotalIs392Not26ActiveOr2423Daily() {
        List<HealthWorkoutEnergy.Session> sessions=Arrays.asList(session(SAMSUNG,0,14),session(SAMSUNG,32,72),session(OWN,0,82));
        List<HealthWorkoutEnergy.EnergyRow> rows=Arrays.asList(energy("first",SAMSUNG,0,14,112,true),energy("second",SAMSUNG,32,72,280,true),energy("daily",SAMSUNG,-1360,80,2423,true),energy("active",SAMSUNG,0,14,26,false));
        HealthWorkoutEnergy.Result r=match(sessions,rows,-1,83);
        assertEquals(392,r.calories,0.0001);assertEquals(54*MINUTE,r.coverageMs);
        assertEquals(Collections.singleton(SAMSUNG),r.sources);assertEquals(Collections.singleton("total"),r.types);assertFalse(r.prorated);
    }
    @Test public void duplicateRecordsAndMirroredSourcesDoNotDoubleCount() {
        List<HealthWorkoutEnergy.Session> sessions=Arrays.asList(session(SAMSUNG,0,40),session("mirror",0,40));
        HealthWorkoutEnergy.EnergyRow original=energy("a",SAMSUNG,0,40,280,true);
        HealthWorkoutEnergy.Result r=match(sessions,Arrays.asList(original,original,energy("duplicate",SAMSUNG,0,40,280,true),energy("mirror", "mirror",0,40,280,true)),0,40);
        assertEquals(280,r.calories,0.0001);assertEquals(1,r.recordIds.size());
    }
    @Test public void wholeWorkoutAndDetailedSamplesAreNotAddedTogether() {
        HealthWorkoutEnergy.Result r=match(Collections.singletonList(session(SAMSUNG,0,40)),Arrays.asList(energy("summary",SAMSUNG,0,40,300,true),energy("first",SAMSUNG,0,20,100,true),energy("second",SAMSUNG,20,40,180,true)),0,40);
        assertEquals(280,r.calories,0.0001);assertEquals(2,r.recordIds.size());assertFalse(r.prorated);
    }
    @Test public void pausedWindowsAreClippedAndCanBeAddedAcrossMidnight() {
        List<HealthWorkoutEnergy.Session> sessions=Collections.singletonList(session(SAMSUNG,0,40));
        List<HealthWorkoutEnergy.EnergyRow> rows=Collections.singletonList(energy("a",SAMSUNG,0,40,280,true));
        HealthWorkoutEnergy.Result first=match(sessions,rows,0,10),second=match(sessions,rows,20,40);
        assertEquals(210,first.calories+second.calories,0.0001);assertEquals(30*MINUTE,first.coverageMs+second.coverageMs);assertTrue(first.prorated);assertTrue(second.prorated);
    }
    @Test public void missingDailyAndOwnRecordsNeverBecomeWorkoutCalories() {
        List<HealthWorkoutEnergy.Session> sessions=Arrays.asList(session(SAMSUNG,0,40),session(OWN,0,40));
        assertNull(match(sessions,Arrays.asList(energy("daily",SAMSUNG,-1360,80,2423,true),energy("own",OWN,0,40,280,true),energy("other","other",0,40,280,true)),0,40).calories);
        assertNull(match(Collections.emptyList(),Collections.singletonList(energy("a",SAMSUNG,0,40,280,true)),0,40).calories);
    }
    @Test public void activeEnergyIsUsedOnlyWhenNoSessionTotalCoversThatTime() {
        HealthWorkoutEnergy.Result r=match(Collections.singletonList(session("watch",0,40)),Collections.singletonList(energy("a","watch",0,40,280,false)),0,40);
        assertEquals(280,r.calories,0.0001);assertEquals(Collections.singleton("active"),r.types);
    }
    @Test public void edgesAreToleratedButOnlyActualSessionTimeIsCounted() {
        HealthWorkoutEnergy.Result r=match(Collections.singletonList(session(SAMSUNG,0,40)),Collections.singletonList(energy("a",SAMSUNG,-1,41,294,true)),0,40);
        assertEquals(280,r.calories,0.0001);assertTrue(r.prorated);
    }
    @Test public void invalidRecordsAreMissingAndRealZeroRemainsZero() {
        List<HealthWorkoutEnergy.Session> sessions=Collections.singletonList(session(SAMSUNG,0,40));
        assertNull(match(sessions,Arrays.asList(energy("nan",SAMSUNG,0,40,Double.NaN,true),energy("negative",SAMSUNG,0,40,-1,true),energy("bad",SAMSUNG,40,0,100,true)),0,40).calories);
        assertEquals(0,match(sessions,Collections.singletonList(energy("zero",SAMSUNG,0,40,0,true)),0,40).calories,0);
    }
}
