package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class IdSlice(
    val count: Long,
    val next: String?,
    val previous: String?,
    val all: List<Long>
)