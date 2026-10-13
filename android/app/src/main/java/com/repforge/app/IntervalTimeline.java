package com.repforge.app;

import java.util.List;

/** Finite monotonic workout timeline; no wall clock or Android dependency. */
final class IntervalTimeline {
    static final long MAX_MS=86400000L;
    static final class Step {
        final String exerciseId,setId,name;
        final int work,rest,rounds;
        String gpsKey;
        Step(String exerciseId,String setId,String name,int work,int rest,int rounds,String gpsKey){
            if(exerciseId==null||exerciseId.isEmpty()||setId==null||setId.isEmpty()||work<1||work>3600||rest<0||rest>3600||rounds<1||rounds>100)throw new IllegalArgumentException("Invalid interval step");
            this.exerciseId=exerciseId;this.setId=setId;this.name=name;this.work=work;this.rest=rest;this.rounds=rounds;this.gpsKey=gpsKey;
        }
        long duration(){return ((long)work*rounds+(long)rest*(rounds-1))*1000;}
    }
    static final class Phase {
        final int step,round,completed;
        final long elapsedMs,stepElapsedMs,remainingMs,totalMs;
        final String phase,token;
        Phase(int step,int round,int completed,long elapsed,long within,long remaining,long total,String phase){this.step=step;this.round=round;this.completed=completed;elapsedMs=elapsed;stepElapsedMs=within;remainingMs=remaining;totalMs=total;this.phase=phase;token=step+":"+round+":"+phase;}
    }
    private final List<Step> steps;
    private final boolean advance;
    private final boolean restOnly;
    private final long initialRest;
    private final long total;
    IntervalTimeline(List<Step> steps,boolean advance){
        this(steps,advance,0,false);
    }
    IntervalTimeline(List<Step> steps,boolean advance,int initialRestSeconds,boolean restOnly){
        if(steps==null||steps.isEmpty()||steps.size()>100)throw new IllegalArgumentException("Invalid interval plan");
        if(initialRestSeconds<0||initialRestSeconds>3600||restOnly&&(steps.size()!=1||advance||initialRestSeconds!=0))throw new IllegalArgumentException("Invalid rest plan");
        this.steps=steps;this.advance=advance;this.restOnly=restOnly;initialRest=initialRestSeconds*1000L;
        long sum=initialRest;for(int i=0;i<steps.size();i++){sum+=duration(i);if(sum>MAX_MS)throw new IllegalArgumentException("Interval plan exceeds 24 hours");}total=sum;
    }
    long duration(int i){return restOnly?steps.get(i).rest*1000L:steps.get(i).duration()+(advance&&i<steps.size()-1?steps.get(i).rest*1000L:0);}
    long phaseDurationMs(Phase p){
        if("work".equals(p.phase))return steps.get(p.step).work*1000L;
        if(!"rest".equals(p.phase))return 0;
        if(!restOnly&&p.elapsedMs<initialRest)return initialRest;
        return steps.get(p.step).rest*1000L;
    }
    Phase at(long milliseconds){
        long elapsed=Math.max(0,Math.min(total,milliseconds)),offset=elapsed;
        if(restOnly)return new Phase(0,1,elapsed==total?1:0,elapsed,0,total-elapsed,total,elapsed==total?"complete":"rest");
        if(elapsed==total){int i=steps.size()-1;return new Phase(i,steps.get(i).rounds,steps.size(),elapsed,duration(i),0,total,"complete");}
        if(elapsed<initialRest)return new Phase(0,1,0,elapsed,0,initialRest-elapsed,total,"rest");
        offset-=initialRest;
        for(int i=0;i<steps.size();i++){
            long length=duration(i);if(offset>=length){offset-=length;continue;}
            Step s=steps.get(i);long left=offset;
            for(int round=1;round<=s.rounds;round++){
                long work=s.work*1000L;if(left<work)return new Phase(i,round,i,elapsed,offset,work-left,total,"work");left-=work;
                if(round<s.rounds||advance&&i<steps.size()-1){long rest=s.rest*1000L;if(left<rest)return new Phase(i,round,i,elapsed,offset,rest-left,total,"rest");left-=rest;}
            }
        }
        throw new IllegalStateException("Unreachable interval phase");
    }
}
