package com.repforge.app;

/** One cue per countdown second; delayed callbacks never replay missed cues. */
final class IntervalHapticCues {
    private String token="";
    private long second=-1,quietUntil=-1;
    int next(IntervalTimeline.Phase p,boolean running){
        if(!running)return 0;
        long remaining=(p.remainingMs+999)/1000;
        if(!p.token.equals(token)){
            boolean transition=!token.isEmpty();token=p.token;second=remaining;
            int duration=transition||"complete".equals(p.phase)?1000:remaining>=1&&remaining<=3?80:100;
            quietUntil=p.elapsedMs+duration;
            return duration;
        }
        if(!"complete".equals(p.phase)&&remaining>=1&&remaining<=3&&remaining!=second){
            second=remaining;
            if(p.elapsedMs>=quietUntil){quietUntil=p.elapsedMs+80;return 80;}
        }
        return 0;
    }
}
