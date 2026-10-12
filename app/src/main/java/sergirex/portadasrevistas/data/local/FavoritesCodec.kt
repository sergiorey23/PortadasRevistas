package sergirex.portadasrevistas.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaRef

@Serializable
private data class FavoriteDto(
    val id: String,
    val name: String,
    val web: String = "",
    val pages: Int = Revista.DEFAULT_PAGES,
    val scale: String = Revista.DEFAULT_SCALE
)

/** How the favorites list is stored inside DataStore: a JSON array under a single key. */
object FavoritesCodec {
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(favorites: List<RevistaRef>): String =
        json.encodeToString(favorites.map { FavoriteDto(it.id, it.name, it.web, it.pages, it.scale) })

    fun decode(raw: String?): List<RevistaRef> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FavoriteDto>>(raw) }
            .getOrDefault(emptyList())
            .map { RevistaRef(it.id, it.name, it.web, it.pages, it.scale) }
    }
}
