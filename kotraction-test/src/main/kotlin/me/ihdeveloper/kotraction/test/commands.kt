package me.ihdeveloper.kotraction.test

import me.ihdeveloper.kotraction.SlashCommands
import me.ihdeveloper.kotraction.utils.slashCommands

private const val TEST_GUILD_ID = "398407326798970880";

internal fun commands(): SlashCommands {
    return slashCommands {
        guildCommand("hello", TEST_GUILD_ID) {
        }
    }
}
