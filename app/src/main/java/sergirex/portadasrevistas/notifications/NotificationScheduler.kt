package sergirex.portadasrevistas.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the daily new-issue check scheduled exactly while the user wants it AND notifications can
 * actually be shown, and cancelled otherwise. Call [sync] whenever either could have changed (app
 * start, setting toggled, permission result).
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private val workManager get() = WorkManager.getInstance(context)

    suspend fun sync() {
        val wanted = settings.dailyNotificationEnabled.first() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (wanted) schedule() else workManager.cancelUniqueWork(WORK_NAME)
    }

    private fun schedule() {
        val request = PeriodicWorkRequestBuilder<NewIssuesWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextRun(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        // KEEP: calling this at every app start must not push back a run that is already scheduled.
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Runs a check right away, outside the daily schedule (used by the debug tools). Returns the work's id. */
    fun checkNow(): UUID {
        val request = OneTimeWorkRequestBuilder<NewIssuesWorker>().build()
        workManager.enqueue(request)
        return request.id
    }

    /** The state of a work enqueued with [checkNow]. */
    fun observe(workId: UUID): Flow<WorkInfo?> = workManager.getWorkInfoByIdFlow(workId)

    private fun millisUntilNextRun(): Long {
        val now = System.currentTimeMillis()
        val next = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DATE, 1)
        }.timeInMillis
        return next - now
    }

    private companion object {
        const val WORK_NAME = "new_issues_check"
        const val HOUR = 9
    }
}
