package com.tahbeer.app.home.domain.settings

class ModelDownloadException(val downloadError: DownloadError, cause: Throwable? = null) :
    Exception(downloadError.name, cause)

enum class DownloadError {
    NETWORK_ERROR,
    DOWNLOAD_FAILED,
    INSUFFICIENT_SPACE,
}