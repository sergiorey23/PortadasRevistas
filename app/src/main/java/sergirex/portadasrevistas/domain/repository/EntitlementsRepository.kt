package sergirex.portadasrevistas.domain.repository

import kotlinx.coroutines.flow.Flow
import sergirex.portadasrevistas.domain.model.Entitlements

/**
 * What the user has bought, as last confirmed by Google Play. Written by the billing flow,
 * observed by the screens that show ads or gate the magazine reader.
 */
interface EntitlementsRepository {
    val entitlements: Flow<Entitlements>

    suspend fun current(): Entitlements

    suspend fun setAdsRemoved(removed: Boolean)
    suspend fun setPremium(premium: Boolean)
}
