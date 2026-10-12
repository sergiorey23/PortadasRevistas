package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.PageScaleRepository
import javax.inject.Inject

class ResolvePageScaleUseCase @Inject constructor(
    private val repository: PageScaleRepository
) {
    /** The scale (as the string the page URLs use) the reader should request pages at. */
    suspend operator fun invoke(magazineId: String, date: String, preferredScale: String): String {
        val preferred = preferredScale.toIntOrNull() ?: return preferredScale
        return repository.resolve(magazineId, date, preferred).toString()
    }

}
