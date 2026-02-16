package de.linkel.model.config

import com.fasterxml.jackson.annotation.JsonProperty

data class Config(
    val authentication: AuthenticationConfig = AuthenticationConfig(),
    val paperless: List<PaperlessConfig> = emptyList()
)

data class AuthenticationConfig(
    val realm: String = "Paperless",
    val basic: List<BasicAuthConfig> = emptyList()
)

data class BasicAuthConfig(
    val username: String,
    val password: String
)

data class PaperlessConfig(
    val url: String,
    val name: String = url,
    val token: String,
    val rules: List<RuleConfig> = emptyList()
)

data class RuleConfig(
    @JsonProperty("when") val conditions: RuleConditions,
    val set: RuleSets,
)

data class RuleConditions(
    val filename: String? = null,
)

data class RuleSets(
    val title: String? = null,
    val created: String? = null,
    val correspondent: String? = null,
    val documentType: String? = null,
    val storagePath: String? = null,
    val tags: List<String> = emptyList(),
    val removeTags: List<String> = emptyList(),
    val custom: Map<String, String> = emptyMap(),
)