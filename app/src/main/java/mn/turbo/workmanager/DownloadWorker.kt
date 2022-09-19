package mn.turbo.workmanager

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

@Suppress("BlockingMethodInNonBlockingContext")
class DownloadWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        showNotification()

        delay(500)

        val response = DownloadApi.instance.downloadImage()
        response.body()?.let { responseBody ->
            return withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "image.jpg")

                val outputStream = FileOutputStream(file)
                outputStream.use { stream ->
                    try {
                        stream.write(responseBody.bytes())
                    } catch (e: IOException) {
                        return@withContext Result.failure(
                            workDataOf(
                                Constants.ERROR_MSG to e.message
                            )
                        )
                    }
                }
                Result.success(
                    workDataOf(
                        Constants.IMAGE_URI to file.toUri().toString()
                    )
                )
            }
        }

        if (!response.isSuccessful) {
            if (response.code().toString().startsWith("5")) {
                return Result.retry()
            }
            return Result.failure(
                workDataOf(
                    Constants.ERROR_MSG to "mf error"
                )
            )
        }

        return Result.failure(
            workDataOf(
                Constants.ERROR_MSG to "unknown error"
            )
        )
    }

    private suspend fun showNotification() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(),
                NotificationCompat.Builder(context, "download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentText("downloading ...")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )
    }

}