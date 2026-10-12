package sergirex.portadasrevistas.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.domain.usecase.CheckNewFavoriteIssuesUseCase

/**
 * The daily check: looks for new issues of the favorites and notifies about them. Runs under
 * WorkManager, which gives it what a plain alarm + BroadcastReceiver can't: a network constraint,
 * a time budget long enough for several requests, retries with backoff, and automatic
 * rescheduling after a reboot or an app update.
 */
class NewIssuesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun checkNewIssues(): CheckNewFavoriteIssuesUseCase
        fun notifier(): NewIssueNotifier
        fun settings(): SettingsRepository
    }

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java)
        // The setting or the system permission may have changed since this run was scheduled.
        val allowed = dependencies.settings().dailyNotificationEnabled.first() &&
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        if (!allowed) return Result.success()

        return dependencies.checkNewIssues()().fold(
            onSuccess = { newIssue ->
                newIssue?.let(dependencies.notifier()::show)
                Result.success()
            },
            // Most likely no network: try again soon, but give up for today after a few attempts.
            onFailure = { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success() }
        )
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
