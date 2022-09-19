package mn.turbo.workmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.*
import coil.compose.rememberImagePainter
import mn.turbo.workmanager.ui.theme.WorkManagerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()

        val workManager = WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    val workInfo = workManager
                        .getWorkInfosForUniqueWorkLiveData("download")
                        .observeAsState()
                        .value

                    val downloadInfo = remember(key1 = workInfo) {
                        workInfo?.find { it.id == downloadRequest.id }
                    }

                    val filterInfo = remember(key1 = workInfo) {
                        workInfo?.find { it.id == colorFilterRequest.id }
                    }

                    val imageUri by derivedStateOf {
                        val downloadUri = downloadInfo?.outputData?.getString(Constants.IMAGE_URI)
                            ?.toUri()
                        val filterUri = filterInfo?.outputData?.getString(Constants.FILTER_URI)
                            ?.toUri()
                        filterUri ?: downloadUri
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        imageUri?.let { uri ->
                            Image(
                                painter = rememberImagePainter(data = uri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Button(
                            onClick = {
                                workManager
                                    .beginUniqueWork(
                                        "download",
                                        ExistingWorkPolicy.KEEP,
                                        downloadRequest
                                    )
                                    .then(colorFilterRequest)
                                    .enqueue()
                            },
                            enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                        ) {
                            Text(text = "start download")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        when (downloadInfo?.state) {
                            WorkInfo.State.RUNNING -> Text(text = "downloading")
                            WorkInfo.State.SUCCEEDED -> Text(text = "Download DONE!")
                            WorkInfo.State.FAILED -> Text(text = "Download FAILED!")
                            WorkInfo.State.CANCELLED -> Text(text = "It was cancelled!")
                            WorkInfo.State.ENQUEUED -> Text(text = "ENQUEUED")
                            WorkInfo.State.BLOCKED -> Text(text = "BLOCKED")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        when (filterInfo?.state) {
                            WorkInfo.State.RUNNING -> Text(text = "filter running ")
                            WorkInfo.State.SUCCEEDED -> Text(text = "filter DONE!")
                            WorkInfo.State.FAILED -> Text(text = "filter FAILED!")
                            WorkInfo.State.CANCELLED -> Text(text = "filter cancelled!")
                            WorkInfo.State.ENQUEUED -> Text(text = "filter ENQUEUED")
                            WorkInfo.State.BLOCKED -> Text(text = "filter BLOCKED")
                        }
                    }
                }
            }
        }
    }
}
