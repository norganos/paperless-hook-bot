package de.linkel.service

import de.linkel.model.config.PaperlessConnector
import de.linkel.model.paperless.Correspondent
import de.linkel.model.paperless.CustomField
import de.linkel.model.paperless.DocumentType
import de.linkel.model.paperless.StoragePath
import de.linkel.model.paperless.Tag

interface PaperlessContextFactory {
    suspend fun createContext(connector: PaperlessConnector): PaperlessContext
}