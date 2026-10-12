package sergirex.portadasrevistas.presentation.portadas

import androidx.annotation.StringRes
import sergirex.portadasrevistas.R
import sergirex.portadasrevistas.domain.model.HomeTab
import sergirex.portadasrevistas.domain.model.RevistaCategory

@get:StringRes
val HomeTab.titleRes: Int
    get() = when (this) {
        HomeTab.Favorites -> R.string.fav_tab
        is HomeTab.Category -> when (category) {
            RevistaCategory.CORAZON -> R.string.first_tab
            RevistaCategory.CULTURA -> R.string.second_tab
            RevistaCategory.DEPORTES -> R.string.third_tab
            RevistaCategory.ESTILO_DE_VIDA -> R.string.fourth_tab
            RevistaCategory.FEMENINAS -> R.string.fifth_tab
            RevistaCategory.MASCULINAS -> R.string.sixth_tab
            RevistaCategory.MOTOR -> R.string.seventh_tab
            RevistaCategory.TECNOLOGIA -> R.string.eight_tab
            RevistaCategory.COCINA -> R.string.nineth_tab
            RevistaCategory.HISTORIA -> R.string.tenth_tab
        }
    }
