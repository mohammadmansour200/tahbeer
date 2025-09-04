package com.tahbeer.app.home.domain.settings

class ModelException(val modelError: ModelError, cause: Throwable? = null) :
    Exception(modelError.name, cause)

enum class ModelError {
    NETWORK_ERROR,
    DOWNLOAD_FAILED,
    INSUFFICIENT_SPACE,
    UNKNOWN_ERROR
}