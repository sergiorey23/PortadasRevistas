package sergirex.portadasrevistas.data.repository

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sergirex.portadasrevistas.data.local.FavoritesCodec
import sergirex.portadasrevistas.data.local.editSafely
import sergirex.portadasrevistas.data.local.safeData
import sergirex.portadasrevistas.domain.model.RevistaRef
import sergirex.portadasrevistas.domain.repository.FavoriteRevistasRepository
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import javax.inject.Inject
import javax.inject.Singleton

private val FAVORITES_KEY = stringPreferencesKey("favorites")

/**
 * One-time import of the old "revistas" SharedPreferences file (key = title, value = legacy
 * "id:title:web:pages:scale" string) so existing users keep their favorites.
 */
private class LegacyFavoritesMigration(private val context: Context) : DataMigration<Preferences> {
    private val legacy get() = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

    override suspend fun shouldMigrate(currentData: Preferences) =
        FAVORITES_KEY !in currentData && legacy.all.isNotEmpty()

    override suspend fun migrate(currentData: Preferences): Preferences {
        val favorites = legacy.all.values.filterIsInstance<String>().map { RevistaRef.fromLegacyEncoded(it) }
        return currentData.toMutablePreferences().apply { this[FAVORITES_KEY] = FavoritesCodec.encode(favorites) }
    }

    override suspend fun cleanUp() {
        legacy.edit().clear().apply()
    }

    private companion object {
        const val LEGACY_PREFS = "revistas"
    }
}

private val Context.favoritesDataStore by preferencesDataStore(
    name = "favorites",
    produceMigrations = { context -> listOf(LegacyFavoritesMigration(context)) }
)

@Singleton
class FavoriteRevistasRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: RevistasRepository
) : FavoriteRevistasRepository {

    override val favorites: Flow<List<RevistaRef>> = context.favoritesDataStore.safeData
        .map { prefs ->
            // The catalog is the source of truth for names, web addresses, page counts and categories, so
            // renamed magazines and favorites saved with older data show the current values.
            FavoritesCodec.decode(prefs[FAVORITES_KEY]).map { ref ->
                catalog.getById(ref.id)?.let { ref.copy(name = it.name, web = it.web, pages = it.pages, scale = it.scale, category = it.category) } ?: ref
            }
        }

    override suspend fun toggle(revista: RevistaRef) {
        context.favoritesDataStore.editSafely { prefs ->
            val current = FavoritesCodec.decode(prefs[FAVORITES_KEY])
            val updated = if (current.any { it.id == revista.id }) current.filterNot { it.id == revista.id } else current + revista
            prefs[FAVORITES_KEY] = FavoritesCodec.encode(updated)
        }
    }
}
