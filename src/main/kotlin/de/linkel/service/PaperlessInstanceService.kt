package de.linkel.service

import de.linkel.model.config.PaperlessConnector
import de.linkel.model.config.RuleConfig
import de.linkel.model.paperless.Document
import de.linkel.model.paperless.DocumentPatch
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.lang.Math.pow
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class PaperlessInstanceService(
    private val connector: PaperlessConnector,
    private val contextFactory: PaperlessContextFactory,
    val rules: List<RuleConfig>,
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

    fun extractDocumentId(document: String): Long? {
        return document.substringBeforeLast('/').substringAfterLast('/')
            .toLongOrNull()
//        val docUrl = connector.url("documents")
//        return document
//            .takeIf { it.startsWith(docUrl) }
//            ?.substring(docUrl.length)
//            ?.substringBefore('/')
//            ?.toLongOrNull()
    }
}