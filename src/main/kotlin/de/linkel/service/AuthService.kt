package de.linkel.service

import de.linkel.model.config.AuthenticationConfig
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.UserPasswordCredential

class AuthService(
    private val authentication: AuthenticationConfig
) {
    fun authenticate(credentials: UserPasswordCredential): UserIdPrincipal? {
        return authentication.basic
            .firstOrNull { it.username == credentials.name && it.password == credentials.password }
            ?.let { UserIdPrincipal(it.username) }
    }
}