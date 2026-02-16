package de.linkel.service

import de.linkel.model.config.PaperlessConfig
import de.linkel.model.config.PaperlessConnector
import io.ktor.client.engine.HttpClientEngine

class PaperlessRegistryService(
    private val clientEngine: HttpClientEngine,
    configs: List<PaperlessConfig>
) {
    data class NamedInstance(val baseUrl: String, val instance: PaperlessInstanceService)
    private val instances = configs
            .map { it.url.trimEnd('/') to it }
            .associate { (url, config) ->
                config.name to NamedInstance(
                    "$url/",
                    PaperlessInstanceService(
                        PaperlessConnector(clientEngine, url, config.token),
                        DefaultPaperlessContextFactory(),
                        config.rules,
                        config.name,
                    )
                )
            }

    fun getInstanceForUrl(url: String): PaperlessInstanceService? {
        return instances
            .values
            .firstOrNull { url.startsWith(it.baseUrl) }
            ?.instance
    }
    fun getInstanceByName(name: String): PaperlessInstanceService? {
        return instances[name]?.instance
    }
}