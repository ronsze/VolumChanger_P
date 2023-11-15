package com.sdbk.domain.usecase

import com.sdbk.domain.location.LocationEntity
import kotlinx.coroutines.CoroutineScope

interface DeleteLocationUseCase {
    operator fun invoke(
        location: LocationEntity,
        scope: CoroutineScope,
        successAction: () -> Unit,
        failureAction: (Throwable) -> Unit
    )
}