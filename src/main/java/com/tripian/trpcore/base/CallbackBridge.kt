package com.tripian.trpcore.base

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridge from TRPRest's two-callback API
 * (`success: (T) -> Unit, error: (Throwable?) -> Unit`) to a coroutine.
 *
 * Used by repositories during the RxJava → Coroutines migration to wrap a
 * single TRPRest call into a suspend fun. Any error the TRPRest layer
 * surfaces (typically Retrofit's HttpException for non-2xx responses) is
 * rethrown so the central [ApiErrorMapper] inside [SuspendUseCase.invoke]
 * can translate it into the domain ErrorModel.
 *
 * Cancellation: the coroutine resumes only while still active, so cancelling
 * the calling scope won't surface a late callback as a duplicate resume.
 */
internal suspend inline fun <T : Any> awaitCallback(
    crossinline block: (success: (T) -> Unit, error: (Throwable?) -> Unit) -> Unit
): T = suspendCancellableCoroutine { cont ->
    block(
        { res -> if (cont.isActive) cont.resume(res) },
        { err -> if (cont.isActive) cont.resumeWithException(err ?: Throwable("Unexpected error code -1")) }
    )
}
