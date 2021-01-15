package me.ihdeveloper.kotraction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class Interaction(
        val id: String,
        val token: String,
        val version: Int,
        val data: ApplicationCommandInteractionData? = null,
        val member: DiscordGuildMember? = null,
        @SerialName("guild_id") val guildId: String? = null,
        @SerialName("channel_id") val channelId: String? = null,
        @SerialName("type") private val _type: Int,
) {
    val type: InteractionType
        get() {
            for (t in InteractionType.values()) {
                if (t.id == _type)
                    return t
            }
            return InteractionType.PING
        }
}

@Serializable
internal enum class InteractionType(
        val id: Int,
) {
    PING(1),
    APPLICATION_COMMAND(2);
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
internal enum class InteractionResponseType {
    @SerialName("1") PONG,
    @SerialName("2") ACK,
    @SerialName("3") CHANNEL_MESSAGE,
    @SerialName("4") CHANNEL_MESSAGE_WITH_SOURCE,
    @SerialName("5") ACK_WITH_SOURCE;
}

@Serializable
internal data class InteractionApplicationCommandCallbackData(
        val content: String,
        val tts: Boolean,
        @SerialName("allowed_mentions") val allowedMentions: AllowedMentions?,
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
