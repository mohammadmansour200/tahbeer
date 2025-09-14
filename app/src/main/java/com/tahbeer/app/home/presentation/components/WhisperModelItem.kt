package com.tahbeer.app.home.presentation.components

import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.home.domain.model.WhisperModel
import com.tahbeer.app.home.presentation.settings.SettingsAction

@Composable
fun WhisperModelItem(
    model: WhisperModel,
    onAction: (SettingsAction) -> Unit
) {
    val context = LocalContext.current
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        headlineContent = {
            Row {
                Text(
                    text = stringResource(
                        if (model.enOnly) R.string.settings_model_en_title else R.string.settings_model_multilingual_title
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Formatter.formatFileSize(
                        context,
                        model.size
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        supportingContent = {
            Text(
                text = stringResource(model.description),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AnimatedContent((model.downloadingProgress != null)) { isDownloading ->
                    if (!isDownloading) {
                        when (model.isDownloaded) {
                            true -> IconButton(
                                onClick = {
                                    onAction(
                                        SettingsAction.OnWhisperModelDelete(
                                            model.type
                                        )
                                    )
                                },
                            ) {
                                IconWithTooltip(
                                    icon = ImageVector.vectorResource(R.drawable.delete),
                                    text = stringResource(R.string.settings_model_delete),
                                )
                            }

                            false -> IconButton(
                                onClick = {
                                    onAction(
                                        SettingsAction.OnWhisperModelDownload(
                                            model.type
                                        )
                                    )
                                },
                            ) {
                                IconWithTooltip(
                                    icon = ImageVector.vectorResource(R.drawable.download),
                                    text = stringResource(R.string.settings_model_download),
                                )
                            }
                        }
                    } else {
                        when (model.downloadingProgress != null) {
                            true -> Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(2.dp),
                                    progress = {
                                        model.downloadingProgress
                                    },
                                )
                                Text(
                                    text = "${
                                        model.downloadingProgress.times(100).toInt()
                                    }%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            false -> CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    )
    HorizontalDivider(modifier = Modifier.alpha(0.5f))
}