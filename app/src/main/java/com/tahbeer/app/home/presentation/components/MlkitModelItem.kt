package com.tahbeer.app.home.presentation.components

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
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.presentation.components.IconWithTooltip
import com.tahbeer.app.home.domain.model.MlkitModel
import com.tahbeer.app.home.presentation.settings.SettingsAction

@Composable
fun MlkitModelItem(
    model: MlkitModel,
    onAction: (SettingsAction) -> Unit
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        headlineContent = {
            val language = Locale(model.lang).platformLocale

            val nativeDisplayName = language.getDisplayLanguage(
                language
            )
            val localizedDisplayName = language.getDisplayLanguage(
                Locale.current.platformLocale
            )

            val showLocalized = nativeDisplayName != localizedDisplayName

            Row(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = nativeDisplayName,
                )

                if (showLocalized) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "($localizedDisplayName)",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (model.lang == "en" && model.isDownloaded) Color.Transparent else MaterialTheme.colorScheme.onSecondary,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                AnimatedContent((model.isDownloading)) { isDownloading ->
                    if (!isDownloading) {
                        when (model.isDownloaded) {
                            true -> {
                                if (model.lang != "en")
                                    IconButton(
                                        onClick = {
                                            onAction(
                                                SettingsAction.OnMlkitModelDelete(
                                                    model.lang
                                                )
                                            )
                                        },
                                    ) {
                                        IconWithTooltip(
                                            icon = ImageVector.vectorResource(R.drawable.delete),
                                            text = stringResource(R.string.settings_model_delete),
                                        )
                                    }
                            }

                            false -> IconButton(
                                onClick = {
                                    onAction(
                                        SettingsAction.OnMlkitModelDownload(
                                            model.lang
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
                        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                    }
                }
            }
        }
    )
}