package sergirex.portadasrevistas.domain.model

/** A tab on the home screen: the user's favorites, or one catalog category. */
sealed interface HomeTab {
    /** Stable, language-independent identifier (also what the "initial tab" setting stores). */
    val key: String

    data object Favorites : HomeTab {
        override val key = "favorites"
    }

    data class Category(val category: RevistaCategory) : HomeTab {
        override val key: String get() = category.name
    }

    companion object {
        val categories: List<HomeTab> = RevistaCategory.entries.map(::Category)
        val default: HomeTab = Category(RevistaCategory.CORAZON)

        /**
         * Reads a stored key. Older versions saved the position of the tab ("0" = favorites,
         * "1".."10" = the categories in tab order); that's still understood here so nobody's chosen
         * initial tab resets on upgrade.
         */
        fun fromKey(key: String?): HomeTab {
            if (key == null) return default
            if (key == Favorites.key) return Favorites
            categories.firstOrNull { it.key == key }?.let { return it }
            val legacyPosition = key.toIntOrNull() ?: return default
            return when {
                legacyPosition == 0 -> Favorites
                legacyPosition in 1..RevistaCategory.entries.size -> Category(RevistaCategory.entries[legacyPosition - 1])
                else -> default
            }
        }
    }
}
