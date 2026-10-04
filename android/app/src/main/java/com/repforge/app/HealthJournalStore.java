package com.repforge.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

/** The primary health store. The WebView only receives bounded projections/pages. */
final class HealthJournalStore extends SQLiteOpenHelper {
    static final String HC = "health_connect", BLE = "ble_watch", LEGACY = "legacy";
    private static final Object LOCK = new Object();
    private static HealthJournalStore instance;
    static HealthJournalStore get(Context context) {
        synchronized (LOCK) {
            if (instance == null) instance = new HealthJournalStore(context.getApplicationContext());
            return instance;
        }
    }
    HealthJournalStore(Context context) { super(context, "health-journal.db", null, 1); }
    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
        try (Cursor result = db.rawQuery("PRAGMA secure_delete=ON", null)) { result.moveToFirst(); }
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE records (row_id INTEGER PRIMARY KEY, channel TEXT NOT NULL, source TEXT NOT NULL, type TEXT NOT NULL, record_id TEXT NOT NULL, sort_ms INTEGER NOT NULL, modified_ms INTEGER NOT NULL, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, day TEXT NOT NULL, json TEXT NOT NULL, UNIQUE(channel,type,record_id))");
        db.execSQL("CREATE INDEX records_page ON records(day DESC,sort_ms DESC,row_id DESC)");
        db.execSQL("CREATE INDEX records_source ON records(channel,source,day DESC,row_id DESC)");
        db.execSQL("CREATE TABLE days (day TEXT NOT NULL, channel TEXT NOT NULL, source TEXT NOT NULL, json TEXT NOT NULL, PRIMARY KEY(day,channel,source))");
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
        db.execSQL("CREATE TABLE deletions (record_id TEXT PRIMARY KEY, deleted_ms INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE dirty_days (day TEXT PRIMARY KEY)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported health schema"); }
    private interface Work<T> { T run(SQLiteDatabase db) throws Exception; }
    private <T> T transaction(Work<T> work) throws Exception {
        synchronized (LOCK) {
            SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
            try { T value = work.run(db); db.setTransactionSuccessful(); return value; }
            finally { db.endTransaction(); }
        }
    }
    private String meta(SQLiteDatabase db, String key, String fallback) {
        try (Cursor c = db.rawQuery("SELECT value FROM meta WHERE key=?", new String[]{key})) { return c.moveToFirst() ? c.getString(0) : fallback; }
    }
    private void putMeta(SQLiteDatabase db, String key, String value) {
        ContentValues v = new ContentValues(); v.put("key", key); v.put("value", value);
        db.insertWithOnConflict("meta", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }
    long generation() { synchronized (LOCK) { return Long.parseLong(meta(getReadableDatabase(), "generation", "0")); } }
    private void writable(SQLiteDatabase db, long generation) {
        if (Long.parseLong(meta(db, "generation", "0")) != generation || !meta(db, "restore_pending", "").isEmpty())
            throw new IllegalStateException("Health data changed; retry after restore/reopen");
    }
    static String day(long time) { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(time)); }
    static String iso(long time) { SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US); f.setTimeZone(TimeZone.getTimeZone("UTC")); return f.format(new Date(time)); }
    static boolean validDay(String day) {
        if (day == null || !day.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        try { SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US); f.setLenient(false); return f.format(f.parse(day)).equals(day); } catch (Exception e) { return false; }
    }
    private static long time(JSONObject r, String key) throws Exception {
        long value = r.getLong(key); if (value < 0 || value > 4102444800000L) throw new IllegalArgumentException("Invalid health time"); return value;
    }
    private static String text(JSONObject r, String key, int max) throws Exception {
        String value = r.getString(key); if (value.isEmpty() || value.length() > max || value.indexOf('\0') >= 0) throw new IllegalArgumentException("Invalid health identity"); return value;
    }
    private void upsert(SQLiteDatabase db, JSONObject r) throws Exception {
        String channel = text(r,"channel",32), source = text(r,"source",255), type = text(r,"type",80), id = text(r,"recordId",200);
        if (!channel.equals(HC) && !channel.equals(BLE)) throw new IllegalArgumentException("Invalid health channel");
        long modified = time(r,"lastModifiedMs"), start = time(r,"startMs"), end = time(r,"endMs");
        if (end < start || !validDay(r.getString("day")) || r.toString().length() > 2*1024*1024) throw new IllegalArgumentException("Invalid health record");
        r.getJSONObject("data");
        if (channel.equals(HC)) try (Cursor c = db.rawQuery("SELECT deleted_ms FROM deletions WHERE record_id=?",new String[]{id})) {
            if (c.moveToFirst() && c.getLong(0) >= modified) return;
        }
        ContentValues v = new ContentValues(); v.put("channel",channel); v.put("source",source); v.put("type",type); v.put("record_id",id);
        v.put("sort_ms",type.equals("SleepSessionRecord")?end:start); v.put("modified_ms",modified); v.put("start_ms",start); v.put("end_ms",end); v.put("day",r.getString("day")); v.put("json",r.toString());
        db.insertWithOnConflict("records", null, v, SQLiteDatabase.CONFLICT_IGNORE);
        db.update("records",v,"channel=? AND type=? AND record_id=? AND modified_ms<=?",new String[]{channel,type,id,Long.toString(modified)});
    }
    void importRecords(JSONArray records, long generation) throws Exception {
        transaction(db -> { writable(db,generation); for(int i=0;i<records.length();i++) upsert(db,records.getJSONObject(i)); return null; });
    }
    void reconcileRecords(JSONArray records,String type,long start,long end,long readAt,long generation) throws Exception {
        transaction(db->{
            writable(db,generation);Set<String> seen=new HashSet<>();for(int i=0;i<records.length();i++){JSONObject r=records.getJSONObject(i);seen.add(r.getString("recordId"));upsert(db,r);}
            // A complete recent query can reconcile contained records after token expiry.
            // Older or cross-boundary records require an explicit HC deletion log.
            if(start>=readAt-28L*86400000){List<String[]> removed=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT record_id,day FROM records WHERE channel=? AND type=? AND start_ms>=? AND start_ms<? AND end_ms<=? AND modified_ms<=?",new String[]{HC,type,Long.toString(start),Long.toString(end),Long.toString(end),Long.toString(readAt)})){while(c.moveToNext())if(!seen.contains(c.getString(0)))removed.add(new String[]{c.getString(0),c.getString(1)});}
                for(String[] row:removed){db.delete("records","channel=? AND type=? AND record_id=?",new String[]{HC,type,row[0]});ContentValues v=new ContentValues();v.put("record_id",row[0]);v.put("deleted_ms",readAt);db.insertWithOnConflict("deletions",null,v,SQLiteDatabase.CONFLICT_REPLACE);v=new ContentValues();v.put("day",row[1]);db.insertWithOnConflict("dirty_days",null,v,SQLiteDatabase.CONFLICT_IGNORE);}}
            return null;
        });
    }
    JSONArray applyChanges(JSONArray records, JSONArray deleted, long generation) throws Exception {
        return transaction(db -> {
            writable(db,generation); Set<String> dirty = new TreeSet<>();
            for (int i=0;i<deleted.length();i++) {
                JSONObject d=deleted.getJSONObject(i); String id=text(d,"recordId",200); long at=time(d,"deletedMs");
                try(Cursor c=db.rawQuery("SELECT day FROM records WHERE channel=? AND record_id=?",new String[]{HC,id})) { while(c.moveToNext()) dirty.add(c.getString(0)); }
                ContentValues v=new ContentValues(); v.put("record_id",id); v.put("deleted_ms",at);
                // Keep the newest deletion even if a changelog page is retried out of order.
                db.insertWithOnConflict("deletions",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                db.update("deletions",v,"record_id=? AND deleted_ms<=?",new String[]{id,Long.toString(at)});
                db.delete("records","channel=? AND record_id=? AND modified_ms<=?",new String[]{HC,id,Long.toString(at)});
            }
            for(int i=0;i<records.length();i++) {
                JSONObject r=records.getJSONObject(i);
                try(Cursor c=db.rawQuery("SELECT day FROM records WHERE channel=? AND type=? AND record_id=?",new String[]{HC,r.getString("type"),r.getString("recordId")})) { if(c.moveToFirst())dirty.add(c.getString(0)); }
                upsert(db,r); dirty.add(r.getString("day"));
            }
            for(String day:dirty) { ContentValues v=new ContentValues();v.put("day",day);db.insertWithOnConflict("dirty_days",null,v,SQLiteDatabase.CONFLICT_IGNORE); }
            return new JSONArray(dirty);
        });
    }
    private void saveDay(SQLiteDatabase db, String day, String channel, String source, JSONObject data) throws Exception {
        if(!validDay(day))throw new IllegalArgumentException("Invalid day");
        ContentValues v=new ContentValues();v.put("day",day);v.put("channel",channel);v.put("source",source);v.put("json",data.toString());
        db.insertWithOnConflict("days",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    void healthDay(String day, JSONObject result, long generation) throws Exception {
        transaction(db -> {
            writable(db,generation); JSONObject out=new JSONObject(result.toString());
            boolean partial=out.optJSONArray("warnings")!=null&&out.getJSONArray("warnings").length()>0;
            if(partial)try(Cursor c=db.rawQuery("SELECT json FROM days WHERE day=? AND channel=? AND source='@aggregate'",new String[]{day,HC})) {
                if(c.moveToFirst()) { JSONObject old=new JSONObject(c.getString(0)); for(Iterator<String> k=old.keys();k.hasNext();) { String key=k.next();Object value=out.opt(key);if(value==null||value==JSONObject.NULL||value instanceof JSONArray&&((JSONArray)value).length()==0)out.put(key,old.get(key)); } }
                else {ContentValues dirty=new ContentValues();dirty.put("day",day);db.insertWithOnConflict("dirty_days",null,dirty,SQLiteDatabase.CONFLICT_IGNORE);return null;}
            }
            out.put("day",day);out.put("partial",partial);out.put("aggregateOrigin","health_connect_priority");
            saveDay(db,day,HC,"@aggregate",out);
            if(!partial)db.delete("dirty_days","day=?",new String[]{day});return null;
        });
    }
    void watchSteps(String deviceId, String name, long count, long requestAt, long observedAt, long generation) throws Exception {
        if(count<0||count>1000000||!day(requestAt).equals(day(observedAt)))throw new IllegalArgumentException("Watch reading crossed midnight or is invalid");
        transaction(db -> {
            writable(db,generation); if(!Boolean.parseBoolean(meta(db,"logWatchSteps","false")))return null;
            JSONObject data=new JSONObject().put("steps",count).put("unit","steps").put("observedAt",iso(observedAt)).put("dateBasis","phone_observation").put("deviceName",name);
            JSONObject r=new JSONObject().put("channel",BLE).put("source",deviceId).put("type","StepsSnapshot").put("recordId",deviceId+":"+day(observedAt)).put("lastModifiedMs",observedAt).put("startMs",observedAt).put("endMs",observedAt).put("day",day(observedAt)).put("data",data);
            upsert(db,r); saveDay(db,day(observedAt),BLE,deviceId,new JSONObject(data.toString()).put("day",day(observedAt)).put("readAt",iso(observedAt)));
            return null;
        });
    }
    JSONObject initialize(JSONObject legacy) throws Exception {
        transaction(db -> {
            if(!meta(db,"restore_pending","").isEmpty())return null;
            if(!meta(db,"migrated_v1","false").equals("true")) {
                JSONObject days=legacy==null?null:legacy.optJSONObject("days");
                if(days!=null)for(Iterator<String> k=days.keys();k.hasNext();) { String day=k.next();if(!validDay(day))throw new IllegalArgumentException("Invalid legacy day");saveDay(db,day,LEGACY,"unknown",days.getJSONObject(day)); }
                JSONArray recovery=legacy==null?null:legacy.optJSONArray("recoveryHistory");
                if(recovery!=null)for(int i=0;i<recovery.length();i++){JSONObject r=recovery.getJSONObject(i);String day=r.optString("day");if(!validDay(day))continue;JSONObject d=days==null?null:days.optJSONObject(day);if(d==null)d=new JSONObject();d.put("legacyRecovery",r);if(!d.has("hrvRmssdMs"))d.put("hrvRmssdMs",r.opt("hrvRmssdMs"));saveDay(db,day,LEGACY,"unknown",d);}
                if(legacy!=null)for(String key:new String[]{"lastSyncAt","lastFullSyncAt","lastAttemptAt"})if(legacy.has(key)&&!legacy.isNull(key))putMeta(db,key,legacy.getString(key));
                putMeta(db,"migrated_v1","true");
            } return null;
        });return projection();
    }
    JSONObject preferences() throws Exception {
        synchronized(LOCK) { SQLiteDatabase db=getReadableDatabase();return new JSONObject().put("primary",meta(db,"primary",HC)).put("autoOnOpen",Boolean.parseBoolean(meta(db,"autoOnOpen","false"))).put("logWatchSteps",Boolean.parseBoolean(meta(db,"logWatchSteps","false"))); }
    }
    void setPreferences(JSONObject preferences) throws Exception {
        transaction(db->{writable(db,generation());for(String key:new String[]{"primary","autoOnOpen","logWatchSteps"})if(preferences.has(key)) { String value=key.equals("primary")?preferences.getString(key):Boolean.toString(preferences.getBoolean(key));if(key.equals("primary")&&!value.equals(HC)&&!value.equals(BLE))throw new IllegalArgumentException("Invalid primary source");putMeta(db,key,value); }return null;});
    }
    void syncMeta(JSONObject meta) throws Exception {
        transaction(db->{writable(db,generation());for(String key:new String[]{"lastSyncAt","lastFullSyncAt","lastAttemptAt"})if(meta.has(key)&&!meta.isNull(key))putMeta(db,key,meta.getString(key));return null;});
    }
    JSONObject projection() throws Exception {
        synchronized(LOCK) {
            SQLiteDatabase db=getReadableDatabase();JSONObject days=new JSONObject();String primary=meta(db,"primary",HC);
            List<String> keys=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT DISTINCT day FROM days ORDER BY day DESC LIMIT 30",null)){while(c.moveToNext())keys.add(c.getString(0));}
            for(String day:keys) {
                JSONObject hc=null,old=null,ble=null;String watchSource=null;
                try(Cursor c=db.rawQuery("SELECT channel,source,json FROM days WHERE day=?",new String[]{day})) {while(c.moveToNext()){String channel=c.getString(0);JSONObject value=new JSONObject(c.getString(2));if(channel.equals(HC))hc=value;else if(channel.equals(LEGACY))old=value;else if(ble==null||value.optString("readAt").compareTo(ble.optString("readAt"))>0){ble=value;watchSource=c.getString(1);}}}
                JSONObject out=hc!=null?hc:old!=null?old:new JSONObject();
                if(hc!=null)out.put("sleepSessions",sleepSessions(db,day));
                if(ble!=null&&(primary.equals(BLE)||out.isNull("steps"))) {out.put("steps",ble.get("steps"));out.put("stepsSource",BLE);out.put("stepsObservedAt",ble.optString("readAt"));out.put("stepsDeviceId",watchSource);}
                else if(!out.isNull("steps"))out.put("stepsSource",hc!=null?HC:LEGACY);
                try(Cursor dirty=db.rawQuery("SELECT day FROM dirty_days WHERE day=?",new String[]{day})){if(dirty.moveToFirst())out.put("partial",true);}
                out.put("day",day);days.put(day,out);
            }
            JSONObject out=new JSONObject().put("version",1).put("days",days).put("preferences",preferences()).put("nativeJournal",true).put("warnings",new JSONArray());
            for(String key:new String[]{"lastSyncAt","lastFullSyncAt","lastAttemptAt"}){String value=meta(db,key,"");out.put(key,value.isEmpty()?JSONObject.NULL:value);}
            return out;
        }
    }
    private JSONArray sleepSessions(SQLiteDatabase db,String day) throws Exception {
        JSONArray result=new JSONArray();try(Cursor c=db.rawQuery("SELECT json FROM records WHERE channel=? AND type='SleepSessionRecord' AND day=? ORDER BY sort_ms DESC LIMIT 100",new String[]{HC,day})){while(c.moveToNext()){
            JSONObject r=new JSONObject(c.getString(0)),data=r.getJSONObject("data");JSONArray stages=new JSONArray(),raw=data.optJSONArray("stages");if(raw!=null)for(int i=0;i<raw.length();i++){JSONObject s=raw.getJSONObject(i);stages.put(new JSONObject().put("start",iso(s.getLong("startMs"))).put("end",iso(s.getLong("endMs"))).put("type",s.getInt("type")).put("isAsleep",s.getBoolean("isAsleep")));}
            result.put(new JSONObject().put("id",r.getString("recordId")).put("source",r.getString("source")).put("start",iso(r.getLong("startMs"))).put("end",iso(r.getLong("endMs"))).put("durationMinutes",(r.getLong("endMs")-r.getLong("startMs"))/60000.0).put("stages",stages));
        }}return result;
    }
    JSONArray dirtyDays() { synchronized(LOCK) { JSONArray days=new JSONArray();try(Cursor c=getReadableDatabase().rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 500",null)){while(c.moveToNext())days.put(c.getString(0));}return days; } }
    JSONObject page(String from, String to, String channel, String source, long before, long beforeTime, int limit) throws Exception {
        if(!validDay(from)||!validDay(to)||from.compareTo(to)>0)throw new IllegalArgumentException("Invalid health period");
        if(!channel.isEmpty()&&!channel.equals(HC)&&!channel.equals(BLE)&&!channel.equals(LEGACY))throw new IllegalArgumentException("Invalid channel");
        synchronized(LOCK) {
            SQLiteDatabase db=getReadableDatabase();limit=Math.max(1,Math.min(50,limit));List<String> args=new ArrayList<>(Arrays.asList(from,to,Long.toString(beforeTime>0?beforeTime:Long.MAX_VALUE),Long.toString(beforeTime>0?beforeTime:Long.MAX_VALUE),Long.toString(before>0?before:Long.MAX_VALUE)));
            String where="day>=? AND day<=? AND (sort_ms<? OR (sort_ms=? AND row_id<?))";if(!channel.isEmpty()){where+=" AND channel=?";args.add(channel);}if(!source.isEmpty()){where+=" AND source=?";args.add(source);}
            JSONArray rows=new JSONArray();long next=0,nextTime=0;
            try(Cursor c=db.rawQuery("SELECT row_id,json,sort_ms FROM records WHERE "+where+" ORDER BY sort_ms DESC,row_id DESC LIMIT "+(limit+1),args.toArray(new String[0]))) {while(c.moveToNext()){if(rows.length()==limit){next=rows.getJSONObject(rows.length()-1).getLong("cursor");nextTime=rows.getJSONObject(rows.length()-1).getLong("cursorTime");break;}JSONObject row=new JSONObject(c.getString(1));row.put("cursor",c.getLong(0));row.put("cursorTime",c.getLong(2));rows.put(row);}}
            JSONArray origins=new JSONArray();try(Cursor c=db.rawQuery("SELECT channel,source,MAX(row_id),json FROM records GROUP BY channel,source ORDER BY channel,source LIMIT 200",null)){while(c.moveToNext())origins.put(new JSONObject().put("channel",c.getString(0)).put("source",c.getString(1)).put("name",new JSONObject(c.getString(3)).getJSONObject("data").optString("deviceName","")));}
            JSONArray summaries=new JSONArray();List<String> dayArgs=new ArrayList<>(Arrays.asList(from,to));String dayWhere="day>=? AND day<=?";if(!channel.isEmpty()){dayWhere+=" AND channel=?";dayArgs.add(channel);}if(!source.isEmpty()){dayWhere+=" AND source=?";dayArgs.add(source);}
            try(Cursor c=db.rawQuery("SELECT day,channel,source,json FROM days WHERE "+dayWhere+" ORDER BY day DESC,channel,source LIMIT 31",dayArgs.toArray(new String[0]))) {while(c.moveToNext()){JSONObject d=new JSONObject(c.getString(3));if(c.getString(1).equals(HC))d.put("sleepSessions",sleepSessions(db,c.getString(0)));try(Cursor dirty=db.rawQuery("SELECT day FROM dirty_days WHERE day=?",new String[]{c.getString(0)})){if(dirty.moveToFirst())d.put("partial",true);}d.put("channel",c.getString(1));d.put("source",c.getString(2));d.put("day",c.getString(0));summaries.put(d);}}
            return new JSONObject().put("records",rows).put("nextCursor",next).put("nextTime",nextTime).put("sources",origins).put("days",summaries).put("preferences",preferences());
        }
    }
    JSONObject exportSnapshot() throws Exception {
        return transaction(db->{JSONObject out=new JSONObject().put("schemaVersion",1);JSONArray records=new JSONArray(),days=new JSONArray(),deleted=new JSONArray();
            int size=0;try(Cursor c=db.rawQuery("SELECT json FROM records ORDER BY row_id",null)){while(c.moveToNext()){String json=c.getString(0);size+=json.length();if(size>18*1024*1024)throw new IllegalStateException("Health export exceeds 18 MB; choose a shorter period in a future export");records.put(new JSONObject(json));}}
            try(Cursor c=db.rawQuery("SELECT day,channel,source,json FROM days ORDER BY day",null)){while(c.moveToNext())days.put(new JSONObject().put("day",c.getString(0)).put("channel",c.getString(1)).put("source",c.getString(2)).put("data",new JSONObject(c.getString(3))));}
            try(Cursor c=db.rawQuery("SELECT record_id,deleted_ms FROM deletions",null)){while(c.moveToNext())deleted.put(new JSONObject().put("recordId",c.getString(0)).put("deletedMs",c.getLong(1)));}
            JSONObject sync=new JSONObject();for(String key:new String[]{"lastSyncAt","lastFullSyncAt","lastAttemptAt"}){String value=meta(db,key,"");if(!value.isEmpty())sync.put(key,value);}
            out.put("records",records).put("days",days).put("deletions",deleted).put("syncMeta",sync);if(out.toString().length()>18*1024*1024)throw new IllegalStateException("Health export exceeds 18 MB");return out;
        });
    }
    private void replace(SQLiteDatabase db,JSONObject snapshot) throws Exception {
        if(snapshot.getInt("schemaVersion")!=1)throw new IllegalArgumentException("Unsupported health backup");
        JSONArray records=snapshot.getJSONArray("records"),days=snapshot.getJSONArray("days"),deleted=snapshot.optJSONArray("deletions");
        if(records.length()>100000||days.length()>20000||snapshot.toString().length()>18*1024*1024)throw new IllegalArgumentException("Health backup too large");
        db.delete("records",null,null);db.delete("days",null,null);db.delete("deletions",null,null);db.delete("dirty_days",null,null);
        if(deleted!=null)for(int i=0;i<deleted.length();i++){JSONObject d=deleted.getJSONObject(i);ContentValues v=new ContentValues();v.put("record_id",text(d,"recordId",200));v.put("deleted_ms",time(d,"deletedMs"));db.insertOrThrow("deletions",null,v);}
        for(int i=0;i<records.length();i++)upsert(db,records.getJSONObject(i));
        for(int i=0;i<days.length();i++){JSONObject d=days.getJSONObject(i);String channel=d.getString("channel"),source=d.getString("source");if(!Arrays.asList(HC,BLE,LEGACY).contains(channel)||source.isEmpty()||source.length()>255)throw new IllegalArgumentException("Invalid day source");saveDay(db,d.getString("day"),channel,source,d.getJSONObject("data"));}
        JSONObject sync=snapshot.optJSONObject("syncMeta");for(String key:new String[]{"lastSyncAt","lastFullSyncAt","lastAttemptAt"}){if(sync!=null&&!sync.isNull(key))putMeta(db,key,sync.getString(key));else db.delete("meta","key=?",new String[]{key});}
        putMeta(db,"migrated_v1","true");
    }
    // Both the old and next dataset are persisted in SQLite before WebView/photo changes.
    String prepareRestore(JSONObject snapshot) throws Exception {
        return transaction(db->{if(!meta(db,"restore_pending","").isEmpty())throw new IllegalStateException("Restore already pending");JSONObject old=exportSnapshot();
            // Validate the entire input against the real schema, then undo that trial atomically.
            db.execSQL("SAVEPOINT validate_health");try{replace(db,snapshot);}finally{db.execSQL("ROLLBACK TO validate_health");db.execSQL("RELEASE validate_health");}
            String token=UUID.randomUUID().toString();putMeta(db,"restore_old",old.toString());putMeta(db,"restore_next",snapshot.toString());putMeta(db,"restore_pending",token);return token;});
    }
    void installRestore(String token) throws Exception {transaction(db->{if(!token.equals(meta(db,"restore_pending","")))throw new IllegalArgumentException("Invalid restore token");replace(db,new JSONObject(meta(db,"restore_next","")));putMeta(db,"generation",Long.toString(generation()+1));return null;});}
    void finishRestore(String token,boolean commit) throws Exception {transaction(db->{if(!token.equals(meta(db,"restore_pending","")))throw new IllegalArgumentException("Invalid restore token");if(!commit)replace(db,new JSONObject(meta(db,"restore_old","")));db.delete("meta","key IN ('restore_pending','restore_old','restore_next')",null);putMeta(db,"generation",Long.toString(generation()+1));return null;});}
    String pendingRestore(){synchronized(LOCK){return meta(getReadableDatabase(),"restore_pending","");}}
    void erase() throws Exception {transaction(db->{long epoch=generation()+1;db.delete("records",null,null);db.delete("days",null,null);db.delete("meta",null,null);db.delete("deletions",null,null);db.delete("dirty_days",null,null);putMeta(db,"generation",Long.toString(epoch));putMeta(db,"migrated_v1","true");return null;});}
}
