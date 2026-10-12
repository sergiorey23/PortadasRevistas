package sergirex.portadasrevistas.data.local

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pageScalesDataStore by preferencesDataStore(name = "page_scales")

@Singleton
class DataStoreScaleStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ScaleStore {

    override suspend fun get(magazineId: String): Int? =
        context.pageScalesDataStore.safeData.first()[intPreferencesKey(magazineId)]

    override suspend fun put(magazineId: String, scale: Int) {
        context.pageScalesDataStore.editSafely { it[intPreferencesKey(magazineId)] = scale }
    }
}
