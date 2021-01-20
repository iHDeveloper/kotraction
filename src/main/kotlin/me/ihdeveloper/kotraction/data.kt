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
        val embeds: Array<DiscordEmbed>?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InteractionApplicationCommandCallbackData

        if (content != other.content) return false
        if (tts != other.tts) return false
        if (allowedMentions != other.allowedMentions) return false
        if (embeds != null) {
            if (other.embeds == null) return false
            if (!embeds.contentEquals(other.embeds)) return false
        } else if (other.embeds != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + tts.hashCode()
        result = 31 * result + (allowedMentions?.hashCode() ?: 0)
        result = 31 * result + (embeds?.contentHashCode() ?: 0)
        return result
    }
}

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
