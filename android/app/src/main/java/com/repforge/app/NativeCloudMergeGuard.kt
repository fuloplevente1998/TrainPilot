package com.repforge.app

import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONObject

/** Optimistic recheck after Drive network I/O, using the existing canonical sync representation. */
internal object NativeCloudMergeGuard {
    fun requireUnchanged(beforeNetwork: JSONObject, current: JSONObject) {
        check(LegacySyncIdentity18.canonical(beforeNetwork) == LegacySyncIdentity18.canonical(current)) {
            "A helyi adatok szinkron közben megváltoztak. Indítsd újra a szinkronizálást; a helyi módosítások megmaradtak."
        }
    }
}
