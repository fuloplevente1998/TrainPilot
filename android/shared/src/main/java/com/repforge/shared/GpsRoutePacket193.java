package com.repforge.shared;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Immutable, bounded route chunks. Coordinates never belong in a workout/Health snapshot. */
public final class GpsRoutePacket193 {
    public static final int MAX_POINTS=12000, MAX_SEGMENTS=256, CHUNK_POINTS=32, MAX_PACKET_BYTES=12000;
    private GpsRoutePacket193() {}
    private static void require(boolean value) { if(!value)throw new IllegalArgumentException("Invalid route packet"); }
    public static String digest(String value) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();for(byte b:hash)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    public static String key(String workoutId,String setId) throws Exception {
        require(text(workoutId,512)&&text(setId,200));
        return "wear193:"+digest(new JSONArray().put(workoutId).put(setId).toString());
    }
    public static boolean id(String value) { return value!=null&&value.matches("[A-Za-z0-9._:-]{1,128}"); }
    private static boolean text(String value,int max) { return value!=null&&!value.isEmpty()&&value.length()<=max; }
    private static long integer(JSONObject value,String name,long min,long max) throws Exception {
        Object raw=value.get(name);require(raw instanceof Number);
        double n=((Number)raw).doubleValue();long result=((Number)raw).longValue();
        require(Double.isFinite(n)&&n==result&&result>=min&&result<=max);return result;
    }
    private static double number(JSONObject value,String name,double min,double max) throws Exception {
        Object raw=value.get(name);require(raw instanceof Number);double result=((Number)raw).doubleValue();
        require(Double.isFinite(result)&&result>=min&&result<=max);return result;
    }
    /** A grant is local durable consent, never a field supplied by a remote packet. */
    public static JSONObject validate(JSONObject input,JSONObject grant,String origin,String localNode) throws Exception {
        require(input!=null&&grant!=null&&input.toString().getBytes(StandardCharsets.UTF_8).length<=MAX_PACKET_BYTES);
        require(integer(input,"schema",1,1)==1&&id(origin)&&id(localNode)&&!origin.equals(localNode));
        String workout=input.getString("workoutId"),set=input.getString("setId"),route=input.getString("routeKey");
        require(text(workout,512)&&text(set,200)&&text(input.getString("exerciseId"),200)&&id(input.getString("streamId")));
        require(route.equals(key(workout,set))&&origin.equals(input.getString("sourceNode"))&&localNode.equals(input.getString("targetNode")));
        require(route.equals(grant.optString("routeKey"))&&workout.equals(grant.optString("workoutId"))&&set.equals(grant.optString("setId"))
            &&input.getString("exerciseId").equals(grant.optString("exerciseId"))&&origin.equals(grant.optString("sourceNode"))
            &&localNode.equals(grant.optString("targetNode"))&&integer(input,"ownerEpoch193",0,Long.MAX_VALUE)==grant.getLong("ownerEpoch193"));
        integer(input,"sequence",1,MAX_POINTS);require(input.get("closed") instanceof Boolean&&input.get("partial") instanceof Boolean);
        JSONArray points=input.getJSONArray("points");require(points.length()<=CHUNK_POINTS&&(points.length()>0||input.getBoolean("closed")));
        long elapsed=-1,epoch=-1,segment=0;double lat=0,lon=0;
        for(int i=0;i<points.length();i++) {
            JSONObject point=points.getJSONObject(i);
            double nextLat=number(point,"latitude",-90,90),nextLon=number(point,"longitude",-180,180);number(point,"accuracyMeters",0,35);
            long nextElapsed=integer(point,"elapsedMillis",0,Long.MAX_VALUE),nextEpoch=integer(point,"epochMillis",1,Long.MAX_VALUE);
            long nextSegment=integer(point,"segment",1,MAX_SEGMENTS);
            require(nextElapsed>elapsed&&nextEpoch>epoch&&nextSegment>=segment);
            if(i>0&&nextSegment==segment)require(nextElapsed-elapsed<=30000&&distance(lat,lon,nextLat,nextLon)/((nextElapsed-elapsed)/1000d)<=20);
            elapsed=nextElapsed;epoch=nextEpoch;segment=nextSegment;lat=nextLat;lon=nextLon;
        }
        // Whitelist prevents arbitrary remote fields from becoming persistent or entering a receipt.
        JSONObject clean=new JSONObject();
        for(String field:new String[]{"schema","routeKey","workoutId","exerciseId","setId","sourceNode","targetNode","ownerEpoch193","streamId","sequence","closed","partial"})clean.put(field,input.get(field));
        JSONArray copied=new JSONArray();for(int i=0;i<points.length();i++) {
            JSONObject point=new JSONObject();for(String field:new String[]{"latitude","longitude","accuracyMeters","elapsedMillis","epochMillis","segment"})point.put(field,points.getJSONObject(i).get(field));copied.put(point);
        }
        return clean.put("points",copied);
    }
    public static double distance(double lat,double lon,double otherLat,double otherLon) {
        double a=Math.toRadians(otherLat-lat),b=Math.toRadians(otherLon-lon);
        double x=Math.sin(a/2)*Math.sin(a/2)+Math.cos(Math.toRadians(lat))*Math.cos(Math.toRadians(otherLat))*Math.sin(b/2)*Math.sin(b/2);
        return 6371000*2*Math.atan2(Math.sqrt(x),Math.sqrt(Math.max(0,1-x)));
    }
}
