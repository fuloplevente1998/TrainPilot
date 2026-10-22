package com.repforge.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal class HealthJournalStore(context: Context) :
    SQLiteOpenHelper(context, "health-journal.db", null, 2) {

    companion object {
        const val HC: String = "health_connect"; const val BLE: String = "ble_watch"; const val LEGACY: String = "legacy"; const val SAMSUNG: String = "samsung_health"
        private val LOCK = Any()
        private const val META_CHUNK_CHARS: Int = 32768
        private const val META_CHUNKS: String = "\u0001health-meta-chunks-v1"
        private val DAY_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val DAILY_SOURCE_REGEX = Regex("[A-Za-z0-9_.-]{1,255}")

        @JvmStatic private var instance: HealthJournalStore? = null

        @JvmStatic fun get(context: Context): HealthJournalStore {
            synchronized(LOCK) {
                if (instance == null) instance = HealthJournalStore(context.applicationContext)
                return instance!!
            }
        }

        private fun createMetaParts(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE meta_parts (key TEXT NOT NULL, part INTEGER NOT NULL, value TEXT NOT NULL, PRIMARY KEY(key,part), FOREIGN KEY(key) REFERENCES meta(key) ON DELETE CASCADE)")
        }

        @JvmStatic fun day(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
        @JvmStatic fun iso(time: Long): String { val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }; return f.format(Date(time)) }
        @JvmStatic fun validDay(day: String?): Boolean {
            if (day == null || !DAY_REGEX.matches(day)) return false
            return try { val f = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }; f.format(f.parse(day)!!) == day } catch (_: Exception) { false }
        }
        @Throws(Exception::class) private fun time(r: JSONObject, key: String): Long { val v = r.getLong(key); if (v < 0L || v > 4_102_444_800_000L) throw IllegalArgumentException("Invalid health time"); return v }
        @Throws(Exception::class) private fun text(r: JSONObject, key: String, max: Int): String { val v = r.getString(key); if (v.isEmpty() || v.length > max || v.indexOf('\u0000') >= 0) throw IllegalArgumentException("Invalid health identity"); return v }
    }

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true); db.rawQuery("PRAGMA secure_delete=ON", null).use { it.moveToFirst() } }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE records (row_id INTEGER PRIMARY KEY, channel TEXT NOT NULL, source TEXT NOT NULL, type TEXT NOT NULL, record_id TEXT NOT NULL, sort_ms INTEGER NOT NULL, modified_ms INTEGER NOT NULL, start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, day TEXT NOT NULL, json TEXT NOT NULL, UNIQUE(channel,type,record_id))")
        db.execSQL("CREATE INDEX records_page ON records(day DESC,sort_ms DESC,row_id DESC)")
        db.execSQL("CREATE INDEX records_source ON records(channel,source,day DESC,row_id DESC)")
        db.execSQL("CREATE TABLE days (day TEXT NOT NULL, channel TEXT NOT NULL, source TEXT NOT NULL, json TEXT NOT NULL, PRIMARY KEY(day,channel,source))")
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createMetaParts(db)
        db.execSQL("CREATE TABLE deletions (record_id TEXT PRIMARY KEY, deleted_ms INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE dirty_days (day TEXT PRIMARY KEY)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) createMetaParts(db) else throw IllegalStateException("Unsupported health schema")
    }

    private fun interface Work<T> { @Throws(Exception::class) fun run(db: SQLiteDatabase): T }
    @Throws(Exception::class) private fun <T> transaction(work: Work<T>): T {
        synchronized(LOCK) {
            val db = writableDatabase; db.beginTransaction()
            try { val v = work.run(db); db.setTransactionSuccessful(); return v } finally { db.endTransaction() }
        }
    }

    private fun meta(db: SQLiteDatabase, key: String, fallback: String): String {
        val first: String; val len: Int
        db.rawQuery("SELECT substr(value,1,$META_CHUNK_CHARS),length(value) FROM meta WHERE key=?", arrayOf(key)).use { c ->
            if (!c.moveToFirst()) return fallback
            first = c.getString(0); len = c.getInt(1)
        }
        val sb = StringBuilder(first)
        if (META_CHUNKS == first) {
            sb.setLength(0); var exp = 0
            db.rawQuery("SELECT part,value FROM meta_parts WHERE key=? ORDER BY part", arrayOf(key)).use { c ->
                while (c.moveToNext()) { if (c.getInt(0) != exp++) throw IllegalStateException("Incomplete health snapshot"); sb.append(c.getString(1)) }
            }
            if (exp == 0) throw IllegalStateException("Missing health snapshot")
        } else {
            var st = META_CHUNK_CHARS + 1
            while (st <= len) {
                db.rawQuery("SELECT substr(value,?,$META_CHUNK_CHARS) FROM meta WHERE key=?", arrayOf(st.toString(), key)).use { c ->
                    if (!c.moveToFirst()) throw IllegalStateException("Missing health snapshot"); sb.append(c.getString(0))
                }
                st += META_CHUNK_CHARS
            }
        }
        return sb.toString()
    }

    private fun putMeta(db: SQLiteDatabase, key: String, value: String) {
        val chunked = value.length > META_CHUNK_CHARS || META_CHUNKS == value
        val v = ContentValues().apply { put("key", key); put("value", if (chunked) META_CHUNKS else value) }
        if (db.insertWithOnConflict("meta", null, v, SQLiteDatabase.CONFLICT_REPLACE) == -1L) throw IllegalStateException("Cannot persist metadata")
        if (chunked) {
            var part = 0; var st = 0
            while (st < value.length) {
                var ed = min(st + META_CHUNK_CHARS, value.length)
                if (ed < value.length && Character.isHighSurrogate(value[ed - 1]) && Character.isLowSurrogate(value[ed])) ed--
                val chunk = ContentValues().apply { put("key", key); put("part", part++); put("value", value.substring(st, ed)) }
                db.insertOrThrow("meta_parts", null, chunk); st = ed
            }
        }
    }

    fun generation(): Long { synchronized(LOCK) { return meta(readableDatabase, "generation", "0").toLong() } }
    private fun writable(db: SQLiteDatabase, gen: Long) {
        if (meta(db, "generation", "0").toLong() != gen || meta(db, "restore_pending", "").isNotEmpty()) throw IllegalStateException("Health data changed")
    }

    @Throws(Exception::class) private fun upsert(db: SQLiteDatabase, r: JSONObject) {
        val channel = text(r, "channel", 32); val source = text(r, "source", 255); val type = text(r, "type", 80); val id = text(r, "recordId", 200)
        if (channel != HC && channel != BLE && channel != SAMSUNG) throw IllegalArgumentException("Invalid health channel")
        val mod = time(r, "lastModifiedMs"); val st = time(r, "startMs"); val ed = time(r, "endMs")
        if (ed < st || !validDay(r.getString("day")) || r.toString().length > 2 * 1024 * 1024) throw IllegalArgumentException("Invalid health record")
        if (channel == HC) db.rawQuery("SELECT deleted_ms FROM deletions WHERE record_id=?", arrayOf(id)).use { c -> if (c.moveToFirst() && c.getLong(0) >= mod) return }
        val v = ContentValues().apply {
            put("channel", channel); put("source", source); put("type", type); put("record_id", id); put("sort_ms", if ("SleepSessionRecord" == type) ed else st); put("modified_ms", mod); put("start_ms", st); put("end_ms", ed); put("day", r.getString("day")); put("json", r.toString())
        }
        db.insertWithOnConflict("records", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        db.update("records", v, "channel=? AND type=? AND record_id=? AND modified_ms<=?", arrayOf(channel, type, id, mod.toString()))
    }

    @Throws(Exception::class) fun importRecords(records: JSONArray, gen: Long) { transaction { db -> writable(db, gen); for (i in 0 until records.length()) upsert(db, records.getJSONObject(i)); null } }
    @Throws(Exception::class) fun reconcileRecords(records: JSONArray, type: String, start: Long, end: Long, readAt: Long, gen: Long) {
        transaction { db ->
            writable(db, gen); val seen = HashSet<String>(); for (i in 0 until records.length()) { val r = records.getJSONObject(i); seen.add(r.getString("recordId")); upsert(db, r) }
            if (start >= readAt - 28L * 86_400_000L) {
                val rem = ArrayList<Array<String>>()
                db.rawQuery("SELECT record_id,day FROM records WHERE channel=? AND type=? AND start_ms>=? AND start_ms<? AND end_ms<=? AND modified_ms<=?", arrayOf(HC, type, start.toString(), end.toString(), end.toString(), readAt.toString())).use { c ->
                    while (c.moveToNext()) if (!seen.contains(c.getString(0))) rem.add(arrayOf(c.getString(0), c.getString(1)))
                }
                for (row in rem) {
                    db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(row[1], HC))
                    db.delete("records", "channel=? AND type=? AND record_id=?", arrayOf(HC, type, row[0]))
                    db.insertWithOnConflict("deletions", null, ContentValues().apply { put("record_id", row[0]); put("deleted_ms", readAt) }, SQLiteDatabase.CONFLICT_REPLACE)
                    db.insertWithOnConflict("dirty_days", null, ContentValues().apply { put("day", row[1]) }, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            null
        }
    }

    @Throws(Exception::class) fun applyChanges(records: JSONArray, deleted: JSONArray, gen: Long): JSONArray = transaction { db ->
        writable(db, gen); val dirty = TreeSet<String>()
        for (i in 0 until deleted.length()) {
            val d = deleted.getJSONObject(i); val id = text(d, "recordId", 200); val at = time(d, "deletedMs")
            db.rawQuery("SELECT day FROM records WHERE channel=? AND record_id=?", arrayOf(HC, id)).use { c -> while (c.moveToNext()) dirty.add(c.getString(0)) }
            val v = ContentValues().apply { put("record_id", id); put("deleted_ms", at) }
            db.insertWithOnConflict("deletions", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("deletions", v, "record_id=? AND deleted_ms<=?", arrayOf(id, at.toString()))
            db.delete("records", "channel=? AND record_id=? AND modified_ms<=?", arrayOf(HC, id, at.toString()))
        }
        for (i in 0 until records.length()) {
            val r = records.getJSONObject(i)
            db.rawQuery("SELECT day FROM records WHERE channel=? AND type=? AND record_id=?", arrayOf(HC, r.getString("type"), r.getString("recordId"))).use { c -> if (c.moveToFirst()) dirty.add(c.getString(0)) }
            upsert(db, r); dirty.add(r.getString("day"))
        }
        for (ds in dirty) {
            db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(ds, HC))
            db.insertWithOnConflict("dirty_days", null, ContentValues().apply { put("day", ds) }, SQLiteDatabase.CONFLICT_IGNORE)
        }
        JSONArray(dirty)
    }

    @Throws(Exception::class) private fun saveDay(db: SQLiteDatabase, day: String, channel: String, source: String, data: JSONObject) {
        if (!validDay(day)) throw IllegalArgumentException("Invalid day")
        db.insertWithOnConflict("days", null, ContentValues().apply { put("day", day); put("channel", channel); put("source", source); put("json", data.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Throws(Exception::class) fun healthDay(day: String, result: JSONObject, gen: Long) {
        transaction { db ->
            writable(db, gen); val out = JSONObject(result.toString()); val src = out.optString("source", "@aggregate")
            if (src.isEmpty() || src.length > 255) throw IllegalArgumentException("Invalid origin")
            val part = out.optJSONArray("warnings")?.let { it.length() > 0 } ?: false
            if (part) {
                db.rawQuery("SELECT json FROM days WHERE day=? AND channel=? AND source=?", arrayOf(day, HC, src)).use { c ->
                    if (c.moveToFirst()) {
                        val old = JSONObject(c.getString(0))
                        if (old.optString("activityOrigin") != out.optString("activityOrigin")) throw IllegalStateException("Retry new source")
                        val keys = old.keys()
                        while (keys.hasNext()) { val k = keys.next(); val v = out.opt(k); if (v == null || v === JSONObject.NULL || (v is JSONArray && v.length() == 0)) out.put(k, old.get(k)) }
                    } else { db.insertWithOnConflict("dirty_days", null, ContentValues().apply { put("day", day) }, SQLiteDatabase.CONFLICT_IGNORE); return@transaction null }
                }
            }
            out.put("day", day).put("partial", part)
            if (!out.has("aggregateOrigin")) out.put("aggregateOrigin", if ("@aggregate" == src) "health_connect_priority" else "health_connect_source")
            saveDay(db, day, HC, src, out)
            if (!part && "@aggregate" == src) db.delete("dirty_days", "day=?", arrayOf(day))
            null
        }
    }

    @Throws(Exception::class) fun samsungDay(day: String, result: JSONObject, records: JSONArray, gen: Long) {
        transaction { db ->
            writable(db, gen); val out = JSONObject(result.toString()); val failed = out.optJSONArray("failedFields")
            if (failed != null && failed.length() > 0) {
                db.rawQuery("SELECT json FROM days WHERE day=? AND channel=? AND source='@aggregate'", arrayOf(day, SAMSUNG)).use { c ->
                    if (c.moveToFirst()) { val prev = JSONObject(c.getString(0)); for (i in 0 until failed.length()) { val k = failed.getString(i); if (prev.has(k)) out.put(k, prev.get(k)) } }
                }
            }
            out.put("day", day).put("source", "@aggregate").put("channel", SAMSUNG).put("partial", failed != null && failed.length() > 0)
            for (i in 0 until records.length()) { val r = records.getJSONObject(i); if (SAMSUNG != r.optString("channel")) throw IllegalArgumentException("Invalid record"); upsert(db, r) }
            saveDay(db, day, SAMSUNG, "@aggregate", out); null
        }
    }

    @Throws(Exception::class) fun watchSteps(deviceId: String, name: String, count: Long, requestAt: Long, observedAt: Long, gen: Long) {
        if (count < 0L || count > 1_000_000L || day(requestAt) != day(observedAt)) throw IllegalArgumentException("Watch reading invalid")
        transaction { db ->
            writable(db, gen); if (!meta(db, "logWatchSteps", "false").toBoolean()) return@transaction null
            val data = JSONObject().put("steps", count).put("unit", "steps").put("observedAt", iso(observedAt)).put("dateBasis", "phone_observation").put("deviceName", name)
            val r = JSONObject().put("channel", BLE).put("source", deviceId).put("type", "StepsSnapshot").put("recordId", "$deviceId:${day(observedAt)}").put("lastModifiedMs", observedAt).put("startMs", observedAt).put("endMs", observedAt).put("day", day(observedAt)).put("data", data)
            upsert(db, r); saveDay(db, day(observedAt), BLE, deviceId, JSONObject(data.toString()).put("day", day(observedAt)).put("readAt", iso(observedAt))); null
        }
    }

    @Throws(Exception::class) fun initialize(legacy: JSONObject?): JSONObject {
        transaction { db ->
            if (meta(db, "restore_pending", "").isNotEmpty()) return@transaction null
            if (meta(db, "migrated_v1", "false") != "true") {
                val days = legacy?.optJSONObject("days")
                if (days != null) { val keys = days.keys(); while (keys.hasNext()) { val d = keys.next(); if (!validDay(d)) throw IllegalArgumentException("Invalid day"); saveDay(db, d, LEGACY, "unknown", days.getJSONObject(d)) } }
                val rec = legacy?.optJSONArray("recoveryHistory")
                if (rec != null) for (i in 0 until rec.length()) { val r = rec.getJSONObject(i); val d = r.optString("day"); if (!validDay(d)) continue; val obj = days?.optJSONObject(d) ?: JSONObject(); obj.put("legacyRecovery", r); if (!obj.has("hrvRmssdMs")) obj.put("hrvRmssdMs", r.opt("hrvRmssdMs")); saveDay(db, d, LEGACY, "unknown", obj) }
                if (legacy != null) for (k in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) if (legacy.has(k) && !legacy.isNull(k)) putMeta(db, k, legacy.getString(k))
                putMeta(db, "migrated_v1", "true")
            }
            null
        }
        return projection()
    }

    @Throws(Exception::class) fun retireWatchUi() {
        if (pendingRestore().isNotEmpty()) return
        val daily = preferences().optString("hcDailySource", "auto")
        setPreferences(JSONObject().put("primary", HC).put("logWatchSteps", false).put("bleBackground", false).put("hcDailySource", if ("priority" == daily) "priority" else "auto"))
    }

    @Throws(Exception::class) fun preferences(): JSONObject {
        synchronized(LOCK) { val db = readableDatabase; return JSONObject().put("provider", meta(db, "provider", HC)).put("primary", meta(db, "primary", HC)).put("autoOnOpen", meta(db, "autoOnOpen", "false").toBoolean()).put("logWatchSteps", meta(db, "logWatchSteps", "false").toBoolean()).put("hcDailySource", meta(db, "hcDailySource", "auto")).put("bleBackground", meta(db, "bleBackground", "false").toBoolean()).put("hcBackground", meta(db, "hcBackground", "false").toBoolean()) }
    }

    @Throws(Exception::class) fun setPreferences(preferences: JSONObject) {
        transaction { db ->
            writable(db, generation()); var srcChg = false; var accChg = false
            for (k in arrayOf("provider", "primary", "autoOnOpen", "logWatchSteps", "hcDailySource", "bleBackground", "hcBackground")) {
                if (preferences.has(k)) {
                    val v = if (k in arrayOf("provider", "primary", "hcDailySource")) preferences.getString(k) else preferences.getBoolean(k).toString()
                    if ("provider" == k && v != HC && v != SAMSUNG) throw IllegalArgumentException("Invalid provider")
                    if ("provider" == k && v != meta(db, k, HC)) accChg = true
                    if ("primary" == k && v != HC && v != BLE) throw IllegalArgumentException("Invalid source")
                    if ("hcDailySource" == k && !DAILY_SOURCE_REGEX.matches(v)) throw IllegalArgumentException("Invalid source")
                    if ("hcDailySource" == k && v != meta(db, k, "auto")) srcChg = true
                    if (k in arrayOf("logWatchSteps", "bleBackground", "hcBackground") && v != meta(db, k, "false")) accChg = true
                    putMeta(db, k, v)
                }
            }
            if (!meta(db, "logWatchSteps", "false").toBoolean()) putMeta(db, "bleBackground", "false")
            if (srcChg || accChg) putMeta(db, "generation", (generation() + 1L).toString())
            if (srcChg) db.execSQL("INSERT OR IGNORE INTO dirty_days SELECT day FROM days WHERE channel='health_connect' AND source='@aggregate'")
            null
        }
    }

    @Throws(Exception::class) fun syncMeta(meta: JSONObject) {
        transaction { db -> writable(db, generation()); for (k in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) if (meta.has(k) && !meta.isNull(k)) putMeta(db, k, meta.getString(k)); null }
    }

    @Throws(Exception::class) fun projection(): JSONObject {
        synchronized(LOCK) {
            val db = readableDatabase; val days = JSONObject(); val primary = meta(db, "primary", HC); val keys = ArrayList<String>()
            val allowBle = if (meta(db, "logWatchSteps", "false").toBoolean()) "1" else "0"
            db.rawQuery("SELECT DISTINCT day FROM days WHERE channel<>'ble_watch' OR $allowBle ORDER BY day DESC LIMIT 30", null).use { c -> while (c.moveToNext()) keys.add(c.getString(0)) }
            for (dStr in keys) {
                var hc: JSONObject? = null; var old: JSONObject? = null; var ble: JSONObject? = null; var samsung: JSONObject? = null; var wSrc: String? = null
                db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(dStr)).use { c ->
                    while (c.moveToNext()) {
                        val ch = c.getString(0); val v = JSONObject(c.getString(2))
                        when {
                            ch == HC && "@aggregate" == c.getString(1) -> hc = v
                            ch == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = v
                            ch == LEGACY -> old = v
                            ch == BLE && (ble == null || v.optString("readAt") > ble!!.optString("readAt")) -> { ble = v; wSrc = c.getString(1) }
                        }
                    }
                }
                if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, dStr, ""))
                val prefProv = meta(db, "provider", HC)
                if (hc == null && old == null && !(prefProv == SAMSUNG && samsung != null) && ble == null) continue
                val out = HealthProviderProjection.day(hc, samsung, old, prefProv)
                if (ble != null && meta(db, "logWatchSteps", "false").toBoolean() && (primary == BLE || out.isNull("steps"))) {
                    out.put("steps", ble!!.get("steps")).put("stepsSource", BLE).put("stepsObservedAt", ble!!.optString("readAt")).put("stepsDeviceId", wSrc)
                }
                db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(dStr)).use { drt -> if (drt.moveToFirst()) out.put("partial", true) }
                out.put("day", dStr); days.put(dStr, out)
            }
            val out = JSONObject().put("version", 1).put("days", days).put("preferences", preferences()).put("nativeJournal", true).put("warnings", JSONArray())
            for (k in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) { val v = meta(db, k, ""); out.put(k, if (v.isEmpty()) JSONObject.NULL else v) }
            return out
        }
    }

    @Throws(Exception::class) private fun sleepSessions(db: SQLiteDatabase, day: String, source: String): JSONArray {
        val res = JSONArray()
        db.rawQuery("SELECT json FROM records WHERE channel=? AND type='SleepSessionRecord' AND day=? AND (?='' OR source=?) ORDER BY sort_ms DESC LIMIT 100", arrayOf(HC, day, source, source)).use { c ->
            while (c.moveToNext()) {
                val r = JSONObject(c.getString(0)); val data = r.getJSONObject("data"); val stages = JSONArray(); val raw = data.optJSONArray("stages")
                if (raw != null) for (i in 0 until raw.length()) { val s = raw.getJSONObject(i); stages.put(JSONObject().put("start", iso(s.getLong("startMs"))).put("end", iso(s.getLong("endMs"))).put("type", s.getInt("type")).put("isAsleep", s.getBoolean("isAsleep"))) }
                res.put(JSONObject().put("id", r.getString("recordId")).put("source", r.getString("source")).put("start", iso(r.getLong("startMs"))).put("end", iso(r.getLong("endMs"))).put("durationMinutes", (r.getLong("endMs") - r.getLong("startMs")) / 60_000.0).put("stages", stages))
            }
        }
        return res
    }

    fun dirtyDays(): JSONArray { synchronized(LOCK) { val days = JSONArray(); readableDatabase.rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 500", null).use { c -> while (c.moveToNext()) days.put(c.getString(0)) }; return days } }
    @Throws(Exception::class) fun needsSourceDay(date: String, source: String): Boolean {
        synchronized(LOCK) {
            val db = readableDatabase; val read: Long
            db.rawQuery("SELECT json FROM days WHERE day=? AND channel=? AND source=?", arrayOf(date, HC, source)).use { c ->
                if (!c.moveToFirst()) return true
                read = try { Instant.parse(JSONObject(c.getString(0)).optString("readAt")).toEpochMilli() } catch (_: Exception) { return true }
            }
            if (date == day(System.currentTimeMillis()) && System.currentTimeMillis() - read > 5 * 60_000L) return true
            db.rawQuery("SELECT MAX(modified_ms) FROM records WHERE day=? AND channel=? AND source=?", arrayOf(date, HC, source)).use { c -> if (c.moveToFirst() && c.getLong(0) > read) return true }
            db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(date)).use { c -> return c.moveToFirst() }
        }
    }
    @Throws(Exception::class) fun sourceDaySeed(date: String, source: String): JSONObject {
        synchronized(LOCK) {
            val out = JSONObject(); val seen = HashSet<String>()
            readableDatabase.rawQuery("SELECT type,json FROM records WHERE day=? AND channel=? AND source=? ORDER BY sort_ms DESC", arrayOf(date, HC, source)).use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(0); if (!seen.add(type)) continue; val record = JSONObject(c.getString(1)); val data = record.getJSONObject("data")
                    when (type) {
                        "WeightRecord" -> out.put("weightKg", data.opt("value"))
                        "BodyFatRecord" -> out.put("bodyFatPercent", data.opt("value"))
                        "OxygenSaturationRecord" -> out.put("oxygenSaturationPercent", data.opt("value"))
                        "Vo2MaxRecord" -> out.put("vo2Max", data.opt("value"))
                        "HeartRateVariabilityRmssdRecord" -> out.put("hrvRmssdMs", data.opt("value"))
                        "RespiratoryRateRecord" -> out.put("respiratoryRate", data.opt("value"))
                        "BloodGlucoseRecord" -> out.put("bloodGlucoseMmolL", data.opt("value"))
                        "BloodPressureRecord" -> { out.put("bloodPressureSystolic", data.opt("systolic")); out.put("bloodPressureDiastolic", data.opt("diastolic")) }
                    }
                }
            }
            return out
        }
    }

    @Throws(Exception::class) fun page(from: String, to: String, channel: String, source: String, before: Long, beforeTime: Long, limit: Int): JSONObject {
        if (!validDay(from) || !validDay(to) || from > to) throw IllegalArgumentException("Invalid health period")
        if (channel.isNotEmpty() && channel !in listOf(HC, BLE, LEGACY, SAMSUNG, "journal")) throw IllegalArgumentException("Invalid channel")
        synchronized(LOCK) {
            val db = readableDatabase; val bLim = max(1, min(50, limit))
            val args = ArrayList(listOf(from, to, (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(), (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(), (if (before > 0L) before else Long.MAX_VALUE).toString()))
            var where = "day>=? AND day<=? AND (sort_ms<? OR (sort_ms=? AND row_id<?))"
            if ("journal" == channel) where += " AND channel<>'ble_watch'" else if (channel.isNotEmpty()) { where += " AND channel=?"; args.add(channel) }
            if (source.isNotEmpty()) { where += " AND source=?"; args.add(source) }
            val rows = JSONArray(); var next = 0L; var nextTime = 0L
            db.rawQuery("SELECT row_id,json,sort_ms FROM records WHERE $where ORDER BY sort_ms DESC,row_id DESC LIMIT ${bLim + 1}", args.toTypedArray()).use { c ->
                while (c.moveToNext()) {
                    if (rows.length() == bLim) { next = rows.getJSONObject(rows.length() - 1).getLong("cursor"); nextTime = rows.getJSONObject(rows.length() - 1).getLong("cursorTime"); break }
                    rows.put(JSONObject(c.getString(1)).put("cursor", c.getLong(0)).put("cursorTime", c.getLong(2)))
                }
            }
            val origins = JSONArray()
            db.rawQuery("SELECT channel,source,MAX(row_id),json FROM records GROUP BY channel,source ORDER BY channel,source LIMIT 200", null).use { c ->
                while (c.moveToNext()) origins.put(JSONObject().put("channel", c.getString(0)).put("source", c.getString(1)).put("name", JSONObject(c.getString(3)).getJSONObject("data").optString("deviceName", "")))
            }
            var summaries = JSONArray()
            val dayArgs = ArrayList(listOf(from, to))
            var dayWhere = "day>=? AND day<=?"
            if ("journal" == channel) dayWhere += " AND (channel='health_connect' AND source='@aggregate' OR channel='legacy' AND NOT EXISTS (SELECT 1 FROM days AS hc WHERE hc.day=days.day AND hc.channel='health_connect' AND hc.source='@aggregate'))"
            else if (channel.isNotEmpty()) { dayWhere += " AND channel=?"; dayArgs.add(channel) }
            if (source.isNotEmpty()) { dayWhere += " AND source=?"; dayArgs.add(source) } else dayWhere += " AND (channel<>'health_connect' OR source='@aggregate')"
            db.rawQuery("SELECT day,channel,source,json FROM days WHERE $dayWhere ORDER BY day DESC,channel,source LIMIT 31", dayArgs.toTypedArray()).use { c ->
                while (c.moveToNext()) {
                    val d = JSONObject(c.getString(3))
                    if (c.getString(1) == HC) d.put("sleepSessions", sleepSessions(db, c.getString(0), if ("@aggregate" == c.getString(2)) "" else c.getString(2)))
                    db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(c.getString(0))).use { drt -> if (drt.moveToFirst()) d.put("partial", true) }
                    summaries.put(d.put("channel", c.getString(1)).put("source", c.getString(2)).put("day", c.getString(0)))
                }
            }
            if ("journal" == channel) {
                summaries = JSONArray(); val pref = meta(db, "provider", HC)
                db.rawQuery("SELECT DISTINCT day FROM days WHERE day>=? AND day<=? AND channel<>'ble_watch' ORDER BY day DESC LIMIT 31", arrayOf(from, to)).use { dates ->
                    while (dates.moveToNext()) {
                        val dt = dates.getString(0); var hc: JSONObject? = null; var samsung: JSONObject? = null; var legacy: JSONObject? = null
                        db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(dt)).use { c ->
                            while (c.moveToNext()) {
                                when {
                                    c.getString(0) == HC && "@aggregate" == c.getString(1) -> hc = JSONObject(c.getString(2))
                                    c.getString(0) == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = JSONObject(c.getString(2))
                                    c.getString(0) == LEGACY -> legacy = JSONObject(c.getString(2))
                                }
                            }
                        }
                        if (hc == null && legacy == null && (pref != SAMSUNG || samsung == null)) continue
                        if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, dt, ""))
                        val sel = HealthProviderProjection.day(hc, samsung, legacy, pref).put("day", dt)
                        if (!sel.has("source")) sel.put("source", if (hc != null || samsung != null) "@aggregate" else "unknown")
                        db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(dt)).use { drt -> if (drt.moveToFirst() && sel.optString("channel") == HC) sel.put("partial", true) }
                        summaries.put(sel)
                    }
                }
            }
            return JSONObject().put("records", rows).put("nextCursor", next).put("nextTime", nextTime).put("sources", origins).put("days", summaries).put("preferences", preferences())
        }
    }

    @Throws(Exception::class) fun exportSnapshot(): JSONObject = transaction { db ->
        val out = JSONObject().put("schemaVersion", 1); val records = JSONArray(); val days = JSONArray(); val deleted = JSONArray()
        var size = 0
        db.rawQuery("SELECT json FROM records ORDER BY row_id", null).use { c ->
            while (c.moveToNext()) {
                val j = c.getString(0); size += j.length; if (size > 18 * 1024 * 1024) throw IllegalStateException("Health export exceeds 18 MB")
                records.put(JSONObject(j))
            }
        }
        db.rawQuery("SELECT day,channel,source,json FROM days ORDER BY day", null).use { c ->
            while (c.moveToNext()) days.put(JSONObject().put("day", c.getString(0)).put("channel", c.getString(1)).put("source", c.getString(2)).put("data", JSONObject(c.getString(3))))
        }
        db.rawQuery("SELECT record_id,deleted_ms FROM deletions", null).use { c ->
            while (c.moveToNext()) deleted.put(JSONObject().put("recordId", c.getString(0)).put("deletedMs", c.getLong(1)))
        }
        val sync = JSONObject()
        for (k in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) { val v = meta(db, k, ""); if (v.isNotEmpty()) sync.put(k, v) }
        out.put("records", records).put("days", days).put("deletions", deleted).put("syncMeta", sync)
        if (out.toString().length > 18 * 1024 * 1024) throw IllegalStateException("Health export exceeds 18 MB")
        out
    }

    @Throws(Exception::class) private fun replace(db: SQLiteDatabase, snapshot: JSONObject) {
        if (snapshot.getInt("schemaVersion") != 1) throw IllegalArgumentException("Unsupported health backup")
        val records = snapshot.getJSONArray("records"); val days = snapshot.getJSONArray("days"); val deleted = snapshot.optJSONArray("deletions")
        if (records.length() > 100_000 || days.length() > 20_000 || snapshot.toString().length > 18 * 1024 * 1024) throw IllegalArgumentException("Health backup too large")
        db.delete("records", null, null); db.delete("days", null, null); db.delete("deletions", null, null); db.delete("dirty_days", null, null)
        if (deleted != null) for (i in 0 until deleted.length()) { val d = deleted.getJSONObject(i); db.insertOrThrow("deletions", null, ContentValues().apply { put("record_id", text(d, "recordId", 200)); put("deleted_ms", time(d, "deletedMs")) }) }
        for (i in 0 until records.length()) upsert(db, records.getJSONObject(i))
        for (i in 0 until days.length()) { val d = days.getJSONObject(i); val ch = d.getString("channel"); val src = d.getString("source"); if (ch !in listOf(HC, BLE, LEGACY, SAMSUNG) || src.isEmpty() || src.length > 255) throw IllegalArgumentException("Invalid day source"); saveDay(db, d.getString("day"), ch, src, d.getJSONObject("data")) }
        val sync = snapshot.optJSONObject("syncMeta")
        for (k in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) { if (sync != null && !sync.isNull(k)) putMeta(db, k, sync.getString(k)) else db.delete("meta", "key=?", arrayOf(k)) }
        putMeta(db, "migrated_v1", "true")
    }

    @Throws(Exception::class) fun prepareRestore(snapshot: JSONObject): String = transaction { db ->
        if (meta(db, "restore_pending", "").isNotEmpty()) throw IllegalStateException("Restore already pending")
        val old = exportSnapshot()
        db.execSQL("SAVEPOINT validate_health")
        try { replace(db, snapshot) } finally { db.execSQL("ROLLBACK TO validate_health"); db.execSQL("RELEASE validate_health") }
        val token = UUID.randomUUID().toString()
        putMeta(db, "restore_old", old.toString()); putMeta(db, "restore_next", snapshot.toString()); putMeta(db, "restore_pending", token); token
    }

    @Throws(Exception::class) fun installRestore(token: String) { transaction { db -> if (token != meta(db, "restore_pending", "")) throw IllegalArgumentException("Invalid restore token"); replace(db, JSONObject(meta(db, "restore_next", ""))); putMeta(db, "generation", (generation() + 1L).toString()); null } }
    @Throws(Exception::class) fun finishRestore(token: String, commit: Boolean) { transaction { db -> if (token != meta(db, "restore_pending", "")) throw IllegalArgumentException("Invalid restore token"); if (!commit) replace(db, JSONObject(meta(db, "restore_old", ""))); db.delete("meta", "key IN ('restore_pending','restore_old','restore_next')", null); putMeta(db, "generation", (generation() + 1L).toString()); null } }
    fun pendingRestore(): String { synchronized(LOCK) { return meta(readableDatabase, "restore_pending", "") } }
    @Throws(Exception::class) fun erase() { transaction { db -> val epoch = generation() + 1L; db.delete("records", null, null); db.delete("days", null, null); db.delete("meta", null, null); db.delete("deletions", null, null); db.delete("dirty_days", null, null); putMeta(db, "generation", epoch.toString()); putMeta(db, "migrated_v1", "true"); null } }
}
