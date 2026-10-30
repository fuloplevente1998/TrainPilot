package com.repforge.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Collections;

public class IntervalHapticCuesTest {
    private IntervalTimeline rest(int seconds){return new IntervalTimeline(Collections.singletonList(new IntervalTimeline.Step("press","s","Press",1,seconds,1,"")),false,0,true);}
    @Test public void lastThreeSecondsHaveShortCuesThenOneSecondFinish(){
        IntervalTimeline t=rest(30);IntervalHapticCues cues=new IntervalHapticCues();
        long[] at={0,26000,27000,27200,28000,29000,30000,31000};
        int[] expected={100,0,80,0,80,80,1000,0};
        for(int i=0;i<at.length;i++)assertEquals(expected[i],cues.next(t.at(at[i]),true));
    }
    @Test public void pauseAndResumeDoNotReplayConsumedSecond(){
        IntervalTimeline t=rest(5);IntervalHapticCues cues=new IntervalHapticCues();
        assertEquals(100,cues.next(t.at(0),true));assertEquals(80,cues.next(t.at(2000),true));
        assertEquals(0,cues.next(t.at(3000),false));assertEquals(80,cues.next(t.at(3000),true));
        assertEquals(0,cues.next(t.at(3000),true));assertEquals(80,cues.next(t.at(4000),true));
    }
    @Test public void lateCallbackDoesNotReplayMissedSeconds(){
        IntervalTimeline t=rest(30);IntervalHapticCues cues=new IntervalHapticCues();
        cues.next(t.at(0),true);assertEquals(80,cues.next(t.at(29500),true));
        assertEquals(1000,cues.next(t.at(30000),true));assertEquals(0,cues.next(t.at(30000),true));
    }
    @Test public void shortNextPhaseDoesNotCutOffOneSecondTransition(){
        IntervalTimeline t=new IntervalTimeline(Collections.singletonList(new IntervalTimeline.Step("run","s","Run",1,2,2,"")),false);
        IntervalHapticCues cues=new IntervalHapticCues();cues.next(t.at(0),true);
        assertEquals(1000,cues.next(t.at(1000),true));assertEquals(0,cues.next(t.at(1100),true));
        assertEquals(80,cues.next(t.at(2000),true));assertEquals(1000,cues.next(t.at(3000),true));
        assertEquals(1000,cues.next(t.at(4000),true));assertEquals(0,cues.next(t.at(4000),true));
    }
}
