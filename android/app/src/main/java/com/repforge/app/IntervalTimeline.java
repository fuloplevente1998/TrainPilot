package com.repforge.app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finite monotonic workout timeline; no wall clock or Android dependency. */
final class IntervalTimeline {
    static final long MAX_MS=86400000L;
    static final class Step {
        final String exerciseId,setId,name;
        final int work,rest,rounds;
        final Bilateral192.Config bilateral;
        String gpsKey;
        Step(String exerciseId,String setId,String name,int work,int rest,int rounds,String gpsKey){this(exerciseId,setId,name,work,rest,rounds,gpsKey,null);}
        Step(String exerciseId,String setId,String name,int work,int rest,int rounds,String gpsKey,Bilateral192.Config bilateral){
            if(exerciseId==null||exerciseId.isEmpty()||setId==null||setId.isEmpty()||work<1||work>3600||rest<0||rest>3600||rounds<1||rounds>100)throw new IllegalArgumentException("Invalid interval step");
            this.exerciseId=exerciseId;this.setId=setId;this.name=name;this.work=work;this.rest=rest;this.rounds=rounds;this.gpsKey=gpsKey;this.bilateral=bilateral;
        }
        long duration(){return ((bilateral==null?(long)work:(long)bilateral.left+bilateral.right+bilateral.swap)*rounds+(long)rest*(rounds-1))*1000;}
    }
    static final class Phase {
        final int step,round,completed;
        final long elapsedMs,stepElapsedMs,remainingMs,totalMs;
        final String phase,token,side192,restKind192;
        final long phaseDuration192;
        Phase(int step,int round,int completed,long elapsed,long within,long remaining,long total,String phase){this(step,round,completed,elapsed,within,remaining,total,phase,"","",0,null);}
        Phase(int step,int round,int completed,long elapsed,long within,long remaining,long total,String phase,String side,String restKind,long duration,String token){
            this.step=step;this.round=round;this.completed=completed;elapsedMs=elapsed;stepElapsedMs=within;remainingMs=remaining;totalMs=total;this.phase=phase;
            side192=side;restKind192=restKind;phaseDuration192=duration;this.token=token!=null?token:step+":"+round+":"+phase;
        }
    }
    static final class Stage {
        final String phase,side,kind,token;final int round;final long duration;
        Stage(int step,int round,String phase,String side,String kind,long duration){this.round=round;this.phase=phase;this.side=side;this.kind=kind;this.duration=duration;
            token=step+":"+round+":"+phase+(side.isEmpty()?"":":"+side+":"+kind);}
    }
    private final List<Step> steps;
    private final boolean advance,restOnly;
    private final int initialRestSeconds;
    private final ArrayList<List<Stage>> stages=new ArrayList<>();
    private final LinkedHashMap<String,Long> overrides=new LinkedHashMap<>();
    private final long[] lengths;
    private long total,initialRest;
    IntervalTimeline(List<Step> steps,boolean advance){this(steps,advance,0,false);}
    IntervalTimeline(List<Step> steps,boolean advance,int initialRestSeconds,boolean restOnly){this(steps,advance,initialRestSeconds,restOnly,new LinkedHashMap<>());}
    IntervalTimeline(List<Step> steps,boolean advance,int initialRestSeconds,boolean restOnly,Map<String,Long> overrides){
        if(steps==null||steps.isEmpty()||steps.size()>100)throw new IllegalArgumentException("Invalid interval plan");
        if(initialRestSeconds<0||initialRestSeconds>3600||restOnly&&(steps.size()!=1||advance||initialRestSeconds!=0))throw new IllegalArgumentException("Invalid rest plan");
        this.steps=new ArrayList<>(steps);this.advance=advance;this.restOnly=restOnly;this.initialRestSeconds=initialRestSeconds;lengths=new long[steps.size()];
        if(overrides.size()>40200)throw new IllegalArgumentException("Too many phase edits");
        this.overrides.putAll(overrides);rebuild();
    }
    private String initialToken(){Bilateral192.Config c=steps.get(0).bilateral;return c==null?"initial:rest":"0:1:rest:"+c.first+":initial";}
    private long duration(Stage s){return overrides.getOrDefault(s.token,s.duration);}
    private void rebuild(){
        stages.clear();initialRest=overrides.getOrDefault(initialToken(),initialRestSeconds*1000L);total=initialRest;
        for(int i=0;i<steps.size();i++){
            Step s=steps.get(i);ArrayList<Stage> list=new ArrayList<>();
            if(restOnly)list.add(new Stage(i,1,"rest","","",s.rest*1000L));
            else for(int r=1;r<=s.rounds;r++){
                if(s.bilateral!=null){Bilateral192.Config c=s.bilateral;
                    list.add(new Stage(i,r,"work",c.first,"",c.work(c.first)*1000L));
                    list.add(new Stage(i,r,"rest",c.second(),"side",c.swap*1000L));
                    list.add(new Stage(i,r,"work",c.second(),"",c.work(c.second())*1000L));
                    list.add(new Stage(i,r,"rest",c.first,"set",(r<s.rounds||advance&&i<steps.size()-1?s.rest:0)*1000L));
                }else{
                    list.add(new Stage(i,r,"work","","",s.work*1000L));
                    list.add(new Stage(i,r,"rest","","",(r<s.rounds||advance&&i<steps.size()-1?s.rest:0)*1000L));
                }
            }
            lengths[i]=0;for(Stage p:list){long n=duration(p);if(n<0||n>3600000||"work".equals(p.phase)&&n==0)throw new IllegalArgumentException("Invalid phase duration");lengths[i]+=n;}
            stages.add(list);total+=lengths[i];
        }
        if(total>MAX_MS)throw new IllegalArgumentException("Interval plan exceeds 24 hours");
    }
    long duration(int i){return lengths[i];}
    long phaseDurationMs(Phase p){return p.phaseDuration192;}
    Map<String,Long> overrides192(){return new LinkedHashMap<>(overrides);}
    Map<String,Long> freezeThrough192(Phase current){
        Map<String,Long> result=overrides192();result.put(initialToken(),initialRest);if(current.token.equals(initialToken()))return result;
        long position=0;for(Stage s:stages.get(current.step)){if(position>current.stepElapsedMs)break;result.put(s.token,duration(s));position+=duration(s);}return result;
    }
    long targetWork192(int index,String side){long sum=0;for(Stage s:stages.get(index))if("work".equals(s.phase)&&s.side.equals(side))sum+=duration(s);return sum;}
    void adjustRest192(Phase p,long delta){
        if(!p.phase.equals("rest")||p.side192.isEmpty())throw new IllegalArgumentException("Not a bilateral rest");
        long change=Math.max(-p.remainingMs,Math.min(3600000-p.phaseDuration192,delta));Map<String,Long> before=overrides192();
        try{overrides.put(p.token,p.phaseDuration192+change);rebuild();}catch(RuntimeException e){overrides.clear();overrides.putAll(before);rebuild();throw e;}
    }
    Phase at(long milliseconds){
        long elapsed=Math.max(0,Math.min(total,milliseconds)),offset=elapsed;
        if(restOnly)return new Phase(0,1,elapsed==total?1:0,elapsed,0,total-elapsed,total,elapsed==total?"complete":"rest","","",elapsed==total?0:total,null);
        if(elapsed==total){int i=steps.size()-1;return new Phase(i,steps.get(i).rounds,steps.size(),elapsed,duration(i),0,total,"complete");}
        if(elapsed<initialRest){Bilateral192.Config c=steps.get(0).bilateral;return new Phase(0,1,0,elapsed,0,initialRest-elapsed,total,"rest",c==null?"":c.first,c==null?"":"initial",initialRest,initialToken());}
        offset-=initialRest;
        for(int i=0;i<steps.size();i++){
            long length=duration(i);if(offset>=length){offset-=length;continue;}
            long left=offset;for(Stage s:stages.get(i)){long n=duration(s);if(left<n)return new Phase(i,s.round,i,elapsed,offset,n-left,total,s.phase,s.side,s.kind,n,s.token);left-=n;}
        }
        throw new IllegalStateException("Unreachable interval phase");
    }
}
