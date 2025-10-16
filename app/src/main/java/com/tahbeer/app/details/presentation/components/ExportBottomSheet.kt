package com.tahbeer.app.details.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.details.domain.model.ExportFormat
import com.tahbeer.app.details.presentation.DetailScreenAction

@Composable
fun ExportBottomSheet(
    onAction: (DetailScreenAction) -> Unit,
    transcriptionItem: TranscriptionItem
) {
    var selectedExportFormat by remember { mutableStateOf(ExportFormat.TXT) }
    val options = ExportFormat.entries.toTypedArray()
    Column {
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            options.forEach {
                EnhancedChip(
                    onClick = {
                        selectedExportFormat = it
                    },
                    selected = selectedExportFormat == it,
                    label = {
                        Text(text = it.name.lowercase())
                    },
                    selectedColor = MaterialTheme.colorScheme.tertiary,
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                )
            }
        }
        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            onClick = {
                onAction(
                    DetailScreenAction.OnExport(
                        exportFormat = selectedExportFormat,
                        transcriptionItem = transcriptionItem
                    )
                )
            }) { Text(stringResource(R.string.export_btn)) }
    }
}