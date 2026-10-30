package com.repforge.app;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.ArrayList;
import java.util.List;

public class BleScanPolicyTest {
    private List<BleScanPolicy.Candidate> full() {
        List<BleScanPolicy.Candidate> result = new ArrayList<>();
        for (int i = 0; i < BleScanPolicy.CAPACITY; i++) result.add(new BleScanPolicy.Candidate("row-" + i, 0, -50));
        return result;
    }
    @Test public void lateWatchIsRetainedEvenWithWeakSignal() {
        assertEquals("row-0", BleScanPolicy.replacement(full(), new BleScanPolicy.Candidate("watch", 4, -99)));
        assertEquals("row-0", BleScanPolicy.replacement(full(), new BleScanPolicy.Candidate("gt4", 3, -99)));
        assertEquals("row-0", BleScanPolicy.replacement(full(), new BleScanPolicy.Candidate("0201", 1, -99)));
    }
    @Test public void ordinaryNeighbourCannotEvictSelectedWatch() {
        List<BleScanPolicy.Candidate> rows = full();
        rows.set(0, new BleScanPolicy.Candidate("saved", 4, -100));
        rows.set(1, new BleScanPolicy.Candidate("weak", 0, -80));
        assertEquals("weak", BleScanPolicy.replacement(rows, new BleScanPolicy.Candidate("neighbour", 0, -60)));
        assertEquals("", BleScanPolicy.replacement(rows, new BleScanPolicy.Candidate("weak-new", 0, -90)));
    }
    @Test public void smallSignalFluctuationDoesNotChurnList() {
        assertEquals("", BleScanPolicy.replacement(full(), new BleScanPolicy.Candidate("new", 0, -47)));
        assertEquals("row-0", BleScanPolicy.replacement(full(), new BleScanPolicy.Candidate("new", 0, -46)));
    }
    @Test public void LowerPriorityCannotEvictCandidates() {
        List<BleScanPolicy.Candidate> rows = new ArrayList<>();
        for (int i = 0; i < BleScanPolicy.CAPACITY; i++) rows.add(new BleScanPolicy.Candidate("candidate-" + i, 1, -90));
        assertEquals("", BleScanPolicy.replacement(rows, new BleScanPolicy.Candidate("strong-generic", 0, -30)));
    }
    @Test public void lessThanCapacityPreservesAllRows() {
        List<BleScanPolicy.Candidate> rows = full(); rows.remove(0);
        assertNull(BleScanPolicy.replacement(rows, new BleScanPolicy.Candidate("late", 0, -110)));
        assertEquals(BleScanPolicy.CAPACITY - 1, rows.size());
    }
}
