package com.repforge.app

/** Render-time clock identity; a delayed Skip or +/- action cannot control a replacement timer. */
data class NativeTimerTarget(val workoutId: String, val generation: Long, val key: String)
