package com.repforge.app.wear

/** Phone-authored pause metadata is independent of local set-edit delivery sequence. */
data class WearActiveInterval(val start: Long,val end: Long = 0L)

internal fun preserveWorkoutPause(value: WearWorkout,source: WearWorkout): WearWorkout = value.copy(
    workoutPaused=source.workoutPaused,pauseRevision=source.pauseRevision,activeIntervals=source.activeIntervals,
    resumeAutomaticOnContinue=source.resumeAutomaticOnContinue,pausedRestMillis=source.pausedRestMillis,
    automation=if(source.workoutPaused || source.pauseRevision>value.pauseRevision)source.automation else value.automation,
    restEndAt=if(source.workoutPaused)0 else if(source.pauseRevision>value.pauseRevision)source.restEndAt else value.restEndAt)

internal fun mergeWorkoutPause(current: WearWorkout?,incoming: WearWorkout,now: Long,wall: Long): WearWorkout {
    if(current==null||current.workoutId!=incoming.workoutId)return incoming
    if(incoming.pauseRevision<current.pauseRevision)return preserveWorkoutPause(incoming,current)
    if(incoming.pauseRevision==current.pauseRevision){
        if(current.workoutPaused)return preserveWorkoutPause(incoming,current)
        return incoming.copy(resumeAutomaticOnContinue=current.resumeAutomaticOnContinue,pausedRestMillis=current.pausedRestMillis)
    }
    if(current.workoutPaused&&incoming.workoutPaused)return preserveWorkoutPause(incoming,current).copy(pauseRevision=incoming.pauseRevision,activeIntervals=incoming.activeIntervals ?: current.activeIntervals)
    var next=incoming
    val a=current.automation
    if(incoming.workoutPaused){
        next=next.copy(resumeAutomaticOnContinue=a?.enabled==true&&!a.paused&&!a.interrupted,
            pausedRestMillis=(current.restEndAt-wall).coerceAtLeast(0),restEndAt=0)
        if(a?.owner=="watch"){
            // Freeze the existing owner, not a possibly delayed mirrored clock.
            // Tick accounts only the work that really elapsed before reception.
            val frozen=WearAutomaticEngine.pause(current,now,a.boot).automation
            next=next.copy(automation=frozen)
        }else next=next.copy(automation=incoming.automation?.copy(paused=true,deadlineAt=0))
    }else if(current.workoutPaused){
        next=next.copy(restEndAt=if(current.pausedRestMillis>0)wall+current.pausedRestMillis else incoming.restEndAt,
            resumeAutomaticOnContinue=false,pausedRestMillis=0)
        if(a?.owner=="watch")next=next.copy(automation=a.copy(
            paused=if(current.resumeAutomaticOnContinue)false else a.paused,anchorMs=now))
    }
    return next
}
