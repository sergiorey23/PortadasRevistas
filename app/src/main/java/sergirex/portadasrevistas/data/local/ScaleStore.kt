package sergirex.portadasrevistas.data.local

/** Remembers, per magazine, the page scale the image service accepted last time. */
interface ScaleStore {
    suspend fun get(magazineId: String): Int?
    suspend fun put(magazineId: String, scale: Int)
}
