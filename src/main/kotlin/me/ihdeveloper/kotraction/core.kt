package me.ihdeveloper.kotraction

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        }
    }

    fun registerCommands() {
        println("[Kotraction/Engine] Registering commands for ${slashCommands?.guildCommands?.size} guilds...")

        slashCommands?.guildCommands?.forEach {
            it.value.forEach { command ->
                println("[Kotraction/Engine] Registering guild command /${command.name}... (id=${it.key})")

                val body = HTTPSlashCommandsRegisterData(command.name, command.description)

                val (_, response, _) = Fuel.post("/applications/$applicationId/guilds/${it.key}/commands")
                    .header("Authorization", "${if(bot) "Bot" else "Bearer"} $token")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Kotraction (https://github.com/iHDeveloper/kotraction, 0.1-dev)")
                    .body(Json.encodeToString(body))
                    .response()

                val code = response.statusCode
                if (code == 201) {
                    println("[Kotraction/Engine] Successfully! Registered guild command /${command.name} (id=${it.key})")
                } else {
                    println("[Kotraction/Engine] Failed! (StatusCode=${code}) Registering guild command /${command.name} (id=${it.key})")
                    println(response.responseMessage)
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
}

class GuildCommand(
    name: String,
    private val guildId: String,
) : Command(name)
