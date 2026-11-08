package sergirex.portadasrevistas.notifications

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import sergirex.portadasrevistas.R

/** The new-issue channel: where the user controls sound and vibration (Android 8+). */
object NotificationChannels {
    const val NEW_ISSUES = "new_issues"

    // The channel of the old daily reminder, no longer used.
    private const val LEGACY_DAILY = "26081995"

    /** Safe to call repeatedly; an existing channel keeps the user's own sound and vibration choices. */
    fun ensureNewIssues(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = NotificationManagerCompat.from(context)
        val channel = NotificationChannelCompat.Builder(NEW_ISSUES, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.notification_channel_name))
            .build()
        manager.createNotificationChannel(channel)
        manager.deleteNotificationChannel(LEGACY_DAILY)
    }
}
