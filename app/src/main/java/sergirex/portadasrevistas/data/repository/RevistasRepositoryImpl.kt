package sergirex.portadasrevistas.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import sergirex.portadasrevistas.data.local.RevistaDto
import sergirex.portadasrevistas.data.local.toDomain
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.repository.RevistasRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the magazine catalog from assets/revistas.json: same "ships inside the APK" deployment
 * model the old Java arrays had, but with a schema and a single class that knows how to read it.
 * Parsed once and cached for the process lifetime (~140 small entries).
 */
@Singleton
class RevistasRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json
) : RevistasRepository {

    private val catalog: List<Revista> by lazy { loadCatalog() }

    override suspend fun getByCategory(category: RevistaCategory): Result<List<Revista>> =
        withContext(Dispatchers.IO) { runCatching { catalog.filter { it.category == category } } }

    override suspend fun getById(id: String): Revista? =
        withContext(Dispatchers.IO) { runCatching { catalog.firstOrNull { it.id == id } }.getOrNull() }

    private fun loadCatalog(): List<Revista> =
        context.assets.open(CATALOG_ASSET).bufferedReader().use { it.readText() }
            .let { json.decodeFromString<List<RevistaDto>>(it) }
            .map { it.toDomain() }

    private companion object {
        const val CATALOG_ASSET = "revistas.json"
    }
}
