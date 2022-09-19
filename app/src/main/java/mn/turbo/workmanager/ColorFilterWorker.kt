package mn.turbo.workmanager

import android.content.Context
import android.graphics.*
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
class ColorFilterWorker(
    private val context: Context,
    private val workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val imageFile = workerParameters.inputData.getString(Constants.IMAGE_URI)
            ?.toUri()
            ?.toFile()

        delay(5000)
        return imageFile?.let { file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val resultBitmap = bitmap.copy(bitmap.config, true)

            val paint = Paint()
            paint.colorFilter = LightingColorFilter(0x08FF04, 1)
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(resultBitmap, 0f, 0f, paint)

            withContext(Dispatchers.IO) {
                val resultImageFile = File(context.cacheDir, "result.jpg")
                val outputStream = FileOutputStream(resultImageFile)
                val success = resultBitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    90, outputStream
                )
                if (success) {
                    Result.success(
                        workDataOf(
                            Constants.IMAGE_URI to resultImageFile.toUri().toString()
                        )
                    )
                } else {
                    Result.failure()
                }
            }
        } ?: Result.failure()
    }

}