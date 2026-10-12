package sergirex.portadasrevistas.data.local

import kotlinx.serialization.Serializable
import sergirex.portadasrevistas.domain.model.Revista
import sergirex.portadasrevistas.domain.model.RevistaCategory

/**
 * Wire shape of each entry in assets/revistas.json. Kept apart from the domain [Revista] on
 * purpose: the JSON schema is a data-layer detail (today a bundled asset, tomorrow possibly a
 * remote endpoint) and shouldn't leak serialization annotations into the domain or UI layers.
 */
@Serializable
data class RevistaDto(
    val id: String,
    val name: String,
    val category: String,
    val web: String = "",
    val pages: Int = Revista.DEFAULT_PAGES,
    val scale: String = Revista.DEFAULT_SCALE
)

fun RevistaDto.toDomain(): Revista = Revista(
    id = id,
    name = name,
    web = web,
    pages = pages,
    scale = scale,
    category = RevistaCategory.valueOf(category)
)
