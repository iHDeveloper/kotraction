package me.ihdeveloper.kotraction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Interaction(
        val id: String,
        val type: InteractionType,
        val data: ApplicationCommandInteractionData?,
        @SerialName("guild_id") val guildId: String,
        @SerialName("channel_id") val channelId: String,
        val token: String,
        val version: Int,
)

@Serializable
internal enum class InteractionType(
        val id: Int,
) {
    PING(1),
    APPLICATION_COMMAND(2),
}

@Serializable
internal data class ApplicationCommandInteractionData(
        val id: String,
        val name: String,
)

@Serializable
internal data class InteractionResponse(
        val type: InteractionResponseType,
        val data: InteractionApplicationCommandCallbackData?,
)

@Serializable
internal enum class InteractionResponseType(
        val id: Int
) {
    PONG(1),
    ACK(2),
    CHANNEL_MESSAGE(3),
    CHANNEL_MESSAGE_WITH_SOURCE(4),
    ACK_WITH_SOURCE(5)
}

@Serializable
internal data class InteractionApplicationCommandCallbackData(
        val content: String,
        val tts: Boolean = false,
)

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
