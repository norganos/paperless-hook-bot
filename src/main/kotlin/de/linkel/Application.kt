package de.linkel

import de.linkel.model.config.Config
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.ktor.serialization.jackson.*
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.linkel.model.api.DocumentAddedBody
import de.linkel.model.api.DocumentQuery
import de.linkel.service.AuthService
import de.linkel.service.QueueService
import de.linkel.service.PaperlessRegistryService
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
    val registry = PaperlessRegistryService(CIO.create(), config.paperless)
    val queue = QueueService(registry)

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
            queue.loop()
        }

    val authOptional = config.authentication.basic.isEmpty()
    if (authOptional)
        log.warn("No Basic-Auth credentials defined => Authentication is disabled")

    routing {
        authenticate("auth", optional = authOptional) {
            post("/document-added") {
                val body = call.receive<DocumentAddedBody>()
                log.info("Received document added event for ${body.document}")
                val response = registry.getInstanceForUrl(body.document)
                    .also {
                        if (it == null) {
                            log.warn("Could not find paperless instance for ${body.document}")
                        }
                    }
                    ?.let { service ->
                        service.extractDocumentId(body.document)
                            .also {
                                if (it == null) {
                                    log.warn("Could not extract document id from ${body.document}")
                                }
                            }
                            ?.also {
                                queue.schedule(service, it)
                            }
                    }
                    ?.let { HttpStatusCode.NoContent } ?: HttpStatusCode.BadRequest

                call.respond(response)
            }
            post("/documents/{instance}") {
                val response = call.parameters["instance"]
                    .also {
                        log.info("Received document query event for $it")
                    }
                    ?.let { name ->
                        registry.getInstanceByName(name)
                            .also {
                                if (it == null) {
                                    log.warn("Could not find paperless instance named $name")
                                }
                            }
                    }
                    ?.let { it to call.receive<DocumentQuery>() }
                    ?.also { (service, query) ->
                        queue.schedule(service, query)
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
