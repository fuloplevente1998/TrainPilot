package com.repforge.app

import kotlinx.coroutines.CancellationException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Foreground orchestration only: existing adapters own SDK IO and the journal owns all data. */
internal object NativeHealthSync {
    class Checkpoint(
        val scope: String = "", val token: String = "", val initialized: Boolean = false,
        val revision: Long = 0
    ) {
        override fun toString(): String = "Health checkpoint (private)"
    }
    class ChangePage(val nextToken: String, val hasMore: Boolean, val days: Set<LocalDate>, val deleted: Boolean = false) {
        override fun toString(): String = "Health change page (private)"
    }
    enum class Stage { PREPARING, CHANGES, DAYS, COMPLETE }
    data class Progress(val stage: Stage, val completed: Int = 0, val total: Int = 0, val pages: Int = 0)
    data class Report(val full: Boolean, val completedDays: Int, val failedDays: List<LocalDate>) {
        val successful: Boolean get() = failedDays.isEmpty()
    }
    interface Provider {
        val changesSupported: Boolean
        suspend fun createToken(): String
        /** Applies exactly one page durably under the publication guard before returning it. */
        suspend fun pollPage(token: String): ChangePage
        fun invalidToken(error: Throwable): Boolean
        /** Returns false for a partial read; received fields are still durable and visible. */
        suspend fun readDay(day: LocalDate): Boolean
        suspend fun requireIdentity()
    }
    interface Journal {
        suspend fun checkpoint(): Checkpoint
        suspend fun dirtyDays(): Set<LocalDate>
        suspend fun attempt(at: Instant)
        suspend fun commit(expected: Checkpoint, next: Checkpoint, full: Boolean, at: Instant)
    }

    fun recentDays(today: LocalDate, count: Int): List<LocalDate> {
        require(count in 1..30)
        return (count - 1 downTo 0).map { today.minusDays(it.toLong()) }
    }

    /** Local dates, not multiples of 24 hours. Today ends at the captured current instant. */
    fun window(day: LocalDate, zone: ZoneId, now: Instant): Pair<Instant, Instant> {
        val start = day.atStartOfDay(zone).toInstant()
        val end = minOf(day.plusDays(1).atStartOfDay(zone).toInstant(), now)
        require(end.isAfter(start)) { "Health day has not started yet" }
        return start to end
    }

    suspend fun run(
        provider: Provider, journal: Journal, scope: String, today: LocalDate,
        explicitFull: Boolean, guard: () -> Unit, progress: suspend (Progress) -> Unit,
        now: () -> Instant = Instant::now
    ): Report {
        guard()
        progress(Progress(Stage.PREPARING))
        journal.attempt(now())
        val old = journal.checkpoint()
        var full = explicitFull || !old.initialized || old.scope != scope ||
            (provider.changesSupported && old.token.isEmpty())
        var candidate = old.token
        var pages = 0
        val changed = sortedSetOf<LocalDate>()
        suspend fun poll() {
            do {
                guard()
                provider.requireIdentity()
                val page = provider.pollPage(candidate)
                guard()
                require(page.nextToken.isNotEmpty()) { "Missing change token" }
                check(!page.hasMore || page.nextToken != candidate) { "Change log made no progress" }
                if (page.deleted) full = true // Unknown/legacy record IDs may lack a local day: reconcile the full window.
                candidate = page.nextToken
                changed.addAll(page.days.filter { it <= today })
                progress(Progress(Stage.CHANGES, pages = ++pages))
                // A bounded flight never silently truncates/acknowledges an unfinished log.
                check(pages <= 500) { "Change log exceeds foreground budget; retry full sync" }
            } while (page.hasMore)
        }
        if (provider.changesSupported) {
            if (!full) {
                try { poll() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    guard()
                    if (!provider.invalidToken(error)) throw error
                    full = true
                }
            }
            if (full) {
                provider.requireIdentity()
                candidate = provider.createToken() // Before snapshot: concurrent SDK writes cannot fall in a gap.
                require(candidate.isNotEmpty()) { "Missing change token" }
            }
        } else candidate = "" // Samsung SDK has no change-log contract.

        val failures = sortedSetOf<LocalDate>()
        val completed = mutableSetOf<LocalDate>()
        suspend fun read(days: Set<LocalDate>) {
            check(days.size <= 530) { "Too many dirty Health days; retry in smaller batches" }
            var done = 0
            progress(Progress(Stage.DAYS, done, days.size, pages))
            for (day in days.sorted()) {
                guard()
                provider.requireIdentity()
                val success = try { provider.readDay(day) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { guard(); false }
                guard()
                if (success) { failures.remove(day); completed.add(day) }
                else { failures.add(day); completed.remove(day) }
                progress(Progress(Stage.DAYS, ++done, days.size, pages))
            }
        }
        val days = recentDays(today, if (full) 30 else 2).toMutableSet()
        if (provider.changesSupported) days.addAll(journal.dirtyDays().filter { it <= today })
        days.addAll(changed)
        read(days)
        if (full && provider.changesSupported) {
            changed.clear()
            poll()
            // Include deletes/updates that happened while reading the 30-day snapshot.
            read(changed)
        }
        guard()
        provider.requireIdentity() // Permission IPC outside the local publication lease.
        if (failures.isEmpty()) journal.commit(old, Checkpoint(scope, candidate, true), full, now())
        progress(Progress(Stage.COMPLETE, completed.size, completed.size + failures.size, pages))
        return Report(full, completed.size, failures.toList())
    }
}
