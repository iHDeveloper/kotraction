package me.ihdeveloper.kotraction

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.serialization.responseObject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.ihdeveloper.kotraction.utils.Logger

private typealias GuildID = String

internal const val DISCORD_ENDPOINT = "https://discord.com/api/v8"

class Kotraction(
        private val applicationId: String,
        private val token: String,
        private val slashCommands: SlashCommands? = null,
        private val bot: Boolean = false
) {
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
            }, { _ ->
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
}

class GuildCommand(
    name: String,
    private val guildId: String,
) : Command(name)
