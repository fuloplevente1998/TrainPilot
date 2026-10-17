package com.repforge.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Versioned configuration and projection for timed left/right work. No inferred legacy data. */
final class Bilateral192 {
    static final class Config {
        final String first;
        final int left,right,swap,setRest;
        Config(String first,int left,int right,int swap) {this(first,left,right,swap,-1);}
        Config(String first,int left,int right,int swap,int setRest) {
            if(!("left".equals(first)||"right".equals(first))||left<1||left>3600||right<1||right>3600||swap<0||swap>3600||setRest< -1||setRest>3600)
                throw new IllegalArgumentException("invalidBilateralConfig");
            this.first=first;this.left=left;this.right=right;this.swap=swap;this.setRest=setRest;
        }
        int work(String side){return "left".equals(side)?left:right;}
        String second(){return "left".equals(first)?"right":"left";}
        JSONObject json()throws JSONException{JSONObject out=new JSONObject().put("schema",1).put("first",first).put("leftSeconds",left).put("rightSeconds",right).put("sideRestSeconds",swap);if(setRest>=0)out.put("setRestSeconds",setRest);return out;}
    }
    static Config parse(JSONObject value)throws JSONException {
        if(value==null)return null;
        if(PhoneWorkoutRecording.version(value,"schema")!=1)throw new IllegalArgumentException("unsupportedBilateralProtocol");
        return new Config(value.getString("first"),integer(value,"leftSeconds",1),integer(value,"rightSeconds",1),integer(value,"sideRestSeconds",0),value.has("setRestSeconds")?integer(value,"setRestSeconds",0):-1);
    }
    private static int integer(JSONObject v,String key,int min)throws JSONException {
        long n=PhoneWorkoutRecording.version(v,key);if(n<min||n>3600)throw new IllegalArgumentException("invalidBilateralConfig");return (int)n;
    }
    static boolean timed(JSONObject e){return "mp".equals(e.optString("repUnit"))||"mp/oldal".equals(e.optString("repUnit"));}
    static Config config(JSONObject e,int work)throws JSONException {
        if(!"mp/oldal".equals(e.optString("repUnit")))return null;
        Config c=parse(e.optJSONObject("bilateral192"));return c!=null?c:new Config("left",work,work,0);
    }
    static String label(String language,IntervalTimeline.Phase p){
        String side="left".equals(p.side192)?DistanceTrackingService.text(language,"Bal","Left","Links","Stânga"):DistanceTrackingService.text(language,"Jobb","Right","Rechts","Dreapta");
        if("rest".equals(p.phase))return ("side".equals(p.restKind192)?DistanceTrackingService.text(language,"Oldalcsere","Switch rest","Wechselpause","Pauză schimb"):DistanceTrackingService.text(language,"Sorozatpihenő","Set rest","Satzpause","Pauză serie"))+" → "+side;
        return side+" · "+DistanceTrackingService.text(language,"Munka","Work","Arbeit","Efort");
    }
    static JSONObject values(JSONObject row,JSONObject part,String key)throws JSONException {
        Config c=parse(row.getJSONObject("bilateral192"));int rounds=row.getInt("rounds");
        long left=part.getLong("leftWorkMs"),right=part.getLong("rightWorkMs");
        boolean skipped=part.optBoolean("skippedWork192");
        JSONObject state=new JSONObject().put("schema",1).put("key",key).put("skipped",skipped)
            .put("left",left>=row.optLong("targetLeftMs192",c.left*1000L*rounds)?"complete":left>0?"partial":part.optBoolean("skippedLeft192")?"skipped":"pending")
            .put("right",right>=row.optLong("targetRightMs192",c.right*1000L*rounds)?"complete":right>0?"partial":part.optBoolean("skippedRight192")?"skipped":"pending")
            .put("timeLogKey",key);
        long l=row.optLong("baseLeftSeconds")+left/1000,r=row.optLong("baseRightSeconds")+right/1000;
        return new JSONObject().put("leftSeconds",l).put("rightSeconds",r).put("reps",String.valueOf(Math.min(l,r)))
            .put("done",!skipped&&left>=row.optLong("targetLeftMs192",c.left*1000L*rounds)&&right>=row.optLong("targetRightMs192",c.right*1000L*rounds)).put("sideState192",state);
    }
}
