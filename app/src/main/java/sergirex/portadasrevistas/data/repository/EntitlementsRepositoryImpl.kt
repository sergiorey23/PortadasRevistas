package sergirex.portadasrevistas.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import sergirex.portadasrevistas.DebugOptions
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.repository.EntitlementsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The purchase flags, kept in the app's default SharedPreferences under the same keys older
 * versions used (the Play product ids), so people who already bought keep their entitlement
 * without a re-sync. Google Play stays the real source of truth: BillingManager rewrites these
 * flags whenever it hears from Play.
 *
 * Debug builds can switch the premium subscription on from Settings ([DebugOptions]), so the
 * reader and the ad-free experience can be developed without a Play account or a purchase. The
 * release build has no such switch: its [DebugOptions] never overrides anything.
 */
@Singleton
class EntitlementsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : EntitlementsRepository {

    private val prefs: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

    override val entitlements: Flow<Entitlements> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(read()) }
        trySend(read())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged().conflate()

    override suspend fun current(): Entitlements = withContext(Dispatchers.IO) { read() }

    override suspend fun setAdsRemoved(removed: Boolean) = write(KEY_ADS_REMOVED, removed)

    override suspend fun setPremium(premium: Boolean) = write(KEY_PREMIUM, premium)

    private suspend fun write(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        prefs.edit { putBoolean(key, value) }
    }

    private fun read() = Entitlements(
        adsRemoved = prefs.getBoolean(KEY_ADS_REMOVED, false),
        premium = DebugOptions.premiumOverride(prefs) || prefs.getBoolean(KEY_PREMIUM, false)
    )

    private companion object {
        const val KEY_ADS_REMOVED = "remove_ads_id"
        const val KEY_PREMIUM = "premium_sub_id"
    }
}
