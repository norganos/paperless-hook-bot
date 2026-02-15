package de.linkel.de.linkel

import de.linkel.model.config.PaperlessConnector
import de.linkel.service.PaperlessContext
import de.linkel.service.PaperlessContextFactory

open class StaticPaperlessContextFactory(
    private val context: PaperlessContext
): PaperlessContextFactory {
    override suspend fun createContext(connector: PaperlessConnector): PaperlessContext {
        return context
    }
}