package com.tahbeer.app.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tahbeer.app.R

@Composable
fun LanguagePickerDialog(
    onLanguageSelected: (String) -> Unit,
    languages: List<String>,
    onDismissRequest: () -> Unit
) {
    val allLanguages = remember {
        languages.map { Locale(it).platformLocale }
            .sortedBy { it.getDisplayLanguage(Locale(it.language).platformLocale) }
    }

    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages = remember(searchQuery, allLanguages) {
        if (searchQuery.isBlank()) {
            allLanguages
        } else {
            allLanguages.filter {
                it.displayLanguage.contains(
                    searchQuery,
                    ignoreCase = true
                ) || it.getDisplayLanguage(Locale(it.language).platformLocale)
                    .contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.search_language)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        )
                    )
                    IconButton(onClick = onDismissRequest) {
                        IconWithTooltip(
                            Icons.Default.Close,
                            text = stringResource(R.string.close)
                        )
                    }
                }
                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    items(filteredLanguages, key = { it }) { language ->
                        ListItem(
                            headlineContent = {
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
                            modifier = Modifier.clickable {
                                onLanguageSelected(language.language)
                            }
                        )
                    }
                }
            }
        }
    }
}