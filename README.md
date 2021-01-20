# Kotraction
Kotlin library for handling discord interactions
 
**This library is under development**

## TODO
- Interactions
- [x] Validating Interaction
- [x] Processing Interaction
- Slash Commands
- [ ] Global Command
- [x] Guild Command
- [ ] Command Options
- [ ] SubCommands
- Slash Commands - Response
- [x] Content
- [x] TTS
- [x] Allowed Mentions
- [ ] Followup (Kotlin Coroutine)
- [x] Embeds 

## Example
Examples of how to use the library 

### Guild Command
A hello guild command for specific guild
```kotlin
internal fun commands(): SlashCommands {
    return slashCommands {
        guildCommand("hello", TEST_GUILD_ID) {
            description = "Says hello to you :D"

            onInteract = { member, _ ->
                CommandResponse(

                        /* Specifies the response type of the command */ 
                        type = CommandResponseType.MESSAGE,

                        /* Set the content of the response */
                        content = "Hello <@${member.user.id}>! o/",

                        /* Specify the allowed mentions (optional) */
                        allowedMentions = AllowedMentions.Builder(
                                mentionRepliedUser = true
                        ).addUser(member.user.id).build(),

                        /* Set a list of embeds to be sent in the message (up to 10) */
                        embeds = arrayOf(embedBuilder {
                            type = DiscordEmbed.Type.RICH

                            description = """
                                This is an embed! \o/
                            """.trimIndent()

                            color = 15797517
                        })
                )
            }
        }
    }
}
```

