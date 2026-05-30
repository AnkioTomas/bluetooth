package net.ankio.bluetooth.webdav

import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.ankio.bluetooth.model.WebdavMode

class WebdavPullWorker(
    context: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (WebdavMode.current() != WebdavMode.SyncFromWebdav) return Result.success()
        if (!WebdavServiceManager.isConfigured(applicationContext)) return Result.success()
        return when (WebdavServiceManager.pullOnce(applicationContext)) {
            WebdavServiceManager.PullResult.Failed -> {
                Log.w(TAG, "Periodic pull failed")
                Result.retry()
            }
            else -> Result.success()
        }
    }

    companion object {
        private const val TAG = "WebdavPullWorker"
    }
}
