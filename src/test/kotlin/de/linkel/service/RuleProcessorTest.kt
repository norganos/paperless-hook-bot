package de.linkel.de.linkel.service

import de.linkel.de.linkel.TestPaperlessContextFactory
import de.linkel.model.config.RuleConditions
import de.linkel.model.config.RuleConfig
import de.linkel.model.config.RuleSets
import de.linkel.model.paperless.Document
import de.linkel.service.RuleProcessor
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test


class RuleProcessorTest {
    @Test
    fun `no rules result in no patch`() {
        val patch = RuleProcessor(TestPaperlessContextFactory.context, emptyList())
                .patchDocument(
                    Document(id = 1L, filename = "test.pdf")
                )
        assertThat(patch).isNull()
    }

    @Test
    fun `no patch if no rule matches`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "prefix.*\\.pdf"),
                        set = RuleSets(
                            title = "hallo welt",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "test.pdf")
            )
        assertThat(patch).isNull()
    }

    @Test
    fun `only static title patch works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = ".*\\.pdf"),
                        set = RuleSets(
                            title = "hallo welt",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "test.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.title).isEqualTo("hallo welt")
        assertThat(patch.documentType).isNull()
        assertThat(patch.correspondent).isNull()
        assertThat(patch.storagePath).isNull()
        assertThat(patch.tags).isNull()
        assertThat(patch.customFields).isNull()
    }

    @Test
    fun `dynamic title patch works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(.*)\\.pdf"),
                        set = RuleSets(
                            title = $$"hallo $filename1",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "test.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.title).isEqualTo("hallo test")
        assertThat(patch.documentType).isNull()
        assertThat(patch.correspondent).isNull()
        assertThat(patch.storagePath).isNull()
        assertThat(patch.tags).isNull()
        assertThat(patch.customFields).isNull()
    }

    @Test
    fun `complex dynamic title assignment works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d+)_(\\d{4})_Nr\\.(\\d+)_Kontoauszug_vom_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"),
                        set = RuleSets(
                            title = $$"$filename1 Kontoauszug $filename2-$filename3",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "12345_2025_Nr.001_Kontoauszug_vom_2025.01.31_20260208110249488.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.title).isEqualTo("12345 Kontoauszug 2025-001")
        assertThat(patch.documentType).isNull()
        assertThat(patch.correspondent).isNull()
        assertThat(patch.storagePath).isNull()
        assertThat(patch.tags).isNull()
        assertThat(patch.customFields).isNull()
    }

    @Test
    fun `documentType resolution works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d+)_(\\d{4})_Nr\\.(\\d+)_Kontoauszug_vom_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"),
                        set = RuleSets(
                            documentType = "Kontoauszug",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "12345_2025_Nr.001_Kontoauszug_vom_2025.01.31_20260208110249488.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.documentType).isEqualTo(3L)
        assertThat(patch.title).isNull()
        assertThat(patch.correspondent).isNull()
        assertThat(patch.storagePath).isNull()
        assertThat(patch.tags).isNull()
        assertThat(patch.customFields).isNull()
    }

    @Test
    fun `correspondent resolution works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d+)_(\\d{4})_Nr\\.(\\d+)_Kontoauszug_vom_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"),
                        set = RuleSets(
                            correspondent = "Bank",
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "12345_2025_Nr.001_Kontoauszug_vom_2025.01.31_20260208110249488.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.correspondent).isEqualTo(1L)
        assertThat(patch.title).isNull()
        assertThat(patch.documentType).isNull()
        assertThat(patch.storagePath).isNull()
        assertThat(patch.tags).isNull()
        assertThat(patch.customFields).isNull()
    }

    @Test
    fun `full set of fields works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d+)_(\\d{4})_Nr\\.(\\d+)_Kontoauszug_vom_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"),
                        set = RuleSets(
                            title = $$"$filename1 Kontoauszug $filename2-$filename3",
                            correspondent = "Bank",
                            documentType = "Kontoauszug",
                            storagePath = "Privat",
                            tags = listOf("rot"),
                            custom = mapOf(
                                "Betrag" to "23",
                                "Konto" to $$"KTO $filename1",
                            )
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "12345_2025_Nr.001_Kontoauszug_vom_2025.01.31_20260208110249488.pdf")
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.title).isEqualTo("12345 Kontoauszug 2025-001")
        assertThat(patch.correspondent).isEqualTo(1L)
        assertThat(patch.documentType).isEqualTo(3L)
        assertThat(patch.storagePath).isEqualTo(1L)
        assertThat(patch.tags).contains(1L)
        assertThat(patch.customFields).hasSize(2)
        val betrag = patch.customFields!!.firstOrNull { it.field == 3L }!!
        assertThat(betrag).isNotNull
        assertThat(betrag.value).isEqualTo("EUR23.00")
        val konto = patch.customFields.firstOrNull { it.field == 4L }!!
        assertThat(konto).isNotNull
        assertThat(konto.value).isEqualTo("opt12345")
    }

    @Test
    fun `tag removal works`() {
        val patch = RuleProcessor(
                TestPaperlessContextFactory.context,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d+)_(\\d{4})_Nr\\.(\\d+)_Kontoauszug_vom_(\\d{4})\\.(\\d{2})\\.(\\d{2})_\\d+\\.pdf"),
                        set = RuleSets(
                            tags = listOf("blau"),
                            removeTags = listOf("rot")
                        )
                    )
                )
            )
            .patchDocument(
                Document(id = 1L, filename = "12345_2025_Nr.001_Kontoauszug_vom_2025.01.31_20260208110249488.pdf", tags = listOf(1L, 2L))
            )
        assertThat(patch).isNotNull
        assertThat(patch!!.tags).hasSize(2)
        assertThat(patch.tags).contains(2L)
        assertThat(patch.tags).contains(3L)
    }
}