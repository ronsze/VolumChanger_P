package com.sdbk.data.usecase

import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.repository.location.LocationRepository
import com.sdbk.domain.usecase.InsertLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InsertLocationUseCaseImpl(
    private val repository: LocationRepository
): InsertLocationUseCase {
    override fun invoke(
        location: LocationEntity,
        scope: CoroutineScope,
        successAction: (LocationEntity) -> Unit,
        failureAction: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = kotlin.runCatching { withContext(Dispatchers.IO) { repository.insertLocation(location) } }
            result.onSuccess { successAction(it) }
            result.onFailure { failureAction(it) }
        }
    }
}