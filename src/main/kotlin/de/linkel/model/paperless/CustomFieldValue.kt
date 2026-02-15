package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CustomFieldValue(
    val field: Long,
    val value: Any
)