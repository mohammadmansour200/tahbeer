package com.tahbeer.app.home.domain.model

import com.google.mlkit.nl.translate.TranslateLanguage

data class MlkitModel(
    val lang: String,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false
)

object MlkitModelList {
    val models = TranslateLanguage.getAllLanguages().map {
        MlkitModel(it)
    }
}
