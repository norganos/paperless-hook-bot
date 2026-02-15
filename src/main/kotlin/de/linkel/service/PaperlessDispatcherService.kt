package de.linkel.service

import de.linkel.model.config.PaperlessConfig
import de.linkel.model.config.PaperlessConnector
import io.ktor.client.engine.HttpClientEngine

class PaperlessDispatcherService(
    private val clientEngine: HttpClientEngine,
    configs: List<PaperlessConfig>
) {
    private val instances = configs
        .map { it.url.trimEnd('/') to it }
        .map { (url, config) -> ("$url/") to PaperlessInstanceService(
            PaperlessConnector(clientEngine, url, config.token),
            DefaultPaperlessContextFactory(),
            config.rules
        ) }

    fun getInstance(url: String): PaperlessInstanceService? {
        return instances
            .firstOrNull { (baseUrl, _) -> url.startsWith(baseUrl) }
            ?.second
    }
}