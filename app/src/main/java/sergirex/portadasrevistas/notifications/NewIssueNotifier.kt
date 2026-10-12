package sergirex.portadasrevistas.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import sergirex.portadasrevistas.Portadas
import sergirex.portadasrevistas.R
import sergirex.portadasrevistas.domain.model.IssueDate
import sergirex.portadasrevistas.domain.model.NewIssue
import javax.inject.Inject
import javax.inject.Singleton

/** Tells the user that one of their favorite magazines has a new issue. */
@Singleton
class NewIssueNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Notifies about [newIssue], replacing the previous notification if it is still showing. */
    @SuppressLint("MissingPermission") // canPost() checks it
    fun show(newIssue: NewIssue) {
        if (!canPost()) return
        createChannel()

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, Portadas::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_new_issue)
            .setContentTitle(context.getString(R.string.new_issue_title, newIssue.revista.name))
            .setContentText(context.getString(R.string.new_issue_text, IssueDate.toDisplay(newIssue.date)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun canPost(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        )
        // The channel of the old daily reminder is no longer used.
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
    }

    private companion object {
        const val CHANNEL_ID = "new_issues"
        const val LEGACY_CHANNEL_ID = "26081995"
        const val NOTIFICATION_ID = 1
    }
}
