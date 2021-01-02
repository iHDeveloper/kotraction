@file:JvmName("Main")

package me.ihdeveloper.kotraction.test

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.ihdeveloper.kotraction.Kotraction
import java.io.File

@Serializable
internal data class SecretData(
    val id: String,
    val bot: Boolean,
    val token: String,
)

fun main() {
    println("Starting Kotration Test...")

    val secretFile = File("secret.json")

    if (!secretFile.exists())
        error("File [/'secret.json'] doesn't exist")

    val secretData = readSecret(secretFile)

    val kotraction = Kotraction(
            applicationId = secretData.id,
            token = secretData.token,
            bot = secretData.bot,
            slashCommands = commands(),
    )

    println("Registering slash guild commands...")
    kotraction.registerCommands()
}

internal fun readSecret(file: File): SecretData {
    val stream = file.inputStream()
    val serializedData = stream.bufferedReader().use { it.readText() }
    stream.close()
    return Json.decodeFromString(serializedData)
}
