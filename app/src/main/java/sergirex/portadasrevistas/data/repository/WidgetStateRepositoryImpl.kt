package sergirex.portadasrevistas.data.repository

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.data.local.editSafely
import sergirex.portadasrevistas.data.local.safeData
import sergirex.portadasrevistas.domain.repository.WidgetStateRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore by preferencesDataStore(name = "widget")

@Singleton
class WidgetStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetStateRepository {

    override suspend fun index(): Int = context.widgetDataStore.safeData.first()[INDEX] ?: 0

    override suspend fun setIndex(index: Int) {
        context.widgetDataStore.editSafely { it[INDEX] = index }
    }

    private companion object {
        val INDEX = intPreferencesKey("index")
    }
}
