package com.tahbeer.app.home.presentation.transcription_list.components

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.core.domain.model.TranscriptionStatus
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.core.presentation.utils.findActivity
import com.tahbeer.app.home.presentation.transcription_list.TranscriptionListAction

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun TranscriptionListItem(
    transcriptionItem: TranscriptionItem,
    isSelected: Boolean,
    onAction: (TranscriptionListAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val context = LocalContext.current
        val windowSizeClass = context.findActivity()?.let { calculateWindowSizeClass(it) }
        val isScreenExpanded = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

        val targetBackgroundColor =
            if (!isSelected) Color.Transparent else MaterialTheme.colorScheme.primaryContainer

        val backgroundColor by animateColorAsState(
            targetValue = targetBackgroundColor,
        )

        val targetTextColor =
            if (!isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimaryContainer


        val textColor by animateColorAsState(
            targetValue = targetTextColor,
        )

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onAction(TranscriptionListAction.OnTranscriptClick(transcriptionItem.id))
                },
            colors = ListItemDefaults.colors(
                containerColor = if (isScreenExpanded) backgroundColor else Color.Transparent
            ),
            headlineContent = {
                Text(
                    text = transcriptionItem.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isScreenExpanded) textColor else Color.Unspecified
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
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isScreenExpanded) textColor else Color.Unspecified
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
            },
            trailingContent = {

                // More options button
                var menuExpanded by remember { mutableStateOf(false) }
                IconButton(onClick = { menuExpanded = !menuExpanded }) {
                    IconWithTooltip(
                        icon = Icons.Filled.MoreVert,
                        text = stringResource(R.string.more_options),
                        tint = if (isScreenExpanded) textColor else LocalContentColor.current
                    )
                }

                var showEditDialog by remember { mutableStateOf(false) }

                if (showEditDialog) {
                    var newTitle by remember { mutableStateOf(transcriptionItem.title) }
                    AlertDialog(
                        onDismissRequest = {
                            showEditDialog = false
                        },
                        title = {
                            Text(text = stringResource(R.string.edit_title))
                        },
                        text = {
                            TextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onAction(
                                        TranscriptionListAction.OnTranscriptEditTitle(
                                            transcriptionId = transcriptionItem.id,
                                            newTitle = newTitle
                                        )
                                    )
                                    showEditDialog = false
                                }
                            ) {
                                Text(stringResource(R.string.confirm_btn))
                            }
                        },
                    )
                }


                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_title)) },
                        onClick = {
                            showEditDialog = true
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            menuExpanded = false
                            onAction(
                                TranscriptionListAction.OnTranscriptDelete(
                                    transcriptionItem.id
                                )
                            )
                        }
                    )
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