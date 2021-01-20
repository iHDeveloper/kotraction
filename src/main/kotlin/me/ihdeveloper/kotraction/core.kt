package me.ihdeveloper.kotraction

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.serialization.responseObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.ihdeveloper.kotraction.utils.Logger
import org.abstractj.kalium.encoders.Encoder
import org.abstractj.kalium.keys.VerifyKey

private typealias GuildID = String

internal const val DISCORD_ENDPOINT = "https://discord.com/api/v8"

private val CUSTOM_JSON = Json {
    ignoreUnknownKeys = true
} 

private val ACK_RESPONSE = InteractionResponse(
        type = InteractionResponseType.ACK,
        data = null
)

private val ACK_WITH_SOURCE_RESPONSE = InteractionResponse(
        type = InteractionResponseType.ACK_WITH_SOURCE,
        data = null
)

class Kotraction(
        private val applicationId: String,
        publicKey: String,
        private val token: String,
        private val slashCommands: SlashCommands? = null,
        private val bot: Boolean = false
) {
    private val verifyKey = VerifyKey(publicKey, Encoder.HEX)

    private var isFetched: Boolean = false

    init {
        FuelManager.instance.run {
            basePath = DISCORD_ENDPOINT
            baseHeaders = mapOf(
                    "Content-Type" to "application/json",
                    "User-Agent" to "Kotraction (https://github.com/iHDeveloper/kotraction, 0.1-dev)",
                    "Authorization" to "${if(bot) "Bot" else "Bearer"} $token",
            )
        }
    }

    fun processInteraction(body: String): String {
        val interaction = CUSTOM_JSON.decodeFromString<Interaction>(body)

        /* Ignore any unknown interactions */
        if (interaction.version != 1) {
            error("Unknown Interaction version. (version=${interaction.version})")
        }

        return when(interaction.type) {
            InteractionType.PING -> { /* Responds to the ping interaction */
                Logger.info("Received PING from discord! Sending PONG message...")
                CUSTOM_JSON.encodeToString(InteractionResponse(
                        type = InteractionResponseType.PONG,
                        data = null
                ))
            }
            InteractionType.APPLICATION_COMMAND -> { /* Responds to the slash command interaction */
                CUSTOM_JSON.encodeToString(onCommandInteraction(interaction))
            }
        }
    }

    fun verifyInteraction(timestamp: String, body: String, signature: String): Boolean {
        return try {
            val encodedPayload = (timestamp + body).toByteArray(Charsets.UTF_8)
            val encodedSignature = Encoder.HEX.decode(signature)

            verifyKey.verify(encodedPayload, encodedSignature)
        } catch (_: Exception) {
            false
        }
    }

    fun fetchCommands() {
        Logger.info("Fetching commands for ${slashCommands?.guildCommands?.size} guilds...")

        slashCommands?.guildCommands?.forEach { id, commands ->
            Logger.info("Fetching guild commands for id=$id")

            val (_, response, result) = Fuel.get("/applications/$applicationId/guilds/$id/commands")
                    .responseObject(ListSerializer(ApplicationSlashCommand.serializer()), json = Json { ignoreUnknownKeys = true })

            result.fold({ rawCommands ->
                rawCommands.forEach {
                    for (command in commands) {
                        if (command.name != it.name)
                            continue

                        command.id = it.id
                        Logger.info("Loaded! Guild command /${command.name} (id=${command.id})")
                    }
                }
            }, {
                Logger.warning("Failed! (StatusCode=${response.statusCode}). It's recommend to register the command!")
            })
        }

        isFetched = true
    }

    fun registerCommands() {
        Logger.info("Registering commands for ${slashCommands?.guildCommands?.size} guilds...")

        slashCommands?.guildCommands?.forEach {
            it.value.forEach { command ->
                Logger.info("Registering guild command /${command.name}... (id=${it.key})")

                val body = HTTPRegisterCommand(command.name, command.description)

                val (_, response, result) = Fuel.post("/applications/$applicationId/guilds/${it.key}/commands")
                    .body(CUSTOM_JSON.encodeToString(body))
                    .responseObject<ApplicationSlashCommand>(json = Json { ignoreUnknownKeys = true })

                val code = response.statusCode

                result.fold({ rawCommand ->
                    command.id = rawCommand.id

                    if (code == 201)
                        Logger.info("Successfully! Registered guild command with id=${command.id}")
                    else if (code == 200)
                        Logger.info("Guild command is already registered! with id=${command.id}")
                }, { fuelError ->
                    Logger.error("Failed! (StatusCode=${response.statusCode}) while registering guild command /${command.name} (id=${it.key})")
                    error(fuelError)
                })
            }
        }
    }

    fun deleteCommands() {
        if (!isFetched)
            error("Failed to fetch commands! Commands data needs to be fetched first!")

        Logger.info("Deleting commands for ${slashCommands?.guildCommands?.size} guilds...")
        slashCommands?.guildCommands?.forEach { id, commands ->
            Logger.info("Deleting guild commands for id=$id")

            commands.forEach {
                Logger.info("Deleting guild command /${it.name} (id=${it.id})")

                val (_, response, _) = Fuel.delete("/applications/$applicationId/guilds/$id/commands/${it.id}")
                    .response()

                val code = response.statusCode
                if (code == 204) {
                    Logger.info("Guild command has been successfully deleted!")
                } else {
                    Logger.warning("Failed to delete the command! (StatusCode=${code}")
                }
            }
        }
    }

    private fun onCommandInteraction(interaction: Interaction): InteractionResponse {
        val commands = slashCommands?.guildCommands?.get(interaction.guildId) ?: return ACK_RESPONSE

        if (interaction.data == null) {
            Logger.warning("Received interaction without data! Responding ACK message...")
            return ACK_RESPONSE
        }

        if (interaction.guildId == null) {
            Logger.warning("Received interaction without guild id! Responding ACK message...")
            return ACK_RESPONSE
        }

        if (interaction.channelId == null) {
            Logger.warning("Received interaction without channel id! Responding ACK message...")
            return ACK_RESPONSE
        }

        if (interaction.member == null) {
            Logger.warning("Received interaction without guild member! Responding ACK message...")
            return ACK_RESPONSE
        }

        Logger.info("Invoking interaction with command /${interaction.data.name}... (id=${interaction.data.id})")

        for (command in commands) {
            if (command.id != interaction.data.id)
                continue

            val response = command.onInteract(interaction.member, interaction.guildId, interaction.channelId)

            return when (response.type) {
                CommandResponseType.ACK -> {
                    Logger.info("Responding with ACK message!")
                    ACK_RESPONSE
                }
                CommandResponseType.ACK_WITH_SOURCE -> {
                    Logger.info("Responding with ACK (with-source) message!")
                    ACK_WITH_SOURCE_RESPONSE
                }
                CommandResponseType.MESSAGE -> InteractionResponse(
                        type = InteractionResponseType.CHANNEL_MESSAGE,
                        data = InteractionApplicationCommandCallbackData(
                                content = response.content ?: "",
                                tts = response.tts,
                                allowedMentions = response.allowedMentions,
                                embeds = response.embeds,
                        )
                )
                CommandResponseType.MESSAGE_WITH_SOURCE -> InteractionResponse(
                        type = InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                        data = InteractionApplicationCommandCallbackData(
                                content = response.content ?: "",
                                tts = response.tts,
                                allowedMentions = response.allowedMentions,
                                embeds = response.embeds,
                        )
                )
            }
        }

        Logger.warning("Command not found to invoke the interaction! Responding with ACK message...")
        return ACK_RESPONSE
    }
}

