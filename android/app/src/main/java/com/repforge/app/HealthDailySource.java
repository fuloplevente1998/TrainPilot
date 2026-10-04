package com.repforge.app;
import java.util.Map;
import java.util.Set;
/** A daily activity source is selected, never summed with another overlapping source. */
final class HealthDailySource {
 static final String SAMSUNG="com.sec.android.app.shealth";
 static String choose(String requested,String preference,Set<String> available){
  if(!requested.isEmpty())return requested;
  if("auto".equals(preference)||"priority".equals(preference))return "";
  return preference;
 }
 static String mostComplete(Map<String,Long> totals){
  String best="";long bestValue=-1;
  for(Map.Entry<String,Long> e:new java.util.TreeMap<>(totals).entrySet()){
   Long value=e.getValue();if(e.getKey()==null||e.getKey().isEmpty()||value==null||value<0)continue;
   if(value>bestValue){best=e.getKey();bestValue=value;}
  }
  return best;
 }
}
