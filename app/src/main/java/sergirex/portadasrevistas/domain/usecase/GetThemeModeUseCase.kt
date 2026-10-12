package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.ThemeMode
import sergirex.portadasrevistas.domain.repository.ThemeRepository
import javax.inject.Inject

class GetThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    operator fun invoke(): Flow<ThemeMode> = repository.themeMode
}
