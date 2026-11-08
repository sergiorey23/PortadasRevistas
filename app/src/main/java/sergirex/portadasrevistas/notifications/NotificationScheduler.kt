package sergirex.portadasrevistas.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.model.ReminderSchedule
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the new-issue check scheduled for its next run (time of day + daily/weekly/monthly frequency from the
 * settings) exactly while the user wants it AND notifications can actually be shown, and cancelled otherwise.
 *
 * Each run is a one-time WorkManager job (network required, retried with backoff) set for the next allowed
 * moment; when it finishes it queues the following one ([scheduleAfterRun]). WorkManager keeps the job across
 * reboots and app updates by itself. Call [sync] whenever the setting or the permission could have changed (app
 * start, setting toggled, permission result); a changed schedule passes `replace` so the old run is dropped.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    private val workManager get() = WorkManager.getInstance(context)

    suspend fun sync(replace: Boolean = false) {
        // Earlier versions ran a periodic job under another name: it would keep firing next to the new one.
        workManager.cancelUniqueWork(LEGACY_PERIODIC_WORK)
        val trigger = nextTrigger(afterFiring = false)
        if (trigger == null) {
            workManager.cancelUniqueWork(WORK_NAME)
        } else {
            // KEEP at app start, so opening the app never pushes back a run that is already set.
            enqueue(trigger, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP)
        }
    }

    /** Called by a scheduled run once it is over: queues the next one behind it. */
    suspend fun scheduleAfterRun() {
        val trigger = nextTrigger(afterFiring = true) ?: return
        enqueue(trigger, ExistingWorkPolicy.APPEND_OR_REPLACE)
    }

    /** Runs a check right away, outside the schedule (used by the debug tools). Returns the work's id. */
    fun checkNow(): UUID {
        val request = OneTimeWorkRequestBuilder<NewIssuesWorker>().build()
        workManager.enqueue(request)
        return request.id
    }

    /** The state of a work enqueued with [checkNow]. */
    fun observe(workId: UUID): Flow<WorkInfo?> = workManager.getWorkInfoByIdFlow(workId)

    private suspend fun nextTrigger(afterFiring: Boolean): Long? {
        val wanted = settings.dailyNotificationEnabled.first() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!wanted) return null
        return ReminderSchedule.nextTrigger(System.currentTimeMillis(), settings.reminderConfig.first(), afterFiring)
    }

    private fun enqueue(trigger: Long, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<NewIssuesWorker>()
            .setInitialDelay((trigger - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .setInputData(workDataOf(NewIssuesWorker.KEY_SCHEDULED to true))
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "new_issues_schedule"
        const val LEGACY_PERIODIC_WORK = "new_issues_check"
    }
}
