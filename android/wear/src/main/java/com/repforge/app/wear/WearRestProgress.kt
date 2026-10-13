package com.repforge.app.wear

/** Each second consumes the same fraction of this rest phase, including pauses. */
internal fun wearRestProgress(workout: WearWorkout, remainingSeconds: Int): Float {
    val automatic=workout.automation
    val duration=if(automatic?.enabled==true&&automatic.phase=="rest")automatic.restSeconds else workout.restSeconds
    return (remainingSeconds.toFloat()/duration.coerceAtLeast(1)).coerceIn(0f,1f)
}
