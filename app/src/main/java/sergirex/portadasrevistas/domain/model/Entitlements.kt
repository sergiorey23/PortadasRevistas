package sergirex.portadasrevistas.domain.model

/** What the user has bought: the one-time purchase that removes the ads. */
data class Entitlements(
    val adsRemoved: Boolean = false
) {
    val showAds: Boolean get() = !adsRemoved
}
