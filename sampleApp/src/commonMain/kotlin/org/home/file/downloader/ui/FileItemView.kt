package org.home.file.downloader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shared.file.downloader.file_downloader.DownloadResult
import org.home.file.downloader.FileItem

@Composable
internal fun FileItem(
    modifier: Modifier = Modifier,
    file: FileItem,
    openFile: (String) -> Unit
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (file.path.isNotEmpty())
                    openFile(file.path)
            },
        shape = RoundedCornerShape(8.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "File: ${file.fileName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            when (val state = file.state) {
                is DownloadResult.ProgressUpdate -> {
                    LinearProgressIndicator(
                        progress = state.progress.value,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "Downloading: ${(state.progress.value * 100).toInt()}%",
                        fontSize = 14.sp
                    )
                }

                is DownloadResult.DownloadCompleted -> {
                    Text(
                        text = "Size: ${file.humanReadableSize} bytes",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Downloaded on: ${file.humanReadableDateTime}",
                        fontSize = 14.sp
                    )
                }

                is DownloadResult.DownloadFailed -> {
                    Text(
                        text = "Download failed",
                        color = MaterialTheme.colors.error,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}