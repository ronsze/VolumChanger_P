package com.sdbk.domain.usecase

import com.sdbk.domain.location.LocationEntity
import kotlinx.coroutines.CoroutineScope

interface InsertLocationUseCase {
    operator fun invoke(
        location: LocationEntity,
        scope: CoroutineScope,
        successAction: (LocationEntity) -> Unit,
        failureAction: (Throwable) -> Unit
    )
}