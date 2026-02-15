package de.linkel

import de.linkel.model.config.Config
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.linkel.model.api.DocumentAddedBody
import de.linkel.model.dto.DocumentTask
import de.linkel.service.AuthService
import de.linkel.service.PaperlessDispatcherService
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.netty.EngineMain
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.File

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val yamlObjectMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    val config = environment.config.propertyOrNull("app.configFile")
        ?.getString()
        ?.let { File(it) }
        ?.takeIf { it.exists() }
        ?.let {
            log.info("loading config file $it")
            yamlObjectMapper.readValue(it, Config::class.java)
        } ?: Config()

    log.info("Loaded config with ${config.paperless.size} paperless instances")
    val authService = AuthService(config.authentication)
    val dispatcher = PaperlessDispatcherService(CIO.create(), config.paperless)
    val queue = Channel<DocumentTask>(256)

    install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
        jackson()
    }
    authentication {
        basic(name = "auth") {
            realm = config.authentication.realm
            validate { credentials ->
                authService.authenticate(credentials)
            }
        }
    }
    CoroutineScope(Job())
        .launch {
            queue.consumeEach { evt ->
                dispatcher.getInstance(evt.url)
                    ?.also { service ->
                        service.extractDocumentId(evt.url)
                            .also {
                                if (it == null) {
                                    log.warn("Could not extract document id from ${evt.url}")
                                }
                            }
                            ?.also { id ->
                                log.info("Processing document $id (${evt.url})")
                                try {
                                    service.process(id)
                                } catch (e: Exception) {
                                    log.error("Error processing document $id (${evt.url})", e)
                                    if (evt.attempt < 5) {
                                        queue.send(evt.copy(attempt = evt.attempt + 1))
                                    }
                                }
                            }
                    }
            }
        }

    val authOptional = config.authentication.basic.isEmpty()
    if (authOptional)
        log.warn("No Basic-Auth credentials defined => Authentication is disabled")

    routing {
        authenticate("auth", optional = authOptional) {
            post("/document-added") {
                val body = call.receive<DocumentAddedBody>()
                log.info("Received document added event for ${body.document}")
                val response = dispatcher.getInstance(body.document)
                    .also {
                        if (it == null) {
                            log.warn("Could not find paperless instance for ${body.document}")
                        }
                    }
                    ?.also {
                        log.info("Scheduling processing for document $body.document")
                        queue.send(DocumentTask(body.document))
                    }
                    ?.let { HttpStatusCode.NoContent } ?: HttpStatusCode.BadRequest

                call.respond(response)
            }
        }
        get("/") {
            call.respond(HttpStatusCode.OK, "")
        }
    }
}
