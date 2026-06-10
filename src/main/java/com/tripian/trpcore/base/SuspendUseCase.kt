package com.tripian.trpcore.base

import com.tripian.trpcore.repository.MiscRepository
import com.tripian.trpcore.repository.TripianUserRepository
import com.tripian.trpcore.repository.base.ErrorModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Coroutine UseCase base. Concrete UseCases implement [execute] with their
 * business logic and the ViewModel call site uses
 * `runCatching { useCase(params) }` to consume the result.
 *
 * Threading: [invoke] dispatches to [Dispatchers.IO] so concrete UseCases
 * never have to think about it. ViewModels stay on the main dispatcher via
 * `viewModelScope.launch`.
 *
 * Error contract: anything thrown by [execute] (Retrofit HttpException,
 * IOException, etc.) is funneled through [ApiErrorMapper] and rethrown as
 * an [ErrorModel] so the call site can `runCatching` it uniformly.
 */
abstract class SuspendUseCase<R, P> {

    @Inject
    internal lateinit var miscRepository: MiscRepository

    @Inject
    internal lateinit var tripianUserRepository: TripianUserRepository

    protected abstract suspend fun execute(params: P): R

    suspend operator fun invoke(params: P): R = withContext(Dispatchers.IO) {
        try {
            execute(params)
        } catch (t: Throwable) {
            throw ApiErrorMapper.map(t, miscRepository, tripianUserRepository)
        }
    }
}
