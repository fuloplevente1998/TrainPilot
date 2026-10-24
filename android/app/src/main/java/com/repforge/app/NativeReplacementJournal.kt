package com.repforge.app

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Durable redo receipt for the existing importer, stored privately outside all cloud backups. */
internal class NativeReplacementJournal(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "native-data-replacement.json"))
    data class Entry(val kind: String, val token: String?, val committed: Boolean, val payloadSha256: String? = null)
    fun exists() = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
    fun read(): Entry {
        val raw = file.openRead().use { input ->
            val bytes = input.readBytes()
            if (bytes.size > 4096) throw IOException("Invalid replacement receipt")
            JSONObject(String(bytes, Charsets.UTF_8))
        }
        check(raw.getInt("schema") == 1)
        val kind = raw.getString("kind")
        check(kind in setOf("json", "zip", "erase"))
        val token = raw.optString("token").takeIf { it.isNotEmpty() }
        check(if (kind == "erase") token == null else token != null && Regex("[a-f0-9-]{36}").matches(token))
        val hash = raw.optString("sha256").takeIf { it.isNotEmpty() }
        check(kind == "erase" || hash != null && Regex("[a-f0-9]{64}").matches(hash))
        return Entry(kind, token, raw.getBoolean("committed"), hash)
    }
    fun write(entry: Entry) {
        val json = JSONObject().put("schema", 1).put("kind", entry.kind).put("token", entry.token ?: "")
            .put("sha256", entry.payloadSha256 ?: "").put("committed", entry.committed).toString().toByteArray(Charsets.UTF_8)
        val out = file.startWrite()
        try { out.write(json); file.finishWrite(out) } catch (error: Throwable) { file.failWrite(out); throw error }
    }
    fun clear() {
        file.delete()
        check(!exists()) { "Cannot retire replacement receipt" }
    }
}
