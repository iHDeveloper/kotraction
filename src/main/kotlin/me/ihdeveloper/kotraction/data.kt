package me.ihdeveloper.kotraction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class HTTPRegisterCommand(
        val name: String,
        val description: String,
        // TODO add options
)

@Serializable
internal data class ApplicationSlashCommand(
        val id: String,
        @SerialName("application_id") val applicationId: String,
        val name: String,
        val description: String,
        // TODO add options
)
