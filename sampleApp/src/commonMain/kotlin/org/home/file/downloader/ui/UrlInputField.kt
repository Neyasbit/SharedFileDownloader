package org.home.file.downloader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun UrlInputField(
    modifier: Modifier = Modifier,
    onDownload: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    Row(
        modifier = modifier.fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedTextField(
            modifier = Modifier.weight(1f),
            value = url,
            onValueChange = {
                url = it
            },
            label = { Text("Enter file URL") },
            shape = RoundedCornerShape(8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
            onClick = { onDownload(url) },
            modifier = Modifier.align(Alignment.CenterVertically).background(
                color = MaterialTheme.colors.primary,
                shape = CircleShape
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                modifier = Modifier.size(24.dp).rotate(90f),
                contentDescription = "Download",
                tint = Color.White
            )
        }
    }

}