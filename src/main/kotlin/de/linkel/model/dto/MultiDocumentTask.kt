package de.linkel.model.dto

import de.linkel.model.api.DocumentQuery

data class MultiDocumentTask(
    override val instance: String,
    val query: DocumentQuery,
    override val attempt: Int = 0
): DocumentTask {
    override fun retryCopy() = copy(attempt = attempt + 1)

    override fun toString(): String {
        return "$query on $instance"
    }
}
