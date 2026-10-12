package sergirex.portadasrevistas.data.repository

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.data.local.editSafely
import sergirex.portadasrevistas.data.local.safeData
import sergirex.portadasrevistas.domain.repository.ReadingProgressRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.readingProgressDataStore by preferencesDataStore(name = "reading_progress")

/**
 * One entry per (magazine, issue): a new issue starts at page one by itself, which the old code
 * had to arrange by deleting the saved page whenever it noticed a newer issue.
 */
@Singleton
class ReadingProgressRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ReadingProgressRepository {

    override suspend fun getPage(magazineId: String, date: String): Int =
        context.readingProgressDataStore.safeData.first()[key(magazineId, date)] ?: 0

    override suspend fun savePage(magazineId: String, date: String, page: Int) {
        context.readingProgressDataStore.editSafely { it[key(magazineId, date)] = page }
    }

    private fun key(magazineId: String, date: String) = intPreferencesKey("$magazineId|$date")
}
