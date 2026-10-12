package sergirex.portadasrevistas

import android.content.SharedPreferences
import androidx.preference.PreferenceFragmentCompat

/** Release twin of the debug source set's DebugOptions: there are no developer switches, nothing is overridden. */
@Suppress("UNUSED_PARAMETER")
object DebugOptions {
    fun premiumOverride(prefs: SharedPreferences): Boolean = false

    fun bindPreferences(fragment: PreferenceFragmentCompat) = Unit
}
