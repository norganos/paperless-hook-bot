package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class DocumentType(
    override val id: Long,
    override val slug: String,
    override val name: String,
): BaseObj