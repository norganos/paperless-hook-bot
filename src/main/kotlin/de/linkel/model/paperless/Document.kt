package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Document(
    val id: Long,
    val correspondent: Long? = null,
    @JsonProperty("document_type") val documentType: Long? = null,
    @JsonProperty("storage_path") val storagePath: Long? = null,
    val title: String? = null,
    val tags: List<Long> = emptyList(),
    val created: String? = null,
    @JsonProperty("original_file_name") val filename: String,
    @JsonProperty("custom_fields") val customFields: List<CustomFieldValue> = emptyList(),
)