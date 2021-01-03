package me.ihdeveloper.kotraction

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.serialization.responseObject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.ihdeveloper.kotraction.utils.Logger
import org.abstractj.kalium.encoders.Encoder
import org.abstractj.kalium.keys.VerifyKey

private typealias GuildID = String

internal const val DISCORD_ENDPOINT = "https://discord.com/api/v8"

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
        val interaction = Json.decodeFromString<Interaction>(body)

        /* Ignore any unknown interactions */
        if (interaction.version != 1) {
            error("Unknown Interaction version. (version=${interaction.version})")
        }

        return when(interaction.type) {
            InteractionType.PING -> { /* Responds to the ping interaction */
                Logger.info("Received PING from discord! Sending PONG message...")
                Json.encodeToString(InteractionResponse(
                        type = InteractionResponseType.PONG,
                        data = null
                ))
            }
            InteractionType.APPLICATION_COMMAND -> { /* Responds to the slash command interaction */
                Json.encodeToString(onCommandInteraction(interaction))
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
                    .body(Json.encodeToString(body))
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

        Logger.info("Invoking interaction with command /${interaction.data.name}... (id=${interaction.data.id})")

        for (command in commands) {
            if (command.id != interaction.data.id)
                continue

            val response = command.onInteraction(interaction.guildId, interaction.channelId)

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
                        data = InteractionApplicationCommandCallbackData(content = response.content ?: "")
                )
                CommandResponseType.MESSAGE_WITH_SOURCE -> InteractionResponse(
                        type = InteractionResponseType.CHANNEL_MESSAGE_WITH_SOURCE,
                        data = InteractionApplicationCommandCallbackData(content = response.content ?: "")
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

    abstract fun onInteraction(guildId: String, channelId: String): CommandResponse
}

data class CommandResponse(
        val type: CommandResponseType,
        val content: String? = null,
)

enum class CommandResponseType {
    ACK,
    ACK_WITH_SOURCE,
    MESSAGE,
    MESSAGE_WITH_SOURCE;
}

class GuildCommand(
    name: String,
    val guildId: String,
) : Command(name) {
    var onInteraction: ((channelId: String) -> CommandResponse)? = null

    override fun onInteraction(guildId: String, channelId: String): CommandResponse {
        if (onInteraction == null) {
            return CommandResponse(
                    type = CommandResponseType.ACK
            )
        }

        return onInteraction!!.invoke(channelId)
    }
}
