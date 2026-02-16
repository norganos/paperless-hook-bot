package de.linkel.de.linkel.service

import de.linkel.de.linkel.TestPaperlessContextFactory
import de.linkel.model.config.PaperlessConnector
import de.linkel.model.config.RuleConditions
import de.linkel.model.config.RuleConfig
import de.linkel.model.config.RuleSets
import de.linkel.service.PaperlessInstanceService
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class PaperlessInstanceTest {

    @Test
    fun sampleClientTest() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine(MockEngineConfig().apply {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                              "id": 289,
                              "correspondent": null,
                              "document_type": null,
                              "storage_path": null,
                              "title": "",
                              "content": "",
                              "tags": [],
                              "created": "2026-01-16",
                              "created_date": "2026-01-16",
                              "modified": "2026-02-13T08:57:47.069411+01:00",
                              "added": "2026-02-13T07:59:39.960394+01:00",
                              "deleted_at": null,
                              "archive_serial_number": null,
                              "original_file_name": "2026-02-13_Test_von_Welt.pdf",
                              "owner": null,
                              "user_can_change": true,
                              "is_shared_by_requester": false,
                              "notes": [],
                              "custom_fields": [],
                              "page_count": 1,
                              "mime_type": "application/pdf"
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                addHandler {
                    respond("""
                            {
                              "id": 289,
                              "correspondent": 1,
                              "document_type": 3,
                              "storage_path": null,
                              "title": "Hallo Welt (2026-02)",
                              "content": "",
                              "tags": [],
                              "created": "2026-01-16",
                              "created_date": "2026-01-16",
                              "modified": "2026-02-13T08:57:47.069411+01:00",
                              "added": "2026-02-13T07:59:39.960394+01:00",
                              "deleted_at": null,
                              "archive_serial_number": null,
                              "original_file_name": "2026-02-13_Test_von_Welt.pdf",
                              "owner": null,
                              "user_can_change": true,
                              "is_shared_by_requester": false,
                              "notes": [],
                              "custom_fields": [],
                              "page_count": 1,
                              "mime_type": "application/pdf"
                            }""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            })

            val connector = PaperlessConnector(mockEngine, url, "token")
            val contextFactory = TestPaperlessContextFactory()
            val service = PaperlessInstanceService(
                connector,
                contextFactory,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d{4})-(\\d{2})-(\\d{2})_Test_von_(.+)\\.pdf"),
                        set = RuleSets(
                            title = $$"Hallo $filename4 ($filename1-$filename2)",
                            correspondent = "Bank",
                            documentType = "Kontoauszug",
                        )
                    )
                ),
                "test"
            )
            service.process(289L)
            assertThat(mockEngine.requestHistory.size).isEqualTo(2)
            assertThat(mockEngine.requestHistory.last().method).isEqualTo(HttpMethod.Patch)
            val body = String(mockEngine.requestHistory.last().body.toByteArray())
            assertThat(body).contains("\"Hallo Welt (2026-02)\"")
            val json = connector.objectMapper.readTree(body)
            assertThat(json).hasSize(3)
        }
    }
    @Test
    fun requestIsRetriedClientTest() {
        val url = "http://mock"
        runBlocking {
            val mockEngine = MockEngine(MockEngineConfig().apply {
                addHandler {
                    respond(
                        content = ByteReadChannel("""
                            {
                              "id": 289,
                              "correspondent": null,
                              "document_type": null,
                              "storage_path": null,
                              "title": "",
                              "content": "",
                              "tags": [],
                              "created": "2026-01-16",
                              "created_date": "2026-01-16",
                              "modified": "2026-02-13T08:57:47.069411+01:00",
                              "added": "2026-02-13T07:59:39.960394+01:00",
                              "deleted_at": null,
                              "archive_serial_number": null,
                              "original_file_name": "2026-02-13_Test_von_Welt.pdf",
                              "owner": null,
                              "user_can_change": true,
                              "is_shared_by_requester": false,
                              "notes": [],
                              "custom_fields": [],
                              "page_count": 1,
                              "mime_type": "application/pdf"
                            }""".trimIndent().trim()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                addHandler {
                    respond("""
                            {
                              "id": 289,
                              "correspondent": null,
                              "document_type": null,
                              "storage_path": null,
                              "title": "Hallo Welt (2026-02)",
                              "content": "",
                              "tags": [],
                              "created": "2026-01-16",
                              "created_date": "2026-01-16",
                              "modified": "2026-02-13T08:57:47.069411+01:00",
                              "added": "2026-02-13T07:59:39.960394+01:00",
                              "deleted_at": null,
                              "archive_serial_number": null,
                              "original_file_name": "2026-02-13_Test_von_Welt.pdf",
                              "owner": null,
                              "user_can_change": true,
                              "is_shared_by_requester": false,
                              "notes": [],
                              "custom_fields": [],
                              "page_count": 1,
                              "mime_type": "application/pdf"
                            }""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                addHandler {
                    respond("""
                            {
                              "id": 289,
                              "correspondent": 1,
                              "document_type": 3,
                              "storage_path": null,
                              "title": "Hallo Welt (2026-02)",
                              "content": "",
                              "tags": [],
                              "created": "2026-01-16",
                              "created_date": "2026-01-16",
                              "modified": "2026-02-13T08:57:47.069411+01:00",
                              "added": "2026-02-13T07:59:39.960394+01:00",
                              "deleted_at": null,
                              "archive_serial_number": null,
                              "original_file_name": "2026-02-13_Test_von_Welt.pdf",
                              "owner": null,
                              "user_can_change": true,
                              "is_shared_by_requester": false,
                              "notes": [],
                              "custom_fields": [],
                              "page_count": 1,
                              "mime_type": "application/pdf"
                            }""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            })

            val connector = PaperlessConnector(mockEngine, url, "token")
            val contextFactory = TestPaperlessContextFactory()
            val service = PaperlessInstanceService(
                connector,
                contextFactory,
                listOf(
                    RuleConfig(
                        conditions = RuleConditions(filename = "(\\d{4})-(\\d{2})-(\\d{2})_Test_von_(.+)\\.pdf"),
                        set = RuleSets(
                            title = $$"Hallo $filename4 ($filename1-$filename2)",
                            correspondent = "Bank",
                            documentType = "Kontoauszug",
                        )
                    )
                ),
                "test"
            )
            service.process(289L)
            assertThat(mockEngine.requestHistory.size).isEqualTo(3)
            assertThat(mockEngine.requestHistory.last().method).isEqualTo(HttpMethod.Patch)
            val body = String(mockEngine.requestHistory.last().body.toByteArray())
            val json = connector.objectMapper.readTree(body)
            assertThat(json).hasSize(2)
            assertThat(json.get("correspondent").asText()).isEqualTo("1")
            assertThat(json.get("document_type").asText()).isEqualTo("3")
        }
    }
}