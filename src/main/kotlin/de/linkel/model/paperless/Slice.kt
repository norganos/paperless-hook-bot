package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Slice<T>(
    val count: Long,
    val next: String?,
    val previous: String?,
    val results: List<T>
)