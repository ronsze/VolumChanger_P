package com.sdbk.domain.usecase

import com.sdbk.domain.location.LocationEntity
import kotlinx.coroutines.CoroutineScope

interface GetLocationUseCase {
    operator fun invoke(
        scope: CoroutineScope,
        successAction: (List<LocationEntity>) -> Unit,
        failureAction: (Throwable) -> Unit
    )

    operator fun invoke(
        id: Int,
        scope: CoroutineScope,
        successAction: (LocationEntity) -> Unit,
        failureAction: (Throwable) -> Unit
    )
}