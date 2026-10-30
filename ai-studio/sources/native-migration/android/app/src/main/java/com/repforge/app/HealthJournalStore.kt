package com.repforge.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.TreeSet
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** The primary health store. The UI only receives bounded projections/pages. */
internal class HealthJournalStore(context: Context) :
    SQLiteOpenHelper(context, "health-journal.db", null, 2) {

    companion object {
        const val HC: String = "health_connect"
        const val BLE: String = "ble_watch"
        const val LEGACY: String = "legacy"
        const val SAMSUNG: String = "samsung_health"
        private val LOCK = Any()
        private const val META_CHUNK_CHARS: Int = 32768
        private const val META_CHUNKS: String = "\u0001health-meta-chunks-v1"
        private val DAY_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val DAILY_SOURCE_REGEX = Regex("[A-Za-z0-9_.-]{1,255}")

        // Kept as @JvmStatic private var instance so Robolectric tests reflecting on "instance" succeed!
        @JvmStatic
        private var instance: HealthJournalStore? = null

        @JvmStatic
        fun get(context: Context): HealthJournalStore {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = HealthJournalStore(context.applicationContext)
                }
                return instance!!
            }
        }

        private fun createMetaParts(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE meta_parts (key TEXT NOT NULL, part INTEGER NOT NULL, value TEXT NOT NULL, " +
                    "PRIMARY KEY(key,part), FOREIGN KEY(key) REFERENCES meta(key) ON DELETE CASCADE)"
            )
        }

        @JvmStatic
        fun day(time: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
        }

        @JvmStatic
        fun iso(time: Long): String {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            f.timeZone = TimeZone.getTimeZone("UTC")
            return f.format(Date(time))
        }

        @JvmStatic
        fun validDay(day: String?): Boolean {
            if (day == null || !DAY_REGEX.matches(day)) return false
            return try {
                val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                f.isLenient = false
                val parsed = f.parse(day) ?: return false
                f.format(parsed) == day
            } catch (_: Exception) {
                false
            }
        }

        @Throws(Exception::class)
        private fun time(r: JSONObject, key: String): Long {
            val value = r.getLong(key)
            if (value < 0L || value > 4_102_444_800_000L) {
                throw IllegalArgumentException("Invalid health time")
            }
            return value
        }

        @Throws(Exception::class)
        private fun text(r: JSONObject, key: String, max: Int): String {
            val value = r.getString(key)
            if (value.isEmpty() || value.length > max || value.indexOf('\u0000') >= 0) {
                throw IllegalArgumentException("Invalid health identity")
            }
            return value
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        db.rawQuery("PRAGMA secure_delete=ON", null).use { it.moveToFirst() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE records (row_id INTEGER PRIMARY KEY, channel TEXT NOT NULL, source TEXT NOT NULL, " +
                "type TEXT NOT NULL, record_id TEXT NOT NULL, sort_ms INTEGER NOT NULL, modified_ms INTEGER NOT NULL, " +
                "start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, day TEXT NOT NULL, json TEXT NOT NULL, " +
                "UNIQUE(channel,type,record_id))"
        )
        db.execSQL("CREATE INDEX records_page ON records(day DESC,sort_ms DESC,row_id DESC)")
        db.execSQL("CREATE INDEX records_source ON records(channel,source,day DESC,row_id DESC)")
        db.execSQL(
            "CREATE TABLE days (day TEXT NOT NULL, channel TEXT NOT NULL, source TEXT NOT NULL, " +
                "json TEXT NOT NULL, PRIMARY KEY(day,channel,source))"
        )
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createMetaParts(db)
        db.execSQL("CREATE TABLE deletions (record_id TEXT PRIMARY KEY, deleted_ms INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE dirty_days (day TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            createMetaParts(db)
        } else {
            throw IllegalStateException("Unsupported health schema")
        }
    }

    private fun interface Work<T> {
        @Throws(Exception::class)
        fun run(db: SQLiteDatabase): T
    }

    @Throws(Exception::class)
    private fun <T> transaction(work: Work<T>): T {
        synchronized(LOCK) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val value = work.run(db)
                db.setTransactionSuccessful()
                return value
            } finally {
                db.endTransaction()
            }
        }
    }

    private fun meta(db: SQLiteDatabase, key: String, fallback: String): String {
        val first: String
        val length: Int
        db.rawQuery(
            "SELECT substr(value,1,$META_CHUNK_CHARS),length(value) FROM meta WHERE key=?",
            arrayOf(key)
        ).use { c ->
            if (!c.moveToFirst()) return fallback
            first = c.getString(0)
            length = c.getInt(1)
        }
        val value = StringBuilder(first)
        if (META_CHUNKS == first) {
            value.setLength(0)
            var expected = 0
            db.rawQuery(
                "SELECT part,value FROM meta_parts WHERE key=? ORDER BY part",
                arrayOf(key)
            ).use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(0) != expected++) {
                        throw IllegalStateException("Incomplete health restore snapshot")
                    }
                    value.append(c.getString(1))
                }
            }
            if (expected == 0) throw IllegalStateException("Missing health restore snapshot")
        } else {
            var start = META_CHUNK_CHARS + 1
            while (start <= length) {
                db.rawQuery(
                    "SELECT substr(value,?,$META_CHUNK_CHARS) FROM meta WHERE key=?",
                    arrayOf(start.toString(), key)
                ).use { c ->
                    if (!c.moveToFirst()) throw IllegalStateException("Missing health restore snapshot")
                    value.append(c.getString(0))
                }
                start += META_CHUNK_CHARS
            }
        }
        return value.toString()
    }

    private fun putMeta(db: SQLiteDatabase, key: String, value: String) {
        val chunked = value.length > META_CHUNK_CHARS || META_CHUNKS == value
        val v = ContentValues().apply {
            put("key", key)
            put("value", if (chunked) META_CHUNKS else value)
        }
        if (db.insertWithOnConflict("meta", null, v, SQLiteDatabase.CONFLICT_REPLACE) == -1L) {
            throw IllegalStateException("Cannot persist health restore metadata")
        }
        if (chunked) {
            var part = 0
            var start = 0
            while (start < value.length) {
                var end = min(start + META_CHUNK_CHARS, value.length)
                if (end < value.length &&
                    Character.isHighSurrogate(value[end - 1]) &&
                    Character.isLowSurrogate(value[end])
                ) {
                    end--
                }
                val chunk = ContentValues().apply {
                    put("key", key)
                    put("part", part++)
                    put("value", value.substring(start, end))
                }
                db.insertOrThrow("meta_parts", null, chunk)
                start = end
            }
        }
    }

    fun generation(): Long {
        synchronized(LOCK) {
            return meta(readableDatabase, "generation", "0").toLong()
        }
    }

    private fun writable(db: SQLiteDatabase, generation: Long) {
        if (meta(db, "generation", "0").toLong() != generation || meta(db, "restore_pending", "").isNotEmpty()) {
            throw IllegalStateException("Health data changed; retry after restore/reopen")
        }
    }

    @Throws(Exception::class)
    private fun upsert(db: SQLiteDatabase, r: JSONObject) {
        val channel = text(r, "channel", 32)
        val source = text(r, "source", 255)
        val type = text(r, "type", 80)
        val id = text(r, "recordId", 200)
        if (channel != HC && channel != BLE && channel != SAMSUNG) {
            throw IllegalArgumentException("Invalid health channel")
        }
        val modified = time(r, "lastModifiedMs")
        val start = time(r, "startMs")
        val end = time(r, "endMs")
        if (end < start || !validDay(r.getString("day")) || r.toString().length > 2 * 1024 * 1024) {
            throw IllegalArgumentException("Invalid health record")
        }
        r.getJSONObject("data")
        if (channel == HC) {
            db.rawQuery("SELECT deleted_ms FROM deletions WHERE record_id=?", arrayOf(id)).use { c ->
                if (c.moveToFirst() && c.getLong(0) >= modified) return
            }
        }
        val v = ContentValues().apply {
            put("channel", channel)
            put("source", source)
            put("type", type)
            put("record_id", id)
            put("sort_ms", if ("SleepSessionRecord" == type) end else start)
            put("modified_ms", modified)
            put("start_ms", start)
            put("end_ms", end)
            put("day", r.getString("day"))
            put("json", r.toString())
        }
        db.insertWithOnConflict("records", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        db.update(
            "records",
            v,
            "channel=? AND type=? AND record_id=? AND modified_ms<=?",
            arrayOf(channel, type, id, modified.toString())
        )
    }

    @Throws(Exception::class)
    fun importRecords(records: JSONArray, generation: Long) {
        transaction { db ->
            writable(db, generation)
            for (i in 0 until records.length()) {
                upsert(db, records.getJSONObject(i))
            }
            null
        }
    }

    @Throws(Exception::class)
    fun reconcileRecords(
        records: JSONArray,
        type: String,
        start: Long,
        end: Long,
        readAt: Long,
        generation: Long
    ) {
        transaction { db ->
            writable(db, generation)
            val seen = HashSet<String>()
            for (i in 0 until records.length()) {
                val r = records.getJSONObject(i)
                seen.add(r.getString("recordId"))
                upsert(db, r)
            }
            if (start >= readAt - 28L * 86_400_000L) {
                val removed = ArrayList<Array<String>>()
                db.rawQuery(
                    "SELECT record_id,day FROM records WHERE channel=? AND type=? AND start_ms>=? AND start_ms<? AND end_ms<=? AND modified_ms<=?",
                    arrayOf(HC, type, start.toString(), end.toString(), end.toString(), readAt.toString())
                ).use { c ->
                    while (c.moveToNext()) {
                        if (!seen.contains(c.getString(0))) {
                            removed.add(arrayOf(c.getString(0), c.getString(1)))
                        }
                    }
                }
                for (row in removed) {
                    db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(row[1], HC))
                    db.delete("records", "channel=? AND type=? AND record_id=?", arrayOf(HC, type, row[0]))
                    val vDel = ContentValues().apply {
                        put("record_id", row[0])
                        put("deleted_ms", readAt)
                    }
                    db.insertWithOnConflict("deletions", null, vDel, SQLiteDatabase.CONFLICT_REPLACE)
                    val vDirty = ContentValues().apply { put("day", row[1]) }
                    db.insertWithOnConflict("dirty_days", null, vDirty, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
            null
        }
    }

    internal fun nativeChangeRevision(expectedGeneration: Long): Long = synchronized(LOCK) {
        writable(readableDatabase, expectedGeneration)
        meta(readableDatabase, "hc_change_revision", "0").toLong()
    }

    /** Atomic local epoch check + local write. No provider IPC is allowed in writer. */
    internal fun nativePublishAtChangeRevision(expectedGeneration: Long, revision: Long, writer: () -> Unit) {
        synchronized(LOCK) {
            check(nativeChangeRevision(expectedGeneration) == revision) { "Health changes superseded this daily read" }
            writer()
        }
    }

    private fun affectedSyncDays(days: MutableSet<String>, start: Long, end: Long, day: String) {
        days.add(day)
        val zone = java.time.ZoneId.systemDefault()
        var cursor = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
        val last = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
        check(java.time.temporal.ChronoUnit.DAYS.between(cursor, last) <= 530) { "Health interval exceeds sync budget" }
        while (cursor <= last) { days.add(cursor.toString()); cursor = cursor.plusDays(1) }
    }

    @Throws(Exception::class)
    fun applyChanges(records: JSONArray, deleted: JSONArray, generation: Long): JSONArray {
        return transaction { db ->
            writable(db, generation)
            val dirty = TreeSet<String>()
            for (i in 0 until deleted.length()) {
                val d = deleted.getJSONObject(i)
                val id = text(d, "recordId", 200)
                val at = time(d, "deletedMs")
                db.rawQuery("SELECT day,start_ms,end_ms FROM records WHERE channel=? AND record_id=?", arrayOf(HC, id)).use { c ->
                    while (c.moveToNext()) affectedSyncDays(dirty, c.getLong(1), c.getLong(2), c.getString(0))
                }
                val v = ContentValues().apply {
                    put("record_id", id)
                    put("deleted_ms", at)
                }
                db.insertWithOnConflict("deletions", null, v, SQLiteDatabase.CONFLICT_IGNORE)
                db.update("deletions", v, "record_id=? AND deleted_ms<=?", arrayOf(id, at.toString()))
                db.delete("records", "channel=? AND record_id=? AND modified_ms<=?", arrayOf(HC, id, at.toString()))
            }
            for (i in 0 until records.length()) {
                val r = records.getJSONObject(i)
                db.rawQuery(
                    "SELECT day,start_ms,end_ms FROM records WHERE channel=? AND type=? AND record_id=?",
                    arrayOf(HC, r.getString("type"), r.getString("recordId"))
                ).use { c ->
                    if (c.moveToFirst()) affectedSyncDays(dirty, c.getLong(1), c.getLong(2), c.getString(0))
                }
                upsert(db, r)
                affectedSyncDays(dirty, r.getLong("startMs"), r.getLong("endMs"), r.getString("day"))
            }
            for (dayStr in dirty) {
                db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(dayStr, HC))
                val v = ContentValues().apply { put("day", dayStr) }
                db.insertWithOnConflict("dirty_days", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            }
            if (records.length() > 0 || deleted.length() > 0) {
                putMeta(db, "hc_change_revision", (meta(db, "hc_change_revision", "0").toLong() + 1).toString())
            }
            JSONArray(dirty)
        }
    }

    @Throws(Exception::class)
    private fun saveDay(db: SQLiteDatabase, day: String, channel: String, source: String, data: JSONObject) {
        if (!validDay(day)) throw IllegalArgumentException("Invalid day")
        val v = ContentValues().apply {
            put("day", day)
            put("channel", channel)
            put("source", source)
            put("json", data.toString())
        }
        db.insertWithOnConflict("days", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    @Throws(Exception::class)
    fun healthDay(day: String, result: JSONObject, generation: Long) {
        transaction { db ->
            writable(db, generation)
            val out = JSONObject(result.toString())
            val source = out.optString("source", "@aggregate")
            if (source.isEmpty() || source.length > 255) {
                throw IllegalArgumentException("Invalid daily origin")
            }
            val partial = out.optJSONArray("warnings")?.let { it.length() > 0 } ?: false
            if (partial) {
                db.rawQuery(
                    "SELECT json FROM days WHERE day=? AND channel=? AND source=?",
                    arrayOf(day, HC, source)
                ).use { c ->
                    if (c.moveToFirst()) {
                        val old = JSONObject(c.getString(0))
                        if (old.optString("activityOrigin") != out.optString("activityOrigin")) {
                            throw IllegalStateException("Retry new source aggregate")
                        }
                        val keys = old.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = out.opt(key)
                            if (value == null || value === JSONObject.NULL || (value is JSONArray && value.length() == 0)) {
                                out.put(key, old.get(key))
                            }
                        }
                    } else {
                        // First read with only some permissions: retain the metrics genuinely
                        // received, visibly marked partial. An absent metric stays absent, not
                        // an invented zero or a discarded measurement.
                        val dirty = ContentValues().apply { put("day", day) }
                        db.insertWithOnConflict("dirty_days", null, dirty, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
            }
            out.put("day", day)
            out.put("partial", partial)
            if (!out.has("aggregateOrigin")) {
                out.put("aggregateOrigin", if ("@aggregate" == source) "health_connect_priority" else "health_connect_source")
            }
            saveDay(db, day, HC, source, out)
            if (!partial && "@aggregate" == source) {
                db.delete("dirty_days", "day=?", arrayOf(day))
            }
            null
        }
    }

    @Throws(Exception::class)
    fun samsungDay(day: String, result: JSONObject, records: JSONArray, generation: Long) {
        transaction { db ->
            writable(db, generation)
            val out = JSONObject(result.toString())
            val failed = out.optJSONArray("failedFields")
            if (failed != null && failed.length() > 0) {
                db.rawQuery(
                    "SELECT json FROM days WHERE day=? AND channel=? AND source='@aggregate'",
                    arrayOf(day, SAMSUNG)
                ).use { c ->
                    if (c.moveToFirst()) {
                        val previous = JSONObject(c.getString(0))
                        for (i in 0 until failed.length()) {
                            val key = failed.getString(i)
                            if (previous.has(key)) out.put(key, previous.get(key))
                        }
                    }
                }
            }
            out.put("day", day)
                .put("source", "@aggregate")
                .put("channel", SAMSUNG)
                .put("partial", failed != null && failed.length() > 0)
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                if (SAMSUNG != record.optString("channel")) {
                    throw IllegalArgumentException("Invalid Samsung record")
                }
                upsert(db, record)
            }
            saveDay(db, day, SAMSUNG, "@aggregate", out)
            null
        }
    }

    @Throws(Exception::class)
    fun watchSteps(
        deviceId: String,
        name: String,
        count: Long,
        requestAt: Long,
        observedAt: Long,
        generation: Long
    ) {
        if (count < 0L || count > 1_000_000L || day(requestAt) != day(observedAt)) {
            throw IllegalArgumentException("Watch reading crossed midnight or is invalid")
        }
        transaction { db ->
            writable(db, generation)
            if (!meta(db, "logWatchSteps", "false").toBoolean()) return@transaction null
            val data = JSONObject()
                .put("steps", count)
                .put("unit", "steps")
                .put("observedAt", iso(observedAt))
                .put("dateBasis", "phone_observation")
                .put("deviceName", name)
            val r = JSONObject()
                .put("channel", BLE)
                .put("source", deviceId)
                .put("type", "StepsSnapshot")
                .put("recordId", "$deviceId:${day(observedAt)}")
                .put("lastModifiedMs", observedAt)
                .put("startMs", observedAt)
                .put("endMs", observedAt)
                .put("day", day(observedAt))
                .put("data", data)
            upsert(db, r)
            saveDay(
                db,
                day(observedAt),
                BLE,
                deviceId,
                JSONObject(data.toString()).put("day", day(observedAt)).put("readAt", iso(observedAt))
            )
            null
        }
    }

    @Throws(Exception::class)
    fun initialize(legacy: JSONObject?): JSONObject {
        transaction { db ->
            if (meta(db, "restore_pending", "").isNotEmpty()) return@transaction null
            if (meta(db, "migrated_v1", "false") != "true") {
                val days = legacy?.optJSONObject("days")
                if (days != null) {
                    val keys = days.keys()
                    while (keys.hasNext()) {
                        val dayStr = keys.next()
                        if (!validDay(dayStr)) throw IllegalArgumentException("Invalid legacy day")
                        saveDay(db, dayStr, LEGACY, "unknown", days.getJSONObject(dayStr))
                    }
                }
                val recovery = legacy?.optJSONArray("recoveryHistory")
                if (recovery != null) {
                    for (i in 0 until recovery.length()) {
                        val r = recovery.getJSONObject(i)
                        val dayStr = r.optString("day")
                        if (!validDay(dayStr)) continue
                        val d = days?.optJSONObject(dayStr) ?: JSONObject()
                        d.put("legacyRecovery", r)
                        if (!d.has("hrvRmssdMs")) d.put("hrvRmssdMs", r.opt("hrvRmssdMs"))
                        saveDay(db, dayStr, LEGACY, "unknown", d)
                    }
                }
                if (legacy != null) {
                    for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) {
                        if (legacy.has(key) && !legacy.isNull(key)) {
                            putMeta(db, key, legacy.getString(key))
                        }
                    }
                }
                putMeta(db, "migrated_v1", "true")
            }
            null
        }
        return projection()
    }

    @Throws(Exception::class)
    fun retireWatchUi() {
        if (pendingRestore().isNotEmpty()) return
        val prefs = preferences()
        val daily = prefs.optString("hcDailySource", "auto")
        if (HC == prefs.optString("primary") && !prefs.optBoolean("logWatchSteps") &&
            !prefs.optBoolean("bleBackground") && (daily == "auto" || daily == "priority")) return
        setPreferences(
            JSONObject()
                .put("primary", HC)
                .put("logWatchSteps", false)
                .put("bleBackground", false)
                .put("hcDailySource", if ("priority" == daily) "priority" else "auto")
        )
    }

    @Throws(Exception::class)
    fun preferences(): JSONObject {
        synchronized(LOCK) {
            val db = readableDatabase
            return JSONObject()
                .put("provider", meta(db, "provider", HC))
                .put("primary", meta(db, "primary", HC))
                .put("autoOnOpen", meta(db, "autoOnOpen", "false").toBoolean())
                .put("logWatchSteps", meta(db, "logWatchSteps", "false").toBoolean())
                .put("hcDailySource", meta(db, "hcDailySource", "auto"))
                .put("bleBackground", meta(db, "bleBackground", "false").toBoolean())
                .put("hcBackground", meta(db, "hcBackground", "false").toBoolean())
        }
    }

    @Throws(Exception::class)
    fun setPreferences(preferences: JSONObject) {
        transaction { db ->
            writable(db, generation())
            var sourceChanged = false
            var accessChanged = false
            for (key in arrayOf("provider", "primary", "autoOnOpen", "logWatchSteps", "hcDailySource", "bleBackground", "hcBackground")) {
                if (preferences.has(key)) {
                    val value = if (key in arrayOf("provider", "primary", "hcDailySource")) {
                        preferences.getString(key)
                    } else {
                        preferences.getBoolean(key).toString()
                    }
                    if ("provider" == key && value != HC && value != SAMSUNG) {
                        throw IllegalArgumentException("Invalid health provider")
                    }
                    if ("provider" == key && value != meta(db, key, HC)) accessChanged = true
                    if ("primary" == key && value != HC && value != BLE) {
                        throw IllegalArgumentException("Invalid primary source")
                    }
                    if ("hcDailySource" == key && !DAILY_SOURCE_REGEX.matches(value)) {
                        throw IllegalArgumentException("Invalid daily source")
                    }
                    if ("hcDailySource" == key && value != meta(db, key, "auto")) sourceChanged = true
                    if (key in arrayOf("logWatchSteps", "bleBackground", "hcBackground") && value != meta(db, key, "false")) {
                        accessChanged = true
                    }
                    putMeta(db, key, value)
                }
            }
            if (!meta(db, "logWatchSteps", "false").toBoolean()) {
                putMeta(db, "bleBackground", "false")
            }
            if (sourceChanged || accessChanged) {
                putMeta(db, "generation", (generation() + 1L).toString())
            }
            if (sourceChanged) {
                db.execSQL("INSERT OR IGNORE INTO dirty_days SELECT day FROM days WHERE channel='health_connect' AND source='@aggregate'")
            }
            null
        }
    }

    internal fun nativeSyncDirtyDays(): Set<java.time.LocalDate> = synchronized(LOCK) {
        val days = sortedSetOf<java.time.LocalDate>()
        readableDatabase.rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 531", null).use { c ->
            while (c.moveToNext()) days.add(java.time.LocalDate.parse(c.getString(0)))
        }
        check(days.size <= 530) { "Health dirty days exceed foreground budget" }
        days
    }

    internal fun nativeSyncLastSuccess(): String = synchronized(LOCK) {
        meta(readableDatabase, "lastSyncAt", "—")
    }

    /** Only this private key contains SDK tokens. Export/projection use explicit whitelists. */
    private fun nativeSyncKey(provider: String): String {
        require(provider == HC || provider == SAMSUNG)
        return "native_sync_private_" + provider
    }

    internal fun nativeSyncState(provider: String, expectedGeneration: Long): NativeHealthSync.Checkpoint = synchronized(LOCK) {
        writable(readableDatabase, expectedGeneration)
        val value = JSONObject(meta(readableDatabase, nativeSyncKey(provider), "{}"))
        if (value.optLong("epoch", -1) != expectedGeneration) NativeHealthSync.Checkpoint()
        else NativeHealthSync.Checkpoint(value.optString("scope"), value.optString("token"),
            value.optBoolean("initialized"), value.optLong("revision"))
    }

    /** Generation AND revision CAS: an old SDK response cannot resurrect a restore's token. */
    internal fun nativeSyncCheckpoint(
        provider: String, expectedGeneration: Long, expectedRevision: Long,
        checkpoint: NativeHealthSync.Checkpoint, full: Boolean, at: String
    ) {
        transaction { db ->
            writable(db, expectedGeneration)
            val current = nativeSyncState(provider, expectedGeneration)
            check(current.revision == expectedRevision) { "Obsolete Health sync checkpoint" }
            putMeta(db, nativeSyncKey(provider), JSONObject().put("epoch", expectedGeneration)
                .put("revision", expectedRevision + 1).put("scope", checkpoint.scope)
                .put("token", checkpoint.token).put("initialized", true).toString())
            putMeta(db, "lastSyncAt", at)
            if (full) putMeta(db, "lastFullSyncAt", at)
            null
        }
    }

    internal fun nativeSyncAttempt(expectedGeneration: Long, at: String) {
        transaction { db ->
            writable(db, expectedGeneration)
            putMeta(db, "lastAttemptAt", at)
            null
        }
    }

    @Throws(Exception::class)
    fun syncMeta(meta: JSONObject) {
        transaction { db ->
            writable(db, generation())
            for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) {
                if (meta.has(key) && !meta.isNull(key)) {
                    putMeta(db, key, meta.getString(key))
                }
            }
            null
        }
    }

    @Throws(Exception::class)
    fun projection(): JSONObject {
        synchronized(LOCK) {
            val db = readableDatabase
            val days = JSONObject()
            val primary = meta(db, "primary", HC)
            val keys = ArrayList<String>()
            val allowBle = if (meta(db, "logWatchSteps", "false").toBoolean()) "1" else "0"
            db.rawQuery(
                "SELECT DISTINCT day FROM days WHERE channel<>'ble_watch' OR $allowBle ORDER BY day DESC LIMIT 30",
                null
            ).use { c ->
                while (c.moveToNext()) keys.add(c.getString(0))
            }
            for (dayStr in keys) {
                var hc: JSONObject? = null
                var old: JSONObject? = null
                var ble: JSONObject? = null
                var samsung: JSONObject? = null
                var watchSource: String? = null
                db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(dayStr)).use { c ->
                    while (c.moveToNext()) {
                        val channel = c.getString(0)
                        val value = JSONObject(c.getString(2))
                        when {
                            channel == HC && "@aggregate" == c.getString(1) -> hc = value
                            channel == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = value
                            channel == LEGACY -> old = value
                            channel == BLE && (ble == null || value.optString("readAt") > ble!!.optString("readAt")) -> {
                                ble = value
                                watchSource = c.getString(1)
                            }
                        }
                    }
                }
                if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, dayStr, ""))
                val preferredProvider = meta(db, "provider", HC)
                if (hc == null && old == null && !(preferredProvider == SAMSUNG && samsung != null) && ble == null) {
                    continue
                }
                val out = HealthProviderProjection.day(hc, samsung, old, preferredProvider)
                if (ble != null && meta(db, "logWatchSteps", "false").toBoolean() && (primary == BLE || out.isNull("steps"))) {
                    out.put("steps", ble!!.get("steps"))
                    out.put("stepsSource", BLE)
                    out.put("stepsObservedAt", ble!!.optString("readAt"))
                    out.put("stepsDeviceId", watchSource)
                }
                db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(dayStr)).use { dirty ->
                    if (dirty.moveToFirst()) out.put("partial", true)
                }
                out.put("day", dayStr)
                days.put(dayStr, out)
            }
            val out = JSONObject()
                .put("version", 1)
                .put("days", days)
                .put("preferences", preferences())
                .put("nativeJournal", true)
                .put("warnings", JSONArray())
            for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) {
                val value = meta(db, key, "")
                out.put(key, if (value.isEmpty()) JSONObject.NULL else value)
            }
            return out
        }
    }

    @Throws(Exception::class)
    private fun sleepSessions(db: SQLiteDatabase, day: String, source: String): JSONArray {
        val result = JSONArray()
        db.rawQuery(
            "SELECT json FROM records WHERE channel=? AND type='SleepSessionRecord' AND day=? AND (?='' OR source=?) ORDER BY sort_ms DESC LIMIT 100",
            arrayOf(HC, day, source, source)
        ).use { c ->
            while (c.moveToNext()) {
                val r = JSONObject(c.getString(0))
                val data = r.getJSONObject("data")
                val stages = JSONArray()
                val raw = data.optJSONArray("stages")
                if (raw != null) {
                    for (i in 0 until raw.length()) {
                        val s = raw.getJSONObject(i)
                        stages.put(
                            JSONObject()
                                .put("start", iso(s.getLong("startMs")))
                                .put("end", iso(s.getLong("endMs")))
                                .put("type", s.getInt("type"))
                                .put("isAsleep", s.getBoolean("isAsleep"))
                        )
                    }
                }
                result.put(
                    JSONObject()
                        .put("id", r.getString("recordId"))
                        .put("source", r.getString("source"))
                        .put("start", iso(r.getLong("startMs")))
                        .put("end", iso(r.getLong("endMs")))
                        .put("durationMinutes", (r.getLong("endMs") - r.getLong("startMs")) / 60_000.0)
                        .put("stages", stages)
                )
            }
        }
        return result
    }

    fun dirtyDays(): JSONArray {
        synchronized(LOCK) {
            val days = JSONArray()
            readableDatabase.rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 500", null).use { c ->
                while (c.moveToNext()) days.put(c.getString(0))
            }
            return days
        }
    }

    @Throws(Exception::class)
    fun needsSourceDay(date: String, source: String): Boolean {
        synchronized(LOCK) {
            val db = readableDatabase
            val read: Long
            db.rawQuery(
                "SELECT json FROM days WHERE day=? AND channel=? AND source=?",
                arrayOf(date, HC, source)
            ).use { c ->
                if (!c.moveToFirst()) return true
                read = try {
                    Instant.parse(JSONObject(c.getString(0)).optString("readAt")).toEpochMilli()
                } catch (_: Exception) {
                    return true
                }
            }
            if (date == day(System.currentTimeMillis()) && System.currentTimeMillis() - read > 5 * 60_000L) {
                return true
            }
            db.rawQuery(
                "SELECT MAX(modified_ms) FROM records WHERE day=? AND channel=? AND source=?",
                arrayOf(date, HC, source)
            ).use { c ->
                if (c.moveToFirst() && c.getLong(0) > read) return true
            }
            db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(date)).use { c ->
                return c.moveToFirst()
            }
        }
    }

    @Throws(Exception::class)
    fun sourceDaySeed(date: String, source: String): JSONObject {
        synchronized(LOCK) {
            val db = readableDatabase
            val out = JSONObject()
            val seen = HashSet<String>()
            db.rawQuery(
                "SELECT type,json FROM records WHERE day=? AND channel=? AND source=? ORDER BY sort_ms DESC",
                arrayOf(date, HC, source)
            ).use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(0)
                    if (!seen.add(type)) continue
                    val record = JSONObject(c.getString(1))
                    val data = record.getJSONObject("data")
                    var key: String? = null
                    when (type) {
                        "WeightRecord" -> key = "weightKg"
                        "BodyFatRecord" -> key = "bodyFatPercent"
                        "OxygenSaturationRecord" -> key = "oxygenSaturationPercent"
                        "Vo2MaxRecord" -> key = "vo2Max"
                        "HeartRateVariabilityRmssdRecord" -> key = "hrvRmssdMs"
                        "RespiratoryRateRecord" -> key = "respiratoryRate"
                        "BloodGlucoseRecord" -> key = "bloodGlucoseMmolL"
                        "BloodPressureRecord" -> {
                            out.put("bloodPressureSystolic", data.opt("systolic"))
                                .put("bloodPressureDiastolic", data.opt("diastolic"))
                        }
                    }
                    if (key != null) out.put(key, data.opt("value"))
                }
            }
            return out
        }
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun page(
        from: String,
        to: String,
        channel: String,
        source: String,
        before: Long,
        beforeTime: Long,
        limit: Int,
        recordType: String = "",
        includeSummaries: Boolean = true
    ): JSONObject {
        if (!validDay(from) || !validDay(to) || from > to) {
            throw IllegalArgumentException("Invalid health period")
        }
        if (channel.isNotEmpty() && channel !in listOf(HC, BLE, LEGACY, SAMSUNG, "journal")) {
            throw IllegalArgumentException("Invalid channel")
        }
        require(recordType.length <= 80) { "Invalid health record type" }
        synchronized(LOCK) {
            val db = readableDatabase
            val boundedLimit = max(1, min(50, limit))
            val args = ArrayList<String>(
                listOf(
                    from,
                    to,
                    (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(),
                    (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(),
                    (if (before > 0L) before else Long.MAX_VALUE).toString()
                )
            )
            var where = "day>=? AND day<=? AND (sort_ms<? OR (sort_ms=? AND row_id<?))"
            if ("journal" == channel) {
                where += " AND channel<>'ble_watch'"
            } else if (channel.isNotEmpty()) {
                where += " AND channel=?"
                args.add(channel)
            }
            if (source.isNotEmpty()) {
                where += " AND source=?"
                args.add(source)
            }
            if (recordType.isNotEmpty()) {
                where += " AND type=?"
                args.add(recordType)
            }
            val rows = JSONArray()
            var next = 0L
            var nextTime = 0L
            db.rawQuery(
                "SELECT row_id,json,sort_ms FROM records WHERE $where ORDER BY sort_ms DESC,row_id DESC LIMIT ${boundedLimit + 1}",
                args.toTypedArray()
            ).use { c ->
                while (c.moveToNext()) {
                    if (rows.length() == boundedLimit) {
                        val lastObj = rows.getJSONObject(rows.length() - 1)
                        next = lastObj.getLong("cursor")
                        nextTime = lastObj.getLong("cursorTime")
                        break
                    }
                    val row = JSONObject(c.getString(1))
                    row.put("cursor", c.getLong(0))
                    row.put("cursorTime", c.getLong(2))
                    rows.put(row)
                }
            }
            val origins = JSONArray()
            val originArgs = arrayListOf<String>()
            var originWhere = ""
            if (!includeSummaries) {
                originWhere = " WHERE day>=? AND day<=?"
                originArgs.add(from); originArgs.add(to)
                if (channel == "journal") originWhere += " AND channel<>'ble_watch'"
                else if (channel.isNotEmpty()) { originWhere += " AND channel=?"; originArgs.add(channel) }
                if (recordType.isNotEmpty()) { originWhere += " AND type=?"; originArgs.add(recordType) }
            }
            db.rawQuery(
                "SELECT channel,source,MAX(row_id),json FROM records$originWhere GROUP BY channel,source ORDER BY channel,source LIMIT 200",
                originArgs.toTypedArray()
            ).use { c ->
                while (c.moveToNext()) {
                    origins.put(
                        JSONObject()
                            .put("channel", c.getString(0))
                            .put("source", c.getString(1))
                            .put("name", JSONObject(c.getString(3)).getJSONObject("data").optString("deviceName", ""))
                    )
                }
            }
            var summaries = JSONArray()
            if (includeSummaries) {
                val dayArgs = ArrayList<String>(listOf(from, to))
                var dayWhere = "day>=? AND day<=?"
                if ("journal" == channel) {
                    dayWhere += " AND (channel='health_connect' AND source='@aggregate' OR channel='legacy' AND NOT EXISTS (SELECT 1 FROM days AS hc WHERE hc.day=days.day AND hc.channel='health_connect' AND hc.source='@aggregate'))"
                } else if (channel.isNotEmpty()) {
                    dayWhere += " AND channel=?"
                    dayArgs.add(channel)
                }
                if (source.isNotEmpty()) {
                    dayWhere += " AND source=?"
                    dayArgs.add(source)
                } else {
                    dayWhere += " AND (channel<>'health_connect' OR source='@aggregate')"
                }
                db.rawQuery(
                    "SELECT day,channel,source,json FROM days WHERE $dayWhere ORDER BY day DESC,channel,source LIMIT 31",
                    dayArgs.toTypedArray()
                ).use { c ->
                    while (c.moveToNext()) {
                        val d = JSONObject(c.getString(3))
                        if (c.getString(1) == HC) {
                            d.put("sleepSessions", sleepSessions(db, c.getString(0), if ("@aggregate" == c.getString(2)) "" else c.getString(2)))
                        }
                        db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(c.getString(0))).use { dirty ->
                            if (dirty.moveToFirst()) d.put("partial", true)
                        }
                        d.put("channel", c.getString(1))
                        d.put("source", c.getString(2))
                        d.put("day", c.getString(0))
                        summaries.put(d)
                    }
                }
                if ("journal" == channel) {
                    summaries = JSONArray()
                    val preferred = meta(db, "provider", HC)
                    db.rawQuery(
                        "SELECT DISTINCT day FROM days WHERE day>=? AND day<=? AND channel<>'ble_watch' ORDER BY day DESC LIMIT 31",
                        arrayOf(from, to)
                    ).use { dates ->
                        while (dates.moveToNext()) {
                            val date = dates.getString(0)
                            var hc: JSONObject? = null
                            var samsung: JSONObject? = null
                            var legacy: JSONObject? = null
                            db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(date)).use { c ->
                                while (c.moveToNext()) {
                                    when {
                                        c.getString(0) == HC && "@aggregate" == c.getString(1) -> hc = JSONObject(c.getString(2))
                                        c.getString(0) == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = JSONObject(c.getString(2))
                                        c.getString(0) == LEGACY -> legacy = JSONObject(c.getString(2))
                                    }
                                }
                            }
                            if (hc == null && legacy == null && (preferred != SAMSUNG || samsung == null)) continue
                            if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, date, ""))
                            val selected = HealthProviderProjection.day(hc, samsung, legacy, preferred).put("day", date)
                            if (!selected.has("source")) {
                                selected.put("source", if (hc != null || samsung != null) "@aggregate" else "unknown")
                            }
                            db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(date)).use { dirty ->
                                if (dirty.moveToFirst() && selected.optString("channel") == HC) {
                                    selected.put("partial", true)
                                }
                            }
                            summaries.put(selected)
                        }
                    }
                }
            }
            return JSONObject()
                .put("records", rows)
                .put("nextCursor", next)
                .put("nextTime", nextTime)
                .put("sources", origins)
                .put("days", summaries)
                .put("preferences", preferences())
        }
    }

    @Throws(Exception::class)
    fun exportSnapshot(): JSONObject {
        return transaction { db ->
            val out = JSONObject().put("schemaVersion", 1)
            val records = JSONArray()
            val days = JSONArray()
            val deleted = JSONArray()
            var size = 0
            db.rawQuery("SELECT json FROM records ORDER BY row_id", null).use { c ->
                while (c.moveToNext()) {
                    val json = c.getString(0)
                    size += json.length
                    if (size > 18 * 1024 * 1024) {
                        throw IllegalStateException("Health export exceeds 18 MB; choose a shorter period in a future export")
                    }
                    records.put(JSONObject(json))
                }
            }
            db.rawQuery("SELECT day,channel,source,json FROM days ORDER BY day", null).use { c ->
                while (c.moveToNext()) {
                    days.put(
                        JSONObject()
                            .put("day", c.getString(0))
                            .put("channel", c.getString(1))
                            .put("source", c.getString(2))
                            .put("data", JSONObject(c.getString(3)))
                    )
                }
            }
            db.rawQuery("SELECT record_id,deleted_ms FROM deletions", null).use { c ->
                while (c.moveToNext()) {
                    deleted.put(JSONObject().put("recordId", c.getString(0)).put("deletedMs", c.getLong(1)))
                }
            }
            val sync = JSONObject()
            for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) {
                val value = meta(db, key, "")
                if (value.isNotEmpty()) sync.put(key, value)
            }
            out.put("records", records).put("days", days).put("deletions", deleted).put("syncMeta", sync)
            if (out.toString().length > 18 * 1024 * 1024) {
                throw IllegalStateException("Health export exceeds 18 MB")
            }
            out
        }
    }

    @Throws(Exception::class)
    private fun replace(db: SQLiteDatabase, snapshot: JSONObject) {
        if (snapshot.getInt("schemaVersion") != 1) {
            throw IllegalArgumentException("Unsupported health backup")
        }
        val records = snapshot.getJSONArray("records")
        val days = snapshot.getJSONArray("days")
        val deleted = snapshot.optJSONArray("deletions")
        if (records.length() > 100_000 || days.length() > 20_000 || snapshot.toString().length > 18 * 1024 * 1024) {
            throw IllegalArgumentException("Health backup too large")
        }
        db.delete("records", null, null)
        db.delete("days", null, null)
        db.delete("deletions", null, null)
        db.delete("dirty_days", null, null)
        if (deleted != null) {
            for (i in 0 until deleted.length()) {
                val d = deleted.getJSONObject(i)
                val v = ContentValues().apply {
                    put("record_id", text(d, "recordId", 200))
                    put("deleted_ms", time(d, "deletedMs"))
                }
                db.insertOrThrow("deletions", null, v)
            }
        }
        for (i in 0 until records.length()) {
            upsert(db, records.getJSONObject(i))
        }
        for (i in 0 until days.length()) {
            val d = days.getJSONObject(i)
            val channel = d.getString("channel")
            val source = d.getString("source")
            if (channel !in listOf(HC, BLE, LEGACY, SAMSUNG) || source.isEmpty() || source.length > 255) {
                throw IllegalArgumentException("Invalid day source")
            }
            saveDay(db, d.getString("day"), channel, source, d.getJSONObject("data"))
        }
        val sync = snapshot.optJSONObject("syncMeta")
        for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt")) {
            if (sync != null && !sync.isNull(key)) {
                putMeta(db, key, sync.getString(key))
            } else {
                db.delete("meta", "key=?", arrayOf(key))
            }
        }
        putMeta(db, "migrated_v1", "true")
    }

    @Throws(Exception::class)
    fun prepareRestore(snapshot: JSONObject): String {
        return transaction { db ->
            if (meta(db, "restore_pending", "").isNotEmpty()) {
                throw IllegalStateException("Restore already pending")
            }
            val old = exportSnapshot()
            db.execSQL("SAVEPOINT validate_health")
            try {
                replace(db, snapshot)
            } finally {
                db.execSQL("ROLLBACK TO validate_health")
                db.execSQL("RELEASE validate_health")
            }
            val token = UUID.randomUUID().toString()
            putMeta(db, "restore_old", old.toString())
            putMeta(db, "restore_next", snapshot.toString())
            putMeta(db, "restore_pending", token)
            token
        }
    }

    @Throws(Exception::class)
    fun installRestore(token: String) {
        transaction { db ->
            if (token != meta(db, "restore_pending", "")) {
                throw IllegalArgumentException("Invalid restore token")
            }
            replace(db, JSONObject(meta(db, "restore_next", "")))
            putMeta(db, "generation", (generation() + 1L).toString())
            null
        }
    }

    @Throws(Exception::class)
    fun finishRestore(token: String, commit: Boolean) {
        transaction { db ->
            if (token != meta(db, "restore_pending", "")) {
                throw IllegalArgumentException("Invalid restore token")
            }
            if (!commit) {
                replace(db, JSONObject(meta(db, "restore_old", "")))
            }
            db.delete("meta", "key IN ('restore_pending','restore_old','restore_next')", null)
            putMeta(db, "generation", (generation() + 1L).toString())
            null
        }
    }

    fun pendingRestore(): String {
        synchronized(LOCK) {
            return meta(readableDatabase, "restore_pending", "")
        }
    }

    @Throws(Exception::class)
    fun erase() {
        transaction { db ->
            val epoch = generation() + 1L
            db.delete("records", null, null)
            db.delete("days", null, null)
            db.delete("meta", null, null)
            db.delete("deletions", null, null)
            db.delete("dirty_days", null, null)
            putMeta(db, "generation", epoch.toString())
            putMeta(db, "migrated_v1", "true")
            null
        }
    }
}
