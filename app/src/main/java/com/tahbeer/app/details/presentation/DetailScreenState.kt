package com.tahbeer.app.details.presentation

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.tahbeer.app.details.domain.model.ExportError

@Immutable
data class DetailScreenState(
    val isOperating: Boolean = false,
    val progress: Float? = null,
    val outputedFile: Uri? = null,
    val error: ExportError? = null,
    val detailedErrorMessage: String? = null
)