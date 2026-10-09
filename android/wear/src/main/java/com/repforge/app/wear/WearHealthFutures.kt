package com.repforge.app.wear

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/** Health Services commands complete with null (Void), while queries return data. */
internal object WearHealthFutures {
    fun <T : Any> awaitValue(future: ListenableFuture<T>, executor: Executor,
                            isActive: () -> Boolean, onFailure: (Exception) -> Unit,
                            onSuccess: (T) -> Unit) {
        listen(future, executor, isActive, onFailure) {
            onSuccess(checkNotNull(future.get()) { "A mérési szolgáltatás nem adott vissza adatot." })
        }
    }

    fun awaitCompletion(future: ListenableFuture<Void>, executor: Executor,
                        isActive: () -> Boolean, onFailure: (Exception) -> Unit,
                        onSuccess: () -> Unit) {
        listen(future, executor, isActive, onFailure) {
            future.get() // A successful Void future returns null. Do not pass it to a Kotlin lambda.
            onSuccess()
        }
    }

    fun isLegacyNullCompletionError(message: String): Boolean =
        message.contains("Parameter specified as non-null is null") &&
            message.contains("parameter it") &&
            (message.contains("com.repforge.app.wear.WearHealthService.capabilities") ||
                message.contains("com.repforge.app.wear.WearHealthService.end"))

    private fun listen(future: ListenableFuture<*>, executor: Executor,
                       isActive: () -> Boolean, onFailure: (Exception) -> Unit,
                       success: () -> Unit) {
        future.addListener({
            if (isActive()) try { success() } catch (error: Exception) { onFailure(error) }
        }, executor)
    }
}
