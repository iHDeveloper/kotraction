@file:JvmName("Main")

package me.ihdeveloper.kotraction.test

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import me.ihdeveloper.kotraction.Kotraction
import java.io.File
import kotlin.system.exitProcess

@Serializable
internal data class Settings(
    val id: String,
    val publicKey: String,
    val token: String,
    val bot: Boolean = true,
    val port: Int = 80,
)

fun main(args: Array<String>) {
    println("Starting Kotration Test...")

    val settingsFile = File("settings.json")

    if (!settingsFile.exists())
        error("File [/'settings.json'] doesn't exist")

    val settings = readSettings(settingsFile)

    val kotraction = Kotraction(
            applicationId = settings.id,
            publicKey = settings.publicKey,
            token = settings.token,
            bot = settings.bot,
            slashCommands = commands(),
    )

    kotraction.run {
        println("Fetching slash guild commands...")
        fetchCommands()

        for (arg in args) {
            when (arg.toLowerCase()) {
                "register-commands" -> {
                    println("Registering slash guild commands...")
                    registerCommands()
                }
                "delete-commands" -> {
                    println("Deleting slash guild commands...")
                    deleteCommands()
                }
                "listen" -> {
                    println("Listening on port ${settings.port}...")
                    embeddedServer(Netty, settings.port) {
                        routing {
                            post("/") {
                                val timestamp = call.request.headers["X-Signature-Timestamp"]
                                val signature = call.request.headers["X-Signature-Ed25519"]
                                val body = call.receiveText()

                                if (timestamp != null && signature != null) {
                                    try {
                                        val verified = kotraction.verifyInteraction(timestamp, body, signature)

                                        if (verified) {
                                            val response = kotraction.processInteraction(body)
                                            call.respond(response)
                                        } else {
                                            call.respond(HttpStatusCode.Unauthorized)
                                        }
                                    } catch (exception: Exception) {
                                        call.respond(HttpStatusCode.InternalServerError)
                                        error(exception)
                                    }
                                } else {
                                    call.respond(HttpStatusCode.Unauthorized)
                                }

                                call.respondText("Hello World", ContentType.Text.Plain)
                            }
                        }
                    }.start(wait = true)
                }
                else -> {
                    exitProcess(-1)
                }
            }
        }
    }
}

internal fun readSettings(file: File): Settings {
    val stream = file.inputStream()
    val serializedData = stream.bufferedReader().use { it.readText() }
    stream.close()
    return Json.decodeFromString(serializedData)
}
