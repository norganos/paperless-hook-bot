package de.linkel.service

import de.linkel.model.api.DocumentQuery
import de.linkel.model.config.PaperlessConnector
import de.linkel.model.config.RuleConfig
import de.linkel.model.dto.DocumentPatch
import de.linkel.model.paperless.Document
import de.linkel.model.paperless.IdSlice
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

class PaperlessInstanceService(
    private val connector: PaperlessConnector,
    private val contextFactory: PaperlessContextFactory,
    val rules: List<RuleConfig>,
    val name: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(PaperlessInstanceService::class.java)
    }

    suspend fun fetchDocument(id: Long): Document? {
        return connector.client.get(connector.url("documents", id)).body()
    }

    suspend fun updateDocument(id: Long, patch: DocumentPatch, attempt: Int = 1) {
        val patchJson = connector.objectMapper.writeValueAsString(patch)
        log.info("Updating document $id with patch ${patchJson} (attempt $attempt)")
        val updated: Document = connector.client.patch(connector.url("documents", id)) {
            contentType(ContentType.Application.Json)
            setBody(patchJson)
        }.body()
        val diff = patch.diffTo(DocumentPatch(updated))
        if (!diff.isEmpty()) {
            log.warn("Could not update document $id => ${connector.objectMapper.writeValueAsString(diff)} remained")
            if (attempt > 7) {
                log.error("aborting update of document $id after ${attempt-1} attempts")
                return
            }
            val wait = 2.toDouble().pow(attempt-1).toInt()
            log.info("retrying after $wait seconds")
            delay(wait.seconds)
            updateDocument(id, diff, attempt + 1)
        } else {
            log.info("updated document $id successfully")
        }
    }

    suspend fun process(id: Long) {
        fetchDocument(id)
            ?.let { document ->
                val context = contextFactory.createContext(connector)
                RuleProcessor(context, rules).patchDocument(document)
            }
            ?.also { patch ->
               updateDocument(id, patch)
            }
    }

    private suspend fun querySlice(url: String): List<Long> {
        val slice: IdSlice = connector.client.get(url).body()
        return slice.all + if (slice.next != null) querySlice(slice.next) else emptyList()
    }

    suspend fun query(query: DocumentQuery): List<Long> {
        val context = contextFactory.createContext(connector)

        return URLBuilder(connector.url("documents")).apply {
            fun addParam(name: String, value: List<String>, lookup: PaperlessLookup<*>) {
                value
                    .mapNotNull { lookup.resolve(it) }
                    .map { it.id }
                    .takeIf { it.isNotEmpty() }
                    ?.also {
                        parameters.append(name, it.joinToString(","))
                    }
            }
            addParam("tags__id__all", query.tags, context.tags)
            addParam("correspondent__id__in", query.correspondents, context.correspondents)
            addParam("document_type__id__in", query.documentTypes, context.documentTypes)
            addParam("storage_path__id__in", query.storagePaths, context.storagePaths)
        }
            .takeIf { !it.parameters.isEmpty() }
            ?.let {
                querySlice(it.buildString())
            } ?: emptyList()
    }

    fun extractDocumentId(document: String): Long? {
        return document.substringBeforeLast('/').substringAfterLast('/')
            .toLongOrNull()
            ?.takeIf { document == "${connector.baseUrl}/documents/$it/" }
    }
}