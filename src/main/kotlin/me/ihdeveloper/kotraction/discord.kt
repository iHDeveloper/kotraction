package me.ihdeveloper.kotraction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscordGuildMember(
        val user: DiscordUser,
        val nick: String?,
        val roles: Array<String>,
        @SerialName("joined_at") val joinedAt: String,
        @SerialName("premium_since") val premiumSince: String?,
        val deaf: Boolean,
        var mute: Boolean,
        val pending: Boolean?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscordGuildMember

        if (user != other.user) return false
        if (nick != other.nick) return false
        if (!roles.contentEquals(other.roles)) return false
        if (joinedAt != other.joinedAt) return false
        if (premiumSince != other.premiumSince) return false
        if (deaf != other.deaf) return false
        if (mute != other.mute) return false
        if (pending != other.pending) return false

        return true
    }

    override fun hashCode(): Int {
        var result = user.hashCode()
        result = 31 * result + (nick?.hashCode() ?: 0)
        result = 31 * result + roles.contentHashCode()
        result = 31 * result + joinedAt.hashCode()
        result = 31 * result + (premiumSince?.hashCode() ?: 0)
        result = 31 * result + deaf.hashCode()
        result = 31 * result + mute.hashCode()
        result = 31 * result + (pending?.hashCode() ?: 0)
        return result
    }
}

@Serializable
data class DiscordUser(
        val id: String,
        val username: String,
        val discriminator: String,
        val avatar: String?,
        val bot: Boolean = false,
        val system: Boolean = false,
        @SerialName("mfa_enabled") val isMFAEnabled: Boolean = false,
        val locale: String? = null,
        val verified: Boolean = false,
        val email: String? = null,
        val flags: Int = 0,
        @SerialName("premium_type") val premiumType: Int = 0,
        @SerialName("public_flags") val publicFlags: Int = 0,
)
