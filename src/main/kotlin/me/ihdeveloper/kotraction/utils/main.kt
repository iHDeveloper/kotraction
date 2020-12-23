@file:JvmName("KotractionUtils")

package me.ihdeveloper.kotraction.utils

import me.ihdeveloper.kotraction.SlashCommands

/**
 * Returns slash commands object
 */
fun slashCommands(block: SlashCommands.() -> Unit): SlashCommands {
    val commands = SlashCommands()
    block(commands)
    return commands
}
