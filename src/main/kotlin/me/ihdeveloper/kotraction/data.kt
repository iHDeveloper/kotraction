package me.ihdeveloper.kotraction

import kotlinx.serialization.Serializable

@Serializable
internal data class HTTPSlashCommandsRegisterData(
    val name: String,
    val description: String,
)
