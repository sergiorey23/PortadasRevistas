package sergirex.portadasrevistas

import android.content.SharedPreferences

/** Release twin of the debug source set's DebugOptions: there are no developer switches, nothing is overridden. */
@Suppress("UNUSED_PARAMETER")
object DebugOptions {
    fun premiumOverride(prefs: SharedPreferences): Boolean = false
}
