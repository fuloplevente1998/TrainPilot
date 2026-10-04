package com.repforge.app;
import java.util.Set;
/** A daily activity source is selected, never summed with another overlapping source. */
final class HealthDailySource {
 static final String SAMSUNG="com.sec.android.app.shealth";
 static String choose(String requested,String preference,Set<String> available){
  if(!requested.isEmpty())return requested;
  if("auto".equals(preference))return available.contains(SAMSUNG)?SAMSUNG:"";
  return "priority".equals(preference)?"":preference;
 }
}
