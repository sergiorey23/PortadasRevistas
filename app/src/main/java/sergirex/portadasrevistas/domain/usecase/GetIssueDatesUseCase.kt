package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.repository.IssueCalendarRepository
import javax.inject.Inject

class GetIssueDatesUseCase @Inject constructor(
    private val repository: IssueCalendarRepository
) {
    suspend operator fun invoke(magazineId: String): Result<List<String>> = repository.getIssueDates(magazineId)
}
