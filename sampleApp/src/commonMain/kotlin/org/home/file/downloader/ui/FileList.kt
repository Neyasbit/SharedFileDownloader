package org.home.file.downloader.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.home.file.downloader.FileItem

@Composable
internal expect fun OpenFile(path: String)

@Composable
internal fun FileList(
    files: List<FileItem>,
    modifier: Modifier = Modifier
) {
    var filePath by rememberSaveable { mutableStateOf("") }

    LazyColumn(modifier = modifier) {
        items(files, key = { it.id }) { file ->
            FileItem(file = file) { path ->
                filePath = path
            }
        }
    }
    if (filePath.isNotEmpty())
        OpenFile(filePath)
}