class SlashCommands {
    internal val guildCommands = mutableMapOf<GuildID, MutableList<GuildCommand>>()

    fun guildCommand(name: String, id: String, block: GuildCommand.() -> Unit) {
        val commands = guildCommands[id] ?: arrayListOf()

        commands.run {
            val command = GuildCommand(name, id)
            block(command)
            add(command)
        }

        guildCommands[id] = commands
    }
}

abstract class Command(
    val name: String,
) {
    var description: String = ""

    internal lateinit var id: String

    abstract fun onInteract(member: DiscordGuildMember, guildId: String, channelId: String): CommandResponse
}

data class CommandResponse(
        val type: CommandResponseType,
        val content: String? = null,
        val tts: Boolean = false,
        val allowedMentions: AllowedMentions? = null,
        val embeds: Array<DiscordEmbed>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandResponse

        if (type != other.type) return false
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
        var result = type.hashCode()
        result = 31 * result + (content?.hashCode() ?: 0)
        result = 31 * result + tts.hashCode()
        result = 31 * result + (allowedMentions?.hashCode() ?: 0)
        result = 31 * result + (embeds?.contentHashCode() ?: 0)
        return result
    }
}

enum class CommandResponseType {
    ACK,
    ACK_WITH_SOURCE,
    MESSAGE,
    MESSAGE_WITH_SOURCE;
}

@Serializable
data class AllowedMentions(
        val parse: Array<String>,
        val users: Array<String>,
        val roles: Array<String>,
        @SerialName("replied_user") private val repliedUser: Boolean,
) {
    class Builder(
            private val isEveryoneAllowed: Boolean = false,
            private val isUsersAllowed: Boolean = false,
            private val isRolesAllowed: Boolean = false,
            private val mentionRepliedUser: Boolean = false,
    ) {
        private val users = arrayListOf<String>()
        private val roles = arrayListOf<String>()

        fun addUser(user: String): Builder {
            users.add(user)
            return this
        }

        fun addRole(role: String): Builder {
            roles.add(role)
            return this
        }

        fun build(): AllowedMentions {
            val rawParse = arrayListOf<String>()

            if (isEveryoneAllowed)
                rawParse.add("everyone")
            if (isUsersAllowed)
                rawParse.add("users")
            if (isRolesAllowed)
                rawParse.add("roles")

            return AllowedMentions(
                    parse = rawParse.toTypedArray(),
                    users = users.toTypedArray(),
                    roles = roles.toTypedArray(),
                    repliedUser = mentionRepliedUser
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AllowedMentions

        if (!parse.contentEquals(other.parse)) return false
        if (!users.contentEquals(other.users)) return false
        if (!roles.contentEquals(other.roles)) return false
        if (repliedUser != other.repliedUser) return false

        return true
    }

    override fun hashCode(): Int {
        var result = parse.contentHashCode()
        result = 31 * result + users.contentHashCode()
        result = 31 * result + roles.contentHashCode()
        result = 31 * result + repliedUser.hashCode()
        return result
    }
}

class GuildCommand(
    name: String,
    val guildId: String,
) : Command(name) {
    var onInteract: ((member: DiscordGuildMember, channelId: String) -> CommandResponse)? = null

    override fun onInteract(member: DiscordGuildMember, guildId: String, channelId: String): CommandResponse {
        if (onInteract == null) {
            return CommandResponse(
                    type = CommandResponseType.ACK
            )
        }

        return onInteract!!.invoke(member, channelId)
    }
}
