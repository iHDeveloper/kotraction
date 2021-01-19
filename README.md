# Kotraction
Kotlin library for handling discord interactions
 
**This library is under development**

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
                        ).addUser(member.user.id).build()
                )
            }
        }
    }
}
```

