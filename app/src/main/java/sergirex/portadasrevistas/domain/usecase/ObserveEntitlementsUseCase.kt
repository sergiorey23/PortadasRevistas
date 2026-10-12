package sergirex.portadasrevistas.domain.usecase

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.repository.EntitlementsRepository
import javax.inject.Inject

class ObserveEntitlementsUseCase @Inject constructor(
    private val repository: EntitlementsRepository
) {
    operator fun invoke(): Flow<Entitlements> = repository.entitlements
}
