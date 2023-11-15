package com.sdbk.data.usecase

import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.repository.location.LocationRepository
import com.sdbk.domain.usecase.DeleteLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeleteLocationUseCaseImpl(
    private val repository: LocationRepository
): DeleteLocationUseCase {
    override fun invoke(
        location: LocationEntity,
        scope: CoroutineScope,
        successAction: () -> Unit,
        failureAction: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = kotlin.runCatching { withContext(Dispatchers.IO) { repository.deleteLocation(location) } }
            result.onSuccess { successAction() }
            result.onFailure { failureAction(it) }
        }
    }
}