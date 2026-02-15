package de.linkel

import de.linkel.model.config.Config
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.linkel.model.api.DocumentAddedBody
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
    val scope = CoroutineScope(Job())

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
                    ?.also { service ->
                        service.extractDocumentId(body.document)
                            .also {
                                if (it == null) {
                                    log.warn("Could not extract document id from ${body.document}")
                                }
                            }
                            ?.also { id ->
                                log.info("Scheduling processing for document $id")
                                scope.launch { service.process(id) }
                            }
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
