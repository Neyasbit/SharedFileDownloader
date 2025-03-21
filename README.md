# SharedFileDownloader

## 📥 Быстрый загрузчик файлов для Kotlin Multiplatform

**FileDownloader** — это библиотека для удобной и эффективной загрузки файлов с поддержкой потокового обновления прогресса, многопоточной загрузки и проверки целостности. 

## 🚀 Основные возможности
- 📂 Поддержка загрузки больших файлов
- 📡 Поддержка возобновляемых загрузок
- ⚡ Использование `Kotlin Coroutines` и `Flow`
- 🔄 Гибкое обновление прогресса загрузки
- 🔍 Валидация загруженного файла (опционально)
- 📱 Поддержка **Kotlin Multiplatform** (Android, iOS, JVM, MasOS)

## Примеры работы приложений

<table>
  <tr>
    <td><img src="https://github.com/Neyasbit/SharedFileDownloader/blob/main/records/Simulator-Screen-Recording-iPhone-15-2025-03-21-at-14.30.34.gif" width="300" alt="IOS"></td>
    <td><img src="https://github.com/Neyasbit/SharedFileDownloader/blob/main/records/Screen_recording_20250320_203521.gif" width="300" alt="Android"></td>
  </tr>
  <tr>
    <td colspan="2"><img src="https://github.com/Neyasbit/SharedFileDownloader/blob/main/records/Запись-экрана-2025-03-21-в-15.03.47.gif" width="600" alt="Desktop"></td>
  </tr>
</table>

## 🚀 Использование

```kotlin
val fileDownloader = createFileDownloader()

val url = "https://example.com/file.zip"
val fileName = "file.zip"

fileDownloader.downloadFile(url, fileName).collect { result ->
    when (result) {
        is DownloadResult.Single.ProgressUpdate -> {
            println("Прогресс: ${result.progress.downloaded} / ${result.progress.total}")
        }
        is DownloadResult.Single.DownloadCompleted -> {
            println("Файл загружен: ${result.filePath}")
        }
        is DownloadResult.Single.DownloadFailed -> {
            println("Ошибка загрузки: ${result.exception}")
        }
    }
}
```

## 🔍 Детали работы

### 📥 Загрузка файлов
Логика загрузки строится на использовании стратегий загрузки (`NormalStrategy`) для файлов меньше чем 5 мегабайт, (`ChunkedStrategy`) для больших файлов и в случае, возможности сервера, позволяя адаптироваться под различные сценарии работы.

### ⏳ Обновление прогресса
Прогресс обновляется через `Flow`, что позволяет эффективно отслеживать состояние загрузки.

### 🛠 Валидация файлов
Можно подключить валидацию загруженных файлов, используя `FileValidator`.

## ✅ Тестирование

Тестирование проводится с помощью `kotlinx.coroutines.test` 

```kotlin
@Test
fun `test file download`() = runTest {
    val mockEngine = MockEngine {
        respond(
            content = ByteArray(1024) { it.toByte() },
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentLength, "1024")
        )
    }

    val client = HttpClient(mockEngine)
    val dependenciesHolder = DependenciesHolder(client, fakeFileSystem, Logger.SIMPLE)
    val fileDownloader = FileDownloader(dependenciesHolder, StrategyFabric(dependenciesHolder))

    val result = fileDownloader.downloadFile("https://example.com/file", "testfile").toList()
    assert(result.any { it is DownloadResult.Single.DownloadCompleted })
}
```

