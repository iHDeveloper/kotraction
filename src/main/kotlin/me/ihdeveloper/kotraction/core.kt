package me.ihdeveloper.kotraction

private typealias GuildID = String

class SlashCommands {
    private val guildCommands = mutableMapOf<GuildID, MutableList<GuildCommand>>()

    fun guildCommand(name: String, id: String, block: GuildCommand.() -> Unit) {
        guildCommands.getOrDefault(id, arrayListOf()).run {
            val command = GuildCommand(name, id)
            block(command)
            add(command)
        }
    }
}

abstract class Command(
    val name: String
)

class GuildCommand(
    name: String,
    private val guildId: String,
) : Command(name)
