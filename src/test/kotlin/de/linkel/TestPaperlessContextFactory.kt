package de.linkel.de.linkel

import de.linkel.model.paperless.Correspondent
import de.linkel.model.paperless.DocumentType
import de.linkel.model.paperless.IntegerCustomField
import de.linkel.model.paperless.MonetaryCustomField
import de.linkel.model.paperless.SelectCustomField
import de.linkel.model.paperless.StoragePath
import de.linkel.model.paperless.StringCustomField
import de.linkel.model.paperless.Tag
import de.linkel.service.PaperlessContext

class TestPaperlessContextFactory: StaticPaperlessContextFactory(context) {
    companion object {
        val context = PaperlessContext(
            tags = StaticPaperlessLookup(
                listOf(
                    Tag(1L, "rot", "rot"),
                    Tag(2L, "gruen", "grün"),
                    Tag(3L, "blau", "blau"),
                )
            ),
            correspondents = StaticPaperlessLookup(
                listOf(
                    Correspondent(1L, "bank", "Bank"),
                    Correspondent(2L, "versicherung", "Versicherung"),
                    Correspondent(3L, "finanzamt", "Finanzamt"),
                )
            ),
            documentTypes = StaticPaperlessLookup(
                listOf(
                    DocumentType(1L, "rechnung", "Rechnung"),
                    DocumentType(2L, "vertrag", "Vertrag"),
                    DocumentType(3L, "kontoauszug", "Kontoauszug"),
                )
            ),
            storagePaths = StaticPaperlessLookup(
                listOf(
                    StoragePath(1L, "privat", "Privat"),
                    StoragePath(2L, "verein", "Verein"),
                )
            ),
            customFields = StaticPaperlessLookup(
                listOf(
                    StringCustomField(1L, "Kontonummer"),
                    IntegerCustomField(2L, "Seiten"),
                    MonetaryCustomField(3L, "Betrag", "EUR"),
                    SelectCustomField(4L, "Konto", listOf(SelectCustomField.SelectOption("opt12345", "KTO 12345"), SelectCustomField.SelectOption("opt4711", "KTO 4711"))),
                )
            ),
        )
    }
}

