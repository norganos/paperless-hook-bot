package de.linkel.service

import com.fasterxml.jackson.databind.node.ArrayNode
import de.linkel.model.config.PaperlessConnector
import de.linkel.model.paperless.BaseObj
import io.ktor.client.request.*
import io.ktor.client.statement.*

class DefaultPaperlessLookup<T: BaseObj>(
    private val connector: PaperlessConnector,
    val typeName: String,
    private val clazz: Class<T>,
) : PaperlessLookup<T> {
    private val byId = mutableMapOf<Long, T>()
    private val byNameOrSlug = mutableMapOf<String, T>()

    val content get() = byId.values
    val size get() = byId.size

    suspend fun load() {
        return load(connector.url(typeName))
    }
    suspend fun load(url: String) {
        val responseText = connector.client.get(url).bodyAsText()
        val jsonSlice = connector.objectMapper.readTree(responseText)
        jsonSlice.get("results")
            ?.takeIf { it.isArray }
            ?.let { it as ArrayNode }
            ?.map { connector.objectMapper.treeToValue(it, clazz) }
            ?.forEach {
                byId[it.id] = it
                byNameOrSlug[it.slug] = it
                byNameOrSlug[it.name] = it
            }
        jsonSlice.get("next")
            ?.takeIf { it.isTextual }
            ?.asText()
            ?.takeIf { it.isNotBlank() }
            ?.also { load(it) }
    }

    override fun resolve(value: String): T? {
        //TODO: as the lookup may be cached, we should be able to lazy load unknown elements?
        return byNameOrSlug[value]
    }

    override fun get(id: Long): T? {
        return byId[id]
    }
}