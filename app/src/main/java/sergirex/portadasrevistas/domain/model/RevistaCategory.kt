package sergirex.portadasrevistas.domain.model

/**
 * The ten sections the app organizes magazines into. Single source of truth for "which categories
 * exist": it replaces ten near-identical Fragments (one per category), the parallel `String[]`
 * constants in Revistas.java and the hand-synced tab title strings.
 */
enum class RevistaCategory {
    CORAZON,
    CULTURA,
    DEPORTES,
    ESTILO_DE_VIDA,
    FEMENINAS,
    MASCULINAS,
    MOTOR,
    TECNOLOGIA,
    COCINA,
    HISTORIA
}
