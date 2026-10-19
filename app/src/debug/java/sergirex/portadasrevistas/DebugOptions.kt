package sergirex.portadasrevistas

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.IssueSeenRepository
import sergirex.portadasrevistas.domain.repository.SettingsRepository
import sergirex.portadasrevistas.notifications.NotificationScheduler

/**
 * Developer tools, present only in debug builds (this file lives in the `debug` source set; the
 * `release` one is an inert twin), so production code carries no "if (debug)" branches. The
 * tools are shown in Settings.
 */
object DebugOptions {
    private const val KEY_SIMULATE_NEW_ISSUE = "debug_simulate_new_issue"
    private const val LONG_AGO = "19000101"

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun favorites(): FavoriteRevistasRepository
        fun seen(): IssueSeenRepository
        fun settings(): SettingsRepository
        fun scheduler(): NotificationScheduler
    }

    /** Wires the debug actions of the Settings screen. */
    fun bindPreferences(fragment: PreferenceFragmentCompat) {
        fragment.findPreference<Preference>(KEY_SIMULATE_NEW_ISSUE)?.setOnPreferenceClickListener {
            val context = fragment.requireContext()
            val dependencies = EntryPointAccessors.fromApplication(context.applicationContext, Dependencies::class.java)
            fragment.lifecycleScope.launch { simulateNewIssue(context, dependencies) }
            true
        }
    }

    /**
     * Makes every favorite look as if it had a new issue and runs the check now. It says out loud
     * why nothing would show up (the real worker returns silently in those cases), since "no
     * notification" has several causes that look the same from outside.
     */
    private suspend fun simulateNewIssue(context: Context, dependencies: Dependencies) {
        val favorites = dependencies.favorites().favorites.first()
        when {
            favorites.isEmpty() ->
                toast(context, "No tienes revistas favoritas: abre una y márcala con el corazón.")
            !NotificationManagerCompat.from(context).areNotificationsEnabled() -> {
                toast(context, "Las notificaciones están desactivadas para la app. Actívalas en los ajustes del sistema.")
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            !dependencies.settings().dailyNotificationEnabled.first() ->
                toast(context, "El interruptor \"Avisos de nuevos números\" está apagado.")
            else -> {
                // Pretend every favorite was last seen long ago, so the check finds a "new" issue for the first one.
                favorites.forEach { dependencies.seen().markSeen(it.id, LONG_AGO) }
                toast(context, "Comprobando ${favorites.size} favorita(s)…")
                val result = dependencies.scheduler().observe(dependencies.scheduler().checkNow())
                    .first { it != null && (it.state.isFinished || it.runAttemptCount > 0 && it.state == WorkInfo.State.ENQUEUED) }
                toast(
                    context,
                    when (result?.state) {
                        WorkInfo.State.SUCCEEDED -> "Comprobación terminada: debería haber una notificación."
                        WorkInfo.State.ENQUEUED -> "No se pudo leer el calendario (¿sin conexión?). Se reintentará."
                        else -> "La comprobación falló (${result?.state})."
                    }
                )
            }
        }
    }

    private fun toast(context: Context, message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
