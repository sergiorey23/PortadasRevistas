package sergirex.portadasrevistas

import android.content.SharedPreferences

/**
 * Developer switches, present only in debug builds (this file lives in the `debug` source set; the
 * `release` one is an inert twin), so production code carries no "if (debug)" branches and the
 * release APK can't be flipped into premium. The switches are shown in Settings.
 */
object DebugOptions {
    const val KEY_PREMIUM = "debug_premium"

    /** Whether the premium subscription is simulated. On by default, so a fresh debug install can read magazines. */
    fun premiumOverride(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_PREMIUM, true)
}
