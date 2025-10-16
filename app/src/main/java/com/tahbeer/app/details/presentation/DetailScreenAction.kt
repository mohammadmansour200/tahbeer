package com.tahbeer.app.details.presentation

import android.net.Uri
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.details.domain.model.ExportFormat

sealed interface DetailScreenAction {
    data class OnExport(val exportFormat: ExportFormat, val transcriptionItem: TranscriptionItem) :
        DetailScreenAction

    data class OnSeek(val position: Long) : DetailScreenAction

    data class OnLoadMedia(val uri: Uri) : DetailScreenAction
}