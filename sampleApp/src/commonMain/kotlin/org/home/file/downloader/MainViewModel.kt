package org.home.file.downloader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shared.file.downloader.file_downloader.DownloadResult
import com.shared.file.downloader.file_downloader.FileDownloader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class FileItem(
    val id: String = Uuid.random().toString(),
    val fileSize: Long = 0,
    val state: DownloadResult,
    val fileName: String = state.url.substringAfterLast("/"),
    val downloadTime: LocalDateTime
) {
    val path = if (state is DownloadResult.DownloadCompleted) state.filePath else ""

    val humanReadableSize: String = when {
        fileSize >= 1_000_000 -> "${fileSize / 1_000_000.0} MB"
        fileSize >= 1_000 -> "${fileSize / 1_000.0} KB"
        else -> "$fileSize B"
    }
    val humanReadableDateTime: String = LocalDateTime.Format {
        year()
        char('/')
        monthNumber()
        char('/')
        dayOfMonth()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()

    }.format(downloadTime)
}

data class UiState(
    val files: List<FileItem> = emptyList(),
)

class MainViewModel(
    private val fileDownloader: FileDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val state = _uiState.asStateFlow()

    fun startDownloadSmallImage() {
        val imageUrl = "https://i.imgur.com/xQ9UO95.jpeg"
        startDownloadFile(imageUrl, imageUrl.substringAfterLast("/"))
    }

    fun startDownloadLargeMp4() {
        val fileUrl =
            "https://thetestdata.com/assets/video/mp4/1080/10MB_1080P_THETESTDATA.COM_mp4_new.mp4"
        startDownloadFile(fileUrl, fileUrl.substringAfterLast("/"))
    }

    fun startDownloadUrl(url: String) {
        val fileName = url.substringAfterLast("/")
        startDownloadFile(url, fileName)
    }

    private fun startDownloadFile(url: String, fileName: String) {

        fileDownloader.downloadFile(url, fileName)
            .onEach { state ->
                val existingFile = _uiState.value.files.find { it.fileName == fileName }
                val newFiles = if (existingFile == null)
                    _uiState.value.files.plus(
                        FileItem(
                            fileName = fileName,
                            state = state,
                            downloadTime = Clock.System.now()
                                .toLocalDateTime(TimeZone.currentSystemDefault())
                        )
                    )
                else
                    _uiState.value.files.map { file ->
                        if (file.fileName == fileName)
                            file.copy(
                                state = state,
                                fileSize = if (state is DownloadResult.DownloadCompleted)
                                    SystemFileSystem.metadataOrNull(Path(state.filePath))?.size ?: 0
                                else 0,
                                downloadTime = Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                            )
                        else file
                    }

                _uiState.update { uiState ->
                    uiState.copy(files = newFiles)
                }
            }.launchIn(viewModelScope)
    }
}