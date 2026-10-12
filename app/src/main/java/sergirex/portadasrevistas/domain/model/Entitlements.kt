package sergirex.portadasrevistas.domain.model

/**
 * What the user has bought. Premium (the subscription) includes everything the one-time
 * "remove ads" purchase gives, plus reading the magazines' pages.
 */
data class Entitlements(
    val adsRemoved: Boolean = false,
    val premium: Boolean = false
) {
    val showAds: Boolean get() = !adsRemoved && !premium
    val canReadMagazines: Boolean get() = premium
}
