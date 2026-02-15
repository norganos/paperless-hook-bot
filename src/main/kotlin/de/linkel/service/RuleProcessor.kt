package de.linkel.service

import de.linkel.model.config.RuleConditions
import de.linkel.model.config.RuleConfig
import de.linkel.model.config.RuleSets
import de.linkel.model.paperless.*

class RuleProcessor(
    private val context: PaperlessContext,
    private val rules: List<RuleConfig>
) {
    fun patchDocument(input: Document): DocumentPatch? {
        return DocumentPatch(input)
            .let { patch ->
                patch to rules
                    .map { it to evaluateConditions(it.conditions, patch) }
                    .filter { (_, placeholders) -> placeholders.isNotEmpty() }
                    .fold(patch) { p, (rule, placeholders) ->
                        rule.set.patchFields(p, placeholders)
                    }
            }
            .takeIf { (original, patched) -> original != patched }
            ?.let { (original, patched) -> patched.diffTo(original) }
    }

    fun evaluateConditions(conditions: RuleConditions, patch: DocumentPatch): Map<String, String> {
        return listOf(
            Triple("filename", conditions.filename, patch.filename),
        )
            .asSequence()
            .filter { (_, regex, value) -> !regex.isNullOrBlank() && !value.isNullOrBlank() }
            .map { (field, regex, value) -> Triple(field, regex!!, value!!) }
            .map { (field, regex, value) ->
                field to Regex(regex).matchEntire(value)
            }
            .filter { (_, match) -> match != null }
            .fold(emptyMap()) {
                    acc, (field, match) ->
                acc + match!!.groups.withIndex().associate { (idx, group) ->
                    "$$field$idx" to group!!.value
                }
            }
    }

    private fun String?.resolveToString(placeholders: Map<String, String>): String? {
        return this
            .takeIf { !it.isNullOrBlank() }
            ?.let { placeholders.subst(it) }
    }
    private fun <T: BaseObj> String?.resolveToBaseObj(lookup: PaperlessLookup<T>, placeholders: Map<String, String>): Long? {
        return this
            .resolveToString(placeholders)
            ?.let { lookup.resolve(it) }
            ?.id
    }
    private fun Map<String,String>?.resolveToCustomFields(lookup: PaperlessLookup<CustomField>, placeholders: Map<String, String>): List<CustomFieldValue>? {
        return this
            ?.entries
            ?.filter { (field, value) -> field.isNotBlank() && value.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.map { (field, value) -> field to placeholders.subst(value) }
            ?.mapNotNull { (field, value) -> lookup.resolve(field)?.toValue(value) }
            ?.takeIf { it.isNotEmpty() }
    }
    private fun List<String>?.resolveToTags(lookup: PaperlessLookup<Tag>, placeholders: Map<String, String>): List<Long>? {
        return this
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.map { placeholders.subst(it) }
            ?.mapNotNull { lookup.resolve(it)?.id }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun RuleSets.patchFields(patch: DocumentPatch, placeholders: Map<String, String>): DocumentPatch {
        return patch
            .patchTitle(this.title.resolveToString(placeholders))
            .patchCreated(this.created.resolveToString(placeholders))
            .patchDocumentType(this.documentType.resolveToBaseObj(context.documentTypes, placeholders))
            .patchCorrespondent(this.correspondent.resolveToBaseObj(context.correspondents, placeholders))
            .patchStoragePath(this.storagePath.resolveToBaseObj(context.storagePaths, placeholders))
            .patchCustomFields(this.custom.resolveToCustomFields(context.customFields, placeholders))
            .patchTags(this.tags.resolveToTags(context.tags, placeholders))
            .removeTags(this.removeTags.resolveToTags(context.tags, placeholders))
    }

    private fun Map<String,String>.subst(input: String) =
        this.entries.fold(input) { acc, (key, value) -> acc.replace(key, value) }
}