package de.linkel.service

import de.linkel.model.config.PaperlessConnector
import de.linkel.model.paperless.Correspondent
import de.linkel.model.paperless.CustomField
import de.linkel.model.paperless.CustomFieldValue
import de.linkel.model.paperless.DocumentType
import de.linkel.model.paperless.StoragePath
import de.linkel.model.paperless.Tag

class DefaultPaperlessContextFactory: PaperlessContextFactory {
    override suspend fun createContext(connector: PaperlessConnector): PaperlessContext {
        return PaperlessContext(
            tags = DefaultPaperlessLookup(connector, "tags", Tag::class.java).also { it.load() },
            correspondents = DefaultPaperlessLookup(connector, "correspondents", Correspondent::class.java).also { it.load() },
            documentTypes = DefaultPaperlessLookup(connector, "document_types", DocumentType::class.java).also { it.load() },
            storagePaths = DefaultPaperlessLookup(connector, "storage_paths", StoragePath::class.java).also { it.load() },
            customFields = DefaultPaperlessLookup(connector, "custom_fields", CustomField::class.java).also { it.load() },
        )
    }
}