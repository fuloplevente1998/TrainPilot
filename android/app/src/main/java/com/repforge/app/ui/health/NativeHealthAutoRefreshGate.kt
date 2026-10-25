package com.repforge.app.ui.health

/**
 * Foreground Health refresh admission only. The caller owns permissions, execution/restore
 * admission and the actual Health Connect / Samsung SDK request. Never persist a provider
 * result based on this UI ticket alone: validate the journal generation at each write site.
 */
internal class NativeHealthAutoRefreshGate(
    private val intervalMs: Long = 10L * 60L * 1000L
) {
    init { require(intervalMs > 0L) }

    enum class Trigger { AUTO, MANUAL }

    data class Snapshot(
        val provider: String,
        val day: String,
        val datasetGeneration: Long,
        val screenGeneration: Long
    )

    data class Ticket internal constructor(
        val serial: Long,
        val trigger: Trigger,
        val snapshot: Snapshot,
        internal val invalidation: Long
    )

    private var serial = 0L
    private var invalidation = 0L
    private var running: Ticket? = null
    private var lastAutoStartMs: Long? = null
    private var lastAutoKey: String? = null

    /**
     * One provider operation at a time. Auto never prompts for permissions. The call site
     * must pass true for safeToRead only while foreground, inactive, and outside restore/
     * ownership/finalization/backup gates. Manual bypasses the auto-only time interval.
     */
    @Synchronized
    fun begin(
        trigger: Trigger,
        elapsedMs: Long,
        optedIn: Boolean,
        permitted: Boolean,
        safeToRead: Boolean,
        snapshot: Snapshot
    ): Ticket? {
        if (running != null || !permitted || !safeToRead || (trigger == Trigger.AUTO && !optedIn)) return null
        if (trigger == Trigger.AUTO) {
            val previous = lastAutoStartMs
            val key = "${snapshot.provider}|${snapshot.day}"
            if (previous != null && lastAutoKey == key &&
                elapsedMs >= previous && elapsedMs - previous < intervalMs) return null
        }
        return Ticket(++serial, trigger, snapshot, invalidation).also {
            running = it
            if (trigger == Trigger.AUTO) {
                lastAutoStartMs = elapsedMs
                lastAutoKey = "${snapshot.provider}|${snapshot.day}"
            }
        }
    }

    /** Invalidate a screen/provider/day dataset without releasing an SDK operation still in flight. */
    @Synchronized
    fun invalidate() { invalidation++ }

    @Synchronized
    fun isCurrent(ticket: Ticket, snapshot: Snapshot, permitted: Boolean, safeToRead: Boolean): Boolean =
        running == ticket && invalidation == ticket.invalidation &&
            snapshot == ticket.snapshot && permitted && safeToRead

    @Synchronized
    fun finish(ticket: Ticket) {
        if (running == ticket) running = null
    }
}

/** Shared across navigation-entry ViewModel recreation; never retains an Activity or Context. */
internal object NativeHealthAutoRefreshCoordinator {
    val gate = NativeHealthAutoRefreshGate()
}
