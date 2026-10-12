package sergirex.portadasrevistas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.widget.Toast
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Starts [intent] for links and chooser sheets, where the device may have no app able to handle it
 * (no browser, no mail app, no Play Store): the user gets a message instead of a crash.
 */
fun Context.startActivityOrToast(intent: Intent): Boolean = try {
    startActivity(intent)
    true
} catch (error: ActivityNotFoundException) {
    Toast.makeText(this, R.string.no_app_found, Toast.LENGTH_LONG).show()
    false
}

/**
 * Opens a magazine's website (stored without scheme, e.g. "semana.es") in a Custom Tab tinted
 * with the app bar color, falling back to any browser when there is no Custom Tabs provider.
 */
fun Context.openWebsite(web: String) {
    if (web.isBlank()) {
        Toast.makeText(this, R.string.no_website, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = Uri.parse("https://www.$web")
    val toolbarColor = TypedValue().let { value ->
        if (theme.resolveAttribute(R.attr.colorAppBar, value, true)) value.data else null
    }
    try {
        val colors = CustomTabColorSchemeParams.Builder().apply { toolbarColor?.let(::setToolbarColor) }.build()
        CustomTabsIntent.Builder().setShowTitle(true).setDefaultColorSchemeParams(colors).build().launchUrl(this, uri)
    } catch (error: ActivityNotFoundException) {
        startActivityOrToast(Intent(Intent.ACTION_VIEW, uri))
    }
}
