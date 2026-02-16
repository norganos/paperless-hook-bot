package de.linkel.model.dto

data class SingleDocumentTask(
    override val instance: String,
    val documentId: Long,
    override val attempt: Int = 0
): DocumentTask {
    override fun retryCopy() = copy(attempt = attempt + 1)

    override fun toString(): String {
        return "Document $documentId on $instance"
    }
}
