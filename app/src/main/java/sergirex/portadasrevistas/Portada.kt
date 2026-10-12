package sergirex.portadasrevistas

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import sergirex.portadasrevistas.domain.model.RevistaCategory
import sergirex.portadasrevistas.domain.model.RevistaRef

/** One magazine in the detail screen's pager (the form [RevistaRef] takes inside an Intent). */
@Parcelize
data class Portada(
    val id: String,
    val name: String,
    val web: String,
    val pages: Int,
    val scale: String,
    val category: RevistaCategory? = null
) : Parcelable {
    fun toRef() = RevistaRef(id, name, web, pages, scale, category)

    companion object {
        fun from(ref: RevistaRef) = Portada(ref.id, ref.name, ref.web, ref.pages, ref.scale, ref.category)
    }
}

/** A name safe to use in a file name, optionally followed by the [suffix] that tells copies apart (e.g. the issue date). */
fun RevistaRef.fileName(suffix: String? = null): String {
    val base = name.replace(Regex("[^\\p{L}\\p{N}]+"), "_").trim('_').ifEmpty { id }
    return if (suffix == null) base else "${base}_$suffix"
}
