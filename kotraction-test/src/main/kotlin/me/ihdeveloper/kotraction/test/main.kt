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
internal data class SecretData(
    val id: String,
    val publicKey: String,
    val bot: Boolean,
    val token: String,
)

fun main(args: Array<String>) {
    println("Starting Kotration Test...")

    val secretFile = File("secret.json")

    if (!secretFile.exists())
        error("File [/'secret.json'] doesn't exist")

    val secretData = readSecret(secretFile)

    val kotraction = Kotraction(
            applicationId = secretData.id,
            publicKey = secretData.publicKey,
            token = secretData.token,
            bot = secretData.bot,
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
                "listener" -> {
                    embeddedServer(Netty, 80) {
                        routing {
                            get("/") {
                                val timestamp = call.request.headers["X-Signature-Timestamp"]
                                val signature = call.request.headers["X-Signature-Ed25519"]
                                val body = call.receiveText()

                                if (timestamp != null && signature != null) {
                                    val verified = kotraction.verify(timestamp, body, signature)

                                    if (verified) {
                                        // TODO process the interaction
                                    } else {
                                        call.respond(HttpStatusCode.Unauthorized)
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

internal fun readSecret(file: File): SecretData {
    val stream = file.inputStream()
    val serializedData = stream.bufferedReader().use { it.readText() }
    stream.close()
    return Json.decodeFromString(serializedData)
}
