package com.repforge.shared;

import org.json.JSONObject;

/** GNSS fixes only. Rejected signal/ownership gaps never contribute distance or a connecting line. */
public final class GpsRouteSampler193 {
    public interface Writer { void append(JSONObject point) throws Exception; }
    private final Writer writer;
    private JSONObject anchor;
    private int points,segment;
    private long lastElapsed=-1,lastEpoch=-1;
    private double meters;
    private boolean partial;
    public GpsRouteSampler193(Writer writer) { this.writer=writer; }
    public void breakSegment() { anchor=null; }
    public int pointCount() { return points; }
    public int segmentCount() { return segment; }
    public double distanceMeters() { return meters; }
    public boolean partial() { return partial; }
    public boolean add(double latitude,double longitude,double accuracy,long elapsed,long epoch,double speed) throws Exception {
        if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180
            ||!Double.isFinite(accuracy)||accuracy<0||accuracy>35||elapsed<0||epoch<=0
            ||Double.isFinite(speed)&&(speed<0||speed>20)) { breakSegment();return false; }
        if(elapsed<=lastElapsed||epoch<=lastEpoch)return false;
        if(anchor!=null&&elapsed-anchor.getLong("elapsedMillis")>30000)breakSegment();
        double delta=0;
        if(anchor!=null) {
            delta=GpsRoutePacket193.distance(anchor.getDouble("latitude"),anchor.getDouble("longitude"),latitude,longitude);
            if(delta/((elapsed-anchor.getLong("elapsedMillis"))/1000d)>20) { breakSegment();return false; }
            double minimum=Math.max(3,Math.min(15,(anchor.getDouble("accuracyMeters")+accuracy)*.25));
            if(delta<minimum||Double.isFinite(speed)&&speed<.5)return false;
        }
        if(points>=GpsRoutePacket193.MAX_POINTS||anchor==null&&segment>=GpsRoutePacket193.MAX_SEGMENTS) { partial=true;return false; }
        int nextSegment=anchor==null?segment+1:segment;
        JSONObject point=new JSONObject().put("latitude",latitude).put("longitude",longitude).put("accuracyMeters",accuracy)
            .put("elapsedMillis",elapsed).put("epochMillis",epoch).put("segment",nextSegment);
        try { writer.append(new JSONObject(point.toString())); }
        catch(Exception error) { partial=true;breakSegment();throw error; }
        anchor=point;segment=nextSegment;points++;meters+=delta;lastElapsed=elapsed;lastEpoch=epoch;return true;
    }
}
