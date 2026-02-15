package de.linkel.service

import de.linkel.model.paperless.Correspondent
import de.linkel.model.paperless.CustomField
import de.linkel.model.paperless.DocumentType
import de.linkel.model.paperless.StoragePath
import de.linkel.model.paperless.Tag

data class PaperlessContext(
    val tags: PaperlessLookup<Tag>,
    val correspondents: PaperlessLookup<Correspondent>,
    val documentTypes: PaperlessLookup<DocumentType>,
    val storagePaths: PaperlessLookup<StoragePath>,
    val customFields: PaperlessLookup<CustomField>,
)