package com.repforge.app

/**
 * Process-local admission fence for SAF backup/export staging. The durable restore/erase
 * epoch is separately owned by NativeDataReplacement. No SDK IPC is held under this monitor.
 *
 * begin/end wrap user initiated backup workflows. publish is limited to short journal writes,
 * preventing an export snapshot from starting between commit validation and journal publication.
 */
internal object NativeHealthBackupGate {
    private var active = 0

    @Synchronized fun begin() { active++ }

    @Synchronized fun end() {
        check(active > 0) { "Unbalanced health backup admission" }
        active--
    }

    @Synchronized fun available(): Boolean = active == 0

    @Synchronized fun <T> publish(operation: () -> T): T {
        check(active == 0) { "Backup or restore is in progress" }
        return operation()
    }
}
