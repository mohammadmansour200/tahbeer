package com.tahbeer.app.details.presentation

import kotlinx.coroutines.CompletableDeferred

sealed interface DetailScreenEvent {
    object Error : DetailScreenEvent
    object Success : DetailScreenEvent
    class PermissionRequired(
        val deferred: CompletableDeferred<Boolean>
    ) : DetailScreenEvent
}