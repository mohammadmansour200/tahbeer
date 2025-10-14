package com.tahbeer.app.home.presentation.transcription_list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tahbeer.app.home.presentation.transcription_list.components.TranscriptionListItem

@Composable
fun TranscriptionList(
    transcriptionListState: TranscriptionListState,
    onItemClick: (String) -> Unit,
    onItemDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (transcriptionListState.isLoading) {
        Box(modifier = modifier) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    LazyColumn(modifier = modifier) {
        items(transcriptionListState.transcriptions, { it.id }) { item ->
            TranscriptionListItem(
                modifier = Modifier.animateItem(),
                transcriptionItem = item,
                isSelected = transcriptionListState.selectedTranscriptionId == item.id,
                onItemClick = { onItemClick(item.id) },
                onDeleteClick = { onItemDelete(item.id) },
            )
        }
    }
}