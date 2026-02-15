package de.linkel.de.linkel.service

import de.linkel.model.config.PaperlessConnector
import de.linkel.model.paperless.*
import de.linkel.service.DefaultPaperlessLookup
import io.ktor.client.engine.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class DefaultPaperlessLookupTest {
    @Test
    fun `one tag can be read from list response`() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine.config {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 8,
                                        "slug": "auto",
                                        "name": "Auto"
                                    }
                                ]
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }.create()

            val connector = PaperlessConnector(mockEngine, url, "token")
            val lookup = DefaultPaperlessLookup(connector, "tags", Tag::class.java)
            lookup.load()

            assertThat(lookup.size).isEqualTo(1)
        }
    }

    @Test
    fun `custom string field can be retrieved`() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine.config {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 2,
                                        "name": "Versicherungsschein-Nummer",
                                        "data_type": "string",
                                        "extra_data": {
                                            "select_options": [],
                                            "default_currency": null
                                        },
                                        "document_count": 29
                                    }
                                ]
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }.create()

            val connector = PaperlessConnector(mockEngine, url, "token")
            val lookup = DefaultPaperlessLookup(connector, "custom_fields", CustomField::class.java)
            lookup.load()

            assertThat(lookup.size).isEqualTo(1)
            assertThat(lookup.resolve("Versicherungsschein-Nummer")).isInstanceOf(StringCustomField::class.java)
        }
    }

    @Test
    fun `custom int field can be retrieved`() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine.config {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 3,
                                        "name": "USt-Satz",
                                        "data_type": "integer",
                                        "extra_data": {
                                            "select_options": [],
                                            "default_currency": null
                                        },
                                        "document_count": 9
                                    }
                                ]
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }.create()

            val connector = PaperlessConnector(mockEngine, url, "token")
            val lookup = DefaultPaperlessLookup(connector, "custom_fields", CustomField::class.java)
            lookup.load()

            assertThat(lookup.size).isEqualTo(1)
            assertThat(lookup.resolve("USt-Satz")).isInstanceOf(IntegerCustomField::class.java)
        }
    }

    @Test
    fun `custom monetary field can be retrieved`() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine.config {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 1,
                                        "name": "Betrag",
                                        "data_type": "monetary",
                                        "extra_data": {
                                            "select_options": [],
                                            "default_currency": "EUR"
                                        },
                                        "document_count": 9
                                    }
                                ]
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }.create()

            val connector = PaperlessConnector(mockEngine, url, "token")
            val lookup = DefaultPaperlessLookup(connector, "custom_fields", CustomField::class.java)
            lookup.load()

            assertThat(lookup.size).isEqualTo(1)
            assertThat(lookup.resolve("Betrag")).isInstanceOf(MonetaryCustomField::class.java)
        }
    }

    @Test
    fun `custom select field can be retrieved`() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine.config {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                                "count": 1,
                                "next": null,
                                "previous": null,
                                "results": [
                                    {
                                        "id": 4,
                                        "name": "Auswahl",
                                        "data_type": "select",
                                        "extra_data": {
                                            "select_options": [
                                                {
                                                    "id": "lJ4ofdbVXIWaF7wE",
                                                    "label": "Eins"
                                                },
                                                {
                                                    "id": "4qHcOV3RVMFbyD7l",
                                                    "label": "Zwei"
                                                }
                                            ],
                                            "default_currency": null
                                        },
                                        "document_count": 9
                                    }
                                ]
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }.create()

            val connector = PaperlessConnector(mockEngine, url, "token")
            val lookup = DefaultPaperlessLookup(connector, "custom_fields", CustomField::class.java)
            lookup.load()

            assertThat(lookup.size).isEqualTo(1)
            assertThat(lookup.resolve("Auswahl")).isInstanceOf(SelectCustomField::class.java)
            val select = lookup.resolve("Auswahl") as SelectCustomField
            assertThat(select.selectOptions).hasSize(2)
        }
    }

}