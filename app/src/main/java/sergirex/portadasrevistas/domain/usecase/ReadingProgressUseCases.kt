package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.ReadingProgressRepository
import javax.inject.Inject

class GetReadingProgressUseCase @Inject constructor(
    private val repository: ReadingProgressRepository
) {
    suspend operator fun invoke(magazineId: String, date: String): Int = repository.getPage(magazineId, date)
}

class SaveReadingProgressUseCase @Inject constructor(
    private val repository: ReadingProgressRepository
) {
    suspend operator fun invoke(magazineId: String, date: String, page: Int) =
        repository.savePage(magazineId, date, page)
}
