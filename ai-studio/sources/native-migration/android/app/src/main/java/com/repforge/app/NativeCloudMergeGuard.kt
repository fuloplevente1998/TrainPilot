package com.repforge.app

import android.content.Context
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONObject

/** Optimistic recheck after Drive network I/O, using the existing canonical sync representation. */
internal object NativeCloudMergeGuard {
    fun requireUnchanged(beforeNetwork: JSONObject, current: JSONObject) {
        check(LegacySyncIdentity18.canonical(beforeNetwork) == LegacySyncIdentity18.canonical(current)) {
            "A helyi adatok szinkron közben megváltoztak. Indítsd újra a szinkronizálást; a helyi módosítások megmaradtak."
        }
    }

    /** Accept through the existing recoverable local importer before making a snapshot visible.
     * Failed publication leaves the accepted data durable and the old cloudBase available for retry.
     * Network I/O runs outside the workout/replacement fence; this is not a distributed transaction.
     */
    suspend fun installAndPublish(
        context: Context,
        beforeNetwork: JSONObject,
        merged: JSONObject,
        photos: NativeCloudPhotoStage,
        owner: String,
        publish: suspend (JSONObject) -> Unit
    ): NativeDataReplacement.Continuation {
        require(owner.isNotBlank()) { "Missing verified cloud owner" }
        val app = context.applicationContext
        val db = TrainPilotDatabase.getInstance(app)
        val migrator = LegacyWebViewMigrator(app, db, TrainPilotSettingsStore(app))
        val acceptedJson = merged.toString()
        val continuation = NativeDataReplacement.exclusively(app) {
            requireUnchanged(beforeNetwork, migrator.exportBackupJson(includeHealthJournal = false))
            BackupRestoreManager(app).installCloudMerge(JSONObject(acceptedJson), photos)
            NativeDataReplacement.continuationAfterReplacement()
        }
        continuation.requireCurrent(app)
        publish(JSONObject(acceptedJson))
        NativeDataReplacement.mutate(app) {
            continuation.requireCurrent(app)
            db.keyValueDocumentDao().putDocument(
                KeyValueDocumentEntity("cloudBase:$owner", acceptedJson, System.currentTimeMillis()))
        }
        return continuation
    }
}
