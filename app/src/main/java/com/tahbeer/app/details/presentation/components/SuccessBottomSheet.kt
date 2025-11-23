package com.tahbeer.app.details.presentation.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.tahbeer.app.R
import com.tahbeer.app.core.utils.mimeType

@Composable
fun SuccessBottomSheet(
    modifier: Modifier = Modifier,
    outputedFile: Uri?,
) {
    val context = LocalContext.current

    Column {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.success_filled),
                    contentDescription = null,
                    tint = Color.Green
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.export_success),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = {
                outputedFile?.let {
                    val isAndroidQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_STREAM,
                            if (isAndroidQOrLater) it else FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                it.toFile()
                            )
                        )
                        type = outputedFile.mimeType(context) ?: ""

                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
            }
        ) {
            Text(text = stringResource(R.string.share_file))
        }
    }
}