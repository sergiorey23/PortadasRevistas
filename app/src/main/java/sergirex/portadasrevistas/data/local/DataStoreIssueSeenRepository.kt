package sergirex.portadasrevistas.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import sergirex.portadasrevistas.domain.repository.IssueSeenRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.seenIssuesDataStore by preferencesDataStore(name = "seen_issues")

@Singleton
class DataStoreIssueSeenRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : IssueSeenRepository {

    override suspend fun lastSeen(magazineId: String): String? =
        context.seenIssuesDataStore.safeData.first()[stringPreferencesKey(magazineId)]

    override suspend fun markSeen(magazineId: String, date: String) {
        context.seenIssuesDataStore.editSafely { it[stringPreferencesKey(magazineId)] = date }
    }
}
