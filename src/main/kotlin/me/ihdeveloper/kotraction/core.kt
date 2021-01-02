package me.ihdeveloper.kotraction

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.serialization.responseObject
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

    fun registerCommands() {
        Logger.info("Registering commands for ${slashCommands?.guildCommands?.size} guilds...")

        slashCommands?.guildCommands?.forEach {
            it.value.forEach { command ->
                Logger.info("Registering guild command /${command.name}... (id=${it.key})")

                val body = HTTPRegisterCommand(command.name, command.description)

                val (_, response, result) = Fuel.post("/applications/$applicationId/guilds/${it.key}/commands")
                    .body(Json.encodeToString(body))
                    .responseObject<ApplicationSlashCommand>(json = Json { ignoreUnknownKeys = true })

                result.fold({ applicationSlashCommand ->
                    command.id = applicationSlashCommand.id

                    Logger.info("Successfully! Registered guild command with id=${command.id}")
                }, { fuelError ->
                    Logger.error("Failed! (StatusCode=${response.statusCode}) Registering guild command /${command.name} (id=${it.key})")
                    error(fuelError)
                })
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
