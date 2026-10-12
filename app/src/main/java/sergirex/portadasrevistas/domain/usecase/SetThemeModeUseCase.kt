package sergirex.portadasrevistas.domain.usecase

import sergirex.portadasrevistas.domain.model.ThemeMode
import sergirex.portadasrevistas.domain.repository.ThemeRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val repository: ThemeRepository
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}
