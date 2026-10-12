package sergirex.portadasrevistas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import sergirex.portadasrevistas.domain.model.Entitlements
import sergirex.portadasrevistas.domain.usecase.ObserveEntitlementsUseCase
import javax.inject.Inject

/** What the user has bought, for the screens that show ads or gate the magazine reader. */
@HiltViewModel
class EntitlementsViewModel @Inject constructor(
    observeEntitlements: ObserveEntitlementsUseCase
) : ViewModel() {

    val entitlements: StateFlow<Entitlements> = observeEntitlements()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Entitlements())
}
