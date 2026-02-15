package de.linkel.model.config

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.*

data class PaperlessConnector(
    val engine: HttpClientEngine,
    val baseUrl: String,
    private val token: String,
) {
    val client = HttpClient(engine) {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(ContentNegotiation) {
            jackson()
        }
        defaultRequest {
            headers {
                append(HttpHeaders.Accept, "application/json")
                append(HttpHeaders.Authorization, "Token $token")
            }
        }
    }
    val objectMapper = ObjectMapper(JsonFactory()).registerKotlinModule()

    fun url(type: String) = "$baseUrl/api/$type/"
    fun url(type: String, id: Long) = "$baseUrl/api/$type/$id/"
}