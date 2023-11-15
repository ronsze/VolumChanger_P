package com.sdbk.data.usecase

import com.sdbk.domain.location.LocationEntity
import com.sdbk.domain.repository.location.LocationRepository
import com.sdbk.domain.usecase.GetLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GetLocationUseCaseImpl(
    private val repository: LocationRepository
): GetLocationUseCase {
    override fun invoke(
        scope: CoroutineScope,
        successAction: (List<LocationEntity>) -> Unit,
        failureAction: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = kotlin.runCatching { withContext(Dispatchers.IO) { repository.getLocationAll() } }
            result.onSuccess { successAction(it) }
            result.onFailure { failureAction(it) }
        }
    }

    override fun invoke(
        id: Int,
        scope: CoroutineScope,
        successAction: (LocationEntity) -> Unit,
        failureAction: (Throwable) -> Unit
    ) {
        scope.launch {
            val result = kotlin.runCatching { withContext(Dispatchers.IO) { repository.getLocation(id) } }
            result.onSuccess { successAction(it) }
            result.onFailure { failureAction(it) }
        }
    }
}