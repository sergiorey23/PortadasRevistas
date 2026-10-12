package sergirex.portadasrevistas.presentation.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sergirex.portadasrevistas.domain.usecase.GetReadingProgressUseCase
import sergirex.portadasrevistas.domain.usecase.ResolvePageScaleUseCase
import sergirex.portadasrevistas.domain.usecase.SaveReadingProgressUseCase
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val getReadingProgress: GetReadingProgressUseCase,
    private val saveReadingProgress: SaveReadingProgressUseCase,
    private val resolvePageScale: ResolvePageScaleUseCase
) : ViewModel() {

    private val _scale = MutableStateFlow<String?>(null)

    /** The page scale to request, or null while the sharpest one the service accepts is still being looked up. */
    val scale: StateFlow<String?> = _scale.asStateFlow()

    private var resolving = false

    /** Looks the scale up once; later calls (e.g. after a rotation) keep the answer. */
    fun resolveScale(magazineId: String, date: String, preferredScale: String) {
        if (resolving) return
        resolving = true
        viewModelScope.launch { _scale.value = resolvePageScale(magazineId, date, preferredScale) }
    }

    suspend fun savedPage(magazineId: String, date: String): Int = getReadingProgress(magazineId, date)

    /** Runs in the ViewModel's scope, so the write isn't cancelled when the Activity goes away. */
    fun savePage(magazineId: String, date: String, page: Int) {
        viewModelScope.launch { saveReadingProgress(magazineId, date, page) }
    }
}
