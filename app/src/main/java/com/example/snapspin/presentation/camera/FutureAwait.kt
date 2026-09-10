package com.example.snapspin.presentation.camera

import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Espera a un `ListenableFuture` de CameraX sin bloquear el hilo principal. */
internal suspend fun <T> ListenableFuture<T>.await(executor: Executor): T =
    suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel(false) }
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (e: ExecutionException) {
                    continuation.resumeWithException(e.cause ?: e)
                } catch (e: Throwable) {
                    continuation.resumeWithException(e)
                }
            },
            executor,
        )
    }
