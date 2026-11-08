package sergirex.portadasrevistas.data.catalog

/** Builds catalog JSON for tests: [count] valid magazines (ids p0, p1, ...) plus any [extra] raw entries. */
fun catalogJson(
    version: Int = 1,
    schema: Int = 1,
    count: Int = 12,
    prefix: String = "p",
    extra: List<String> = emptyList()
): String {
    val valid = (0 until count).map {
        """{"id":"$prefix$it","name":"Magazine $it","category":"CORAZON","web":"$prefix$it.es","pages":100,"scale":"74"}"""
    }
    return """{"schemaVersion":$schema,"catalogVersion":$version,"revistas":[${(valid + extra).joinToString(",")}]}"""
}
