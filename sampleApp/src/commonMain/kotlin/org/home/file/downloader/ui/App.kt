import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shared.file.downloader.file_downloader.createFileDownloader
import org.home.file.downloader.MainViewModel
import org.home.file.downloader.ui.DownloadButton
import org.home.file.downloader.ui.FileList
import org.home.file.downloader.ui.UrlInputField

@Composable
fun App(
    viewModel: MainViewModel = MainViewModel(createFileDownloader())
) {
    val state by viewModel.state.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {

            UrlInputField { viewModel.startDownloadUrl(it) }

            Spacer(modifier = Modifier.height(16.dp))

            DownloadButton(
                text = "Download Small File",
                onClick = { viewModel.startDownloadSmallImage() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            DownloadButton(
                text = "Download Large File",
                onClick = { viewModel.startDownloadLargeMp4() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            FileList(files = state.files)
        }
    }
}