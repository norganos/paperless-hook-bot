package de.linkel.model.api

data class DocumentQuery(
    val tags: List<String> = emptyList(),
    val correspondents: List<String> = emptyList(),
    val documentTypes: List<String> = emptyList(),
    val storagePaths: List<String> = emptyList(),
) {
    override fun toString(): String {
        return listOf(
            "tags" to tags,
            "correspondents" to correspondents,
            "documentTypes" to documentTypes,
            "storagePaths" to storagePaths,
        )
            .filter { (_, values) -> values.isNotEmpty() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" && ") { (key, values) -> "$key in ['${values.joinToString("','")}']" }
            ?.let { "documents with $it" }
            ?: "all documents"
    }
}
