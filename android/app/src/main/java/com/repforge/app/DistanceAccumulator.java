package com.repforge.app;

/** GNSS distance only: coordinates are transient and never persisted. */
final class DistanceAccumulator {
    private double lat,lon,accuracy,total;
    private long at;
    private boolean anchor;
    double totalMeters(){return total;}
    boolean add(double latitude,double longitude,double accuracyMeters,long elapsedMillis,double speedMps){
        if(!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180
                ||!Double.isFinite(accuracyMeters)||accuracyMeters<0||accuracyMeters>35||elapsedMillis<0)return false;
        if(!anchor){remember(latitude,longitude,accuracyMeters,elapsedMillis);return true;}
        long dt=elapsedMillis-at;if(dt<=0)return false;
        // Missing signal is a gap, not a straight-line route segment.
        if(dt>30000){remember(latitude,longitude,accuracyMeters,elapsedMillis);return true;}
        double meters=distance(lat,lon,latitude,longitude);
        if(meters/(dt/1000d)>20)return false;
        double threshold=Math.max(3,Math.min(15,(accuracy+accuracyMeters)*0.25));
        if(meters<threshold||Double.isFinite(speedMps)&&speedMps<0.5)return true;
        total+=meters;remember(latitude,longitude,accuracyMeters,elapsedMillis);return true;
    }
    private void remember(double latitude,double longitude,double acc,long millis){lat=latitude;lon=longitude;accuracy=acc;at=millis;anchor=true;}
    static double distance(double a,double b,double c,double d){
        double latDelta=Math.toRadians(c-a),lonDelta=Math.toRadians(d-b);
        double x=Math.sin(latDelta/2)*Math.sin(latDelta/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(lonDelta/2)*Math.sin(lonDelta/2);
        return 6371000d*2*Math.atan2(Math.sqrt(x),Math.sqrt(Math.max(0,1-x)));
    }
}
