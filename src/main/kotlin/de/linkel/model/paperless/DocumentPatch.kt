package de.linkel.model.paperless

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DocumentPatch(
    val correspondent: Long? = null,
    @JsonProperty("document_type") val documentType: Long? = null,
    @JsonProperty("storage_path") val storagePath: Long? = null,
    val title: String? = null,
    val created: String? = null,
    val tags: List<Long>? = null,
    @JsonProperty("original_file_name") val filename: String? = null,
    @JsonProperty("custom_fields") val customFields: List<CustomFieldValue>? = null,
) {
    constructor(document: Document) : this(
        correspondent = document.correspondent,
        documentType = document.documentType,
        storagePath = document.storagePath,
        title = document.title,
        created = document.created,
        tags = document.tags,
        filename = document.filename,
        customFields = document.customFields
    )

    fun patchCorrespondent(correspondent: Long?) = correspondent?.let { copy(correspondent = it) } ?: this
    fun patchDocumentType(documentType: Long?) = documentType?.let { copy(documentType = it ) } ?: this
    fun patchStoragePath(storagePath: Long?) = storagePath?.let { copy(storagePath = it ) } ?: this
    fun patchTitle(title: String?) = title?.let { copy(title = it ) } ?: this
    fun patchCreated(created: String?) = created?.let { copy(created = it ) } ?: this
    fun patchTags(tags: Iterable<Long>?) = tags?.let { copy(tags = ((this.tags ?: emptyList()) + it).toSet().toList()) } ?: this
    fun removeTags(tags: Iterable<Long>?) = tags?.let { copy(tags = ((this.tags ?: emptyList()) - it.toSet()).toList()) } ?: this
    fun patchCustomFields(customFields: Iterable<CustomFieldValue>?) = customFields?.let { cf ->
        copy(
            customFields = ((this.customFields ?: emptyList()).associateBy { it.field } + cf.associateBy { it.field })
                .values.toList()
        )
    } ?: this

    fun diffTo(original: DocumentPatch) = copy(
        correspondent = correspondent.takeIf { it != original.correspondent },
        documentType = documentType.takeIf { it != original.documentType },
        storagePath = storagePath.takeIf { it != original.storagePath },
        title = title.takeIf { it != original.title },
        created = created.takeIf { it != original.created },
        tags = tags.takeIf { it?.toSet() != original.tags?.toSet() },
        filename = filename.takeIf { it != original.filename },
        customFields = customFields.takeIf { it?.toSet() != original.customFields?.toSet() },
    )

    @JsonIgnore
    fun isEmpty(): Boolean {
        return     correspondent == null
                && documentType == null
                && storagePath == null
                && title == null
                && created == null
                && tags == null
                && filename == null
                && customFields == null
    }
}