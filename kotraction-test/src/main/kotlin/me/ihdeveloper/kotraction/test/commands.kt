package me.ihdeveloper.kotraction.test

import me.ihdeveloper.kotraction.CommandResponse
import me.ihdeveloper.kotraction.CommandResponseType
import me.ihdeveloper.kotraction.SlashCommands
import me.ihdeveloper.kotraction.utils.slashCommands

private const val TEST_GUILD_ID = "211543198651121664"

internal fun commands(): SlashCommands {
    return slashCommands {
        guildCommand("hello", TEST_GUILD_ID) {
            description = "Prints hello world message"

            onInteract = {
                CommandResponse(
                        type = CommandResponseType.MESSAGE,
                        content = "Hello World! o/ <#$it>"
                )
            }
        }
    }
}
