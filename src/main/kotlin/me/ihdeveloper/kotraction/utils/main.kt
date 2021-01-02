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

internal object Logger {

    fun info(message: String) = print("INFO", message)

    fun warning(message: String) = print("WARN", message)

    fun error(message: String) = print("ERR", message)

    private fun print(prefix: String, message: String) = println("[Kotraction/Engine] [$prefix] $message")
}
