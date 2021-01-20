package me.ihdeveloper.kotraction.test

import me.ihdeveloper.kotraction.AllowedMentions
import me.ihdeveloper.kotraction.CommandResponse
import me.ihdeveloper.kotraction.CommandResponseType
import me.ihdeveloper.kotraction.DiscordEmbed
import me.ihdeveloper.kotraction.DiscordUser
import me.ihdeveloper.kotraction.SlashCommands
import me.ihdeveloper.kotraction.embedBuilder
import me.ihdeveloper.kotraction.utils.slashCommands

private const val TEST_GUILD_ID = "211543198651121664"

internal fun commands(): SlashCommands {
    return slashCommands {
        guildCommand("hello", TEST_GUILD_ID) {
            description = "Says hello to you :D"

            onInteract = { member, _ ->
                val infoEmbed = embedBuilder {
                    type = DiscordEmbed.Type.RICH

                    description = """
                                An open-source library for processing discord interactions
                                Written in Kotlin
                            """.trimIndent()

                    color = 0

                    fields = arrayOf(
                            DiscordEmbed.Field(
                                    name = "Author",
                                    value = "iHDeveloper#7043",
                                    inline = true,
                            ),
                            DiscordEmbed.Field(
                                    name = "Version",
                                    value = "0.1-dev",
                                    inline = true
                            )
                    )

                    author = DiscordEmbed.Author(
                            name = "Kotraction",
                            url = "https://github.com/iHDeveloper/kotraction",
                            iconUrl = "https://cdn.discordapp.com/avatars/216180750331019264/920d00d68357ea6e5c6dc3142d6fcba3.png?size=64",
                    )

                    footer = DiscordEmbed.Footer(
                            text = "Requested by ${member.user.username}#${member.user.discriminator}",
                            iconUrl = getAvatarUrl(member.user),
                    )
                }

                val anotherEmbed = embedBuilder {
                    type = DiscordEmbed.Type.RICH

                    description = """
                        Slash commands supports multiple embeds \o/
                    """.trimIndent()

                    color = 15797517
                }

                CommandResponse(
                        type = CommandResponseType.MESSAGE,
                        content = "Hello <@${member.user.id}>! o/",
                        allowedMentions = AllowedMentions.Builder(
                                mentionRepliedUser = true
                        ).addUser(member.user.id).build(),
                        embeds = arrayOf(
                                infoEmbed,
                                anotherEmbed
                        )
                )
            }
        }
    }
}

private fun getAvatarUrl(user: DiscordUser, size: Int = 64): String {
    return "https://cdn.discordapp.com/avatars/${user.id}/${user.avatar}.png?size=$size"
}
