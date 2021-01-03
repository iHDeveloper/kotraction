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
                            get("/") {
                                println("[DEBUG] Received a test request! Responding to it now...")
                                call.respond("Hello World!")
                            }
                            post("/") {
                                println("[DEBUG] Received a webhook request! Processing it now...")

                                val timestamp = call.request.headers["X-Signature-Timestamp"]
                                println("[DEBUG] Timestamp: $timestamp")

                                val signature = call.request.headers["X-Signature-Ed25519"]
                                println("[DEBUG] Signature: $signature")

                                val body = call.receiveText()
                                println("[DEBUG] body: $body")

                                if (timestamp != null && signature != null) {
                                    try {
                                        println("[DEBUG] Verifying the webhook...")
                                        val verified = kotraction.verifyInteraction(timestamp, body, signature)
                                        println("[DEBUG] Verification status: $verified")

                                        if (verified) {
                                            println("[DEBUG] Processing the interaction...")
                                            val response = kotraction.processInteraction(body)
                                            println("[DEBUG] Responding to the interaction...")
                                            println("[DEBUG] Response: $response")
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
