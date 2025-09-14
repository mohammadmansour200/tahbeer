package com.tahbeer.app.home.presentation.transcription_list

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tahbeer.app.home.domain.model.TranscriptionItem
import com.tahbeer.app.home.presentation.transcription_list.components.TranscriptionListItem

@Composable
fun TranscriptionList(
    transcriptionItems: List<TranscriptionItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(transcriptionItems, { it.id }) { item ->
            TranscriptionListItem(
                modifier = Modifier.animateItem(),
                transcriptionItem = item,
                onItemClick = { onItemClick(item.id) },
                onDeleteClick = {},
            )
        }
    }
}