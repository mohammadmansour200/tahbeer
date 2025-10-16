package com.tahbeer.app.details.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arthenica.ffmpegkit.FFmpegKit
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.tahbeer.app.R
import com.tahbeer.app.core.domain.model.TranscriptionItem
import com.tahbeer.app.details.presentation.DetailScreenAction
import com.tahbeer.app.details.presentation.DetailScreenState
import kotlinx.coroutines.launch

enum class VerticalPosition {
    BOTTOM, CENTER, TOP
}

data class SubtitleStyles(
    val fontSize: Float = 16f,
    val fontWeight: FontWeight = FontWeight.Normal,
    val textColor: Color = Color.White,
    val outlineColor: Color = Color.Black,
    val outlineWidth: Float = 2f,
    val verticalPosition: VerticalPosition = VerticalPosition.BOTTOM
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun BurnSubtitleSheet(
    snackbarHostState: SnackbarHostState,
    state: DetailScreenState,
    onAction: (DetailScreenAction) -> Unit,
    transcriptionItem: TranscriptionItem
) {
    var styles by remember { mutableStateOf(SubtitleStyles()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Subtitle Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(bottom = 8.dp)
                .background(Color.Green, RoundedCornerShape(8.dp))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = when (styles.verticalPosition) {
                    VerticalPosition.BOTTOM -> Alignment.BottomCenter
                    VerticalPosition.CENTER -> Alignment.Center
                    VerticalPosition.TOP -> Alignment.TopCenter
                }
            ) {
                val density = LocalDensity.current
                Text(
                    text = transcriptionItem.result?.get(0)?.text ?: "Preview Subtitle",
                    fontSize = with(density) { (styles.fontSize / density.density / fontScale).sp },
                    fontWeight = styles.fontWeight,
                    color = styles.textColor,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Text Properties Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            stringResource(R.string.section_text_properties),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        SliderWithLabel(
            label = stringResource(R.string.label_font_size),
            value = styles.fontSize,
            onValueChange = { newValue -> styles = styles.copy(fontSize = newValue) },
            valueRange = 12f..96f,
            steps = 84,
        )

        FontWeightSelector(
            currentFontWeight = styles.fontWeight,
            onFontWeightChange = { newWeight -> styles = styles.copy(fontWeight = newWeight) }
        )

        ColorPicker(
            label = stringResource(R.string.label_text_color),
            currentColor = styles.textColor,
            onColorSelected = { newColor -> styles = styles.copy(textColor = newColor) },
            enabled = true
        )

        Spacer(Modifier.height(16.dp))

        // Positioning Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            stringResource(R.string.section_positioning),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        VerticalPositionSelector(
            currentPosition = styles.verticalPosition,
            onPositionChange = { newPos -> styles = styles.copy(verticalPosition = newPos) }
        )

        Spacer(Modifier.height(16.dp))

        // Border/Outline Properties Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(
            text = stringResource(R.string.border_properties),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )

        val isBorderDisabled = styles.outlineWidth == 0f
        ColorPicker(
            label = stringResource(R.string.border_color),
            currentColor = styles.outlineColor,
            onColorSelected = { newColor ->
                if (!isBorderDisabled) {
                    styles = styles.copy(outlineColor = newColor)
                }
            },
            enabled = !isBorderDisabled
        )

        SliderWithLabel(
            label = stringResource(R.string.border_width),
            value = styles.outlineWidth,
            onValueChange = { newValue -> styles = styles.copy(outlineWidth = newValue) },
            valueRange = 0f..8f,
            steps = 7,
        )

        Spacer(Modifier.height(16.dp))

        // Apply Button
        AnimatedContent(state.isOperating) { isBurning ->
            if (isBurning) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (state.progress != null) {
                        true -> LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { state.progress })

                        false -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onClick = {
                            scope.launch { FFmpegKit.cancel() }
                        })
                    { Text(stringResource(R.string.cancel_btn)) }
                }
            } else {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    onClick = {
                        onAction(
                            DetailScreenAction.OnBurnSubtitle(
                                transcriptionItem = transcriptionItem, subtitleStyles = styles
                            )
                        )
                    }) { Text(stringResource(R.string.burn_subtitle_btn)) }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    label: String,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(8.dp))
        val colorForPicker = if (!enabled) {
            Color.LightGray.copy(alpha = 0.4f)
        } else {
            currentColor
        }
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(ButtonDefaults.MinHeight)
                .height(36.dp)
                .background(colorForPicker, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                .clickable {
                    expanded = true
                }
        ) {
            if (!enabled) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val strokeWidth = 2.dp.toPx()
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            DropdownMenu(
                modifier = Modifier.padding(6.dp),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                ClassicColorPicker(
                    modifier = Modifier
                        .size(200.dp),
                    color = HsvColor.from(currentColor), showAlphaBar = false,
                    onColorChanged = {
                        onColorSelected(it.toColor())
                    })
            }
        }
    }
}

@Composable
private fun SliderWithLabel(
    modifier: Modifier = Modifier,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FontWeightSelector(
    currentFontWeight: FontWeight,
    onFontWeightChange: (FontWeight) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.label_font_bold),
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            Switch(currentFontWeight == FontWeight.Bold, onCheckedChange = {
                onFontWeightChange(
                    if (it) FontWeight.Bold else FontWeight.Normal
                )
            })
        }
    }
}

@Composable
private fun VerticalPositionSelector(
    currentPosition: VerticalPosition,
    onPositionChange: (VerticalPosition) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val positions = remember { VerticalPosition.entries }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.label_alignment),
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Box(
            modifier = Modifier.weight(1f)
        ) {
            val localizedPositionOption = @Composable { position: VerticalPosition ->
                when (position) {
                    VerticalPosition.BOTTOM -> stringResource(R.string.pos_bottom)
                    VerticalPosition.CENTER -> stringResource(R.string.pos_center)
                    VerticalPosition.TOP -> stringResource(R.string.pos_top)
                }
            }
            OutlinedButton(
                onClick = { expanded = true },
            ) {
                Text(localizedPositionOption(currentPosition))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 150.dp)
            ) {
                positions.forEach { position ->
                    DropdownMenuItem(
                        text = { Text(localizedPositionOption(position)) },
                        onClick = {
                            onPositionChange(position)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


