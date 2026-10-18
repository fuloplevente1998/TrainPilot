package com.repforge.app;

import java.util.List;
import com.repforge.shared.IntervalTimeline193;

/** Phone compatibility name; both devices use the same finite timeline calculation. */
final class IntervalTimeline extends IntervalTimeline193 {
    IntervalTimeline(List<Step> steps,boolean advance){super(steps,advance);}
    IntervalTimeline(List<Step> steps,boolean advance,int initialRestSeconds,boolean restOnly){
        super(steps,advance,initialRestSeconds,restOnly);
    }
}
