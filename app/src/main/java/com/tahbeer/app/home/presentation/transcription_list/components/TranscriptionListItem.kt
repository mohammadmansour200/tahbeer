package com.tahbeer.app.home.presentation.transcription_list.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.home.domain.model.TranscriptionItem
import com.tahbeer.app.home.domain.model.TranscriptionStatus

@Composable
fun TranscriptionListItem(
    transcriptionItem: TranscriptionItem,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TextButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onItemClick() },
            shape = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues()
        ) {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                ),
                headlineContent = {
                    Text(
                        text = transcriptionItem.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                supportingContent = {
                    Column {
                        when (transcriptionItem.status) {
                            TranscriptionStatus.SUCCESS -> {
                                transcriptionItem.result?.let { result ->
                                    Text(
                                        text = result[0].text,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TranscriptionStatus.PROCESSING -> {
                                Row {
                                    Text(
                                        text = stringResource(R.string.status_processing),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    transcriptionItem.progress?.let {
                                        Text(
                                            text = "${it.times(100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            TranscriptionStatus.ERROR_PROCESSING -> {
                                Text(
                                    text = stringResource(R.string.status_error_processing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            )
        }
        // Progress indicator
        if (transcriptionItem.status == TranscriptionStatus.PROCESSING) {
            Box(
                modifier = Modifier
                    .padding(bottom = 1.dp),
            ) {
                when (transcriptionItem.progress) {
                    null -> LinearProgressIndicator(Modifier.fillMaxWidth())

                    else -> LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = { transcriptionItem.progress },
                    )
                }
            }
        }
        HorizontalDivider()
    }
}