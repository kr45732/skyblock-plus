# Skyblock Plus Discord Bot
<a href="https://github.com/kr45732/skyblock-plus/blob/master/LICENSE" target="_blank">
  <img alt="license" src="https://img.shields.io/github/license/kr45732/skyblock-plus?style=for-the-badge" />
</a>
<a href="https://sbplus.codes/support" target="_blank">
  <img alt="license" src="https://img.shields.io/discord/796790757947867156?color=4166f5&label=discord&style=for-the-badge" />
</a> 
<a href="https://github.com/kr45732/skyblock-plus/stargazers" target="_blank">
  <img alt="license" src="https://img.shields.io/github/stars/kr45732/skyblock-plus?style=for-the-badge" />
</a>
<a href="https://github.com/kr45732/skyblock-plus/contributors" target="_blank">
  <img alt="license" src="https://img.shields.io/github/contributors/kr45732/skyblock-plus?style=for-the-badge" />
</a>

This is the source code for the Skyblock Plus Discord bot. A full list of commands and features as well as additional information can be found on the website linked below. This bot was developed over 3 years with a lot of head banging against my wall so place consider dropping a star ❤️

## Self Hosting
### Preface
You may need basic Discord, Gradle, Java, and Postgres knowledge to set up and maintain the bot. There are hardcoded constants that you will need to replace, databases you will need to create, and more. Note that I did not design the bot with the intention of others self-hosting in mind, so the process is not simple nor straightforward.

### Prerequisites
Some of these prerequisites aren't necessary required but having all of them will make setting up bot without modification much easier.
- A Discord Bot (created using the [Discord Developers Portal](https://discord.com/developers/applications); you can find tons of tutorials on creating a bot online) 
- Postgres database (two would be ideal as using one would require you to modify the code; or maybe you can set both database URLs to the same one and see what happens)
- Self-hosted [rust-query-api](https://github.com/kr45732/rust-query-api) (this is required for price calculations as it is used for lowest bin, average bin, average auction, querying the auction house, etc; also used for auction flipper)
- Self-hosted [haste](https://github.com/kr45732/hste) (I know my haste implementation is wacky, so I would suggest replace it with your own (which will require some code modification) or you can try to self-host mine)
  - Hosting my haste steps (good luck!):
    - Clone my wacky [hastebin](https://github.com/kr45732/hste)
    - Get a [fauna database]([hste](https://github.com/kr45732/hste))
    - Set FAUNA_ADMIN_KEY environment variable (might be called secret)
    - Set FAUNA_DB_DOMAIN environment variable (might be called endpoint)
    - Set KEY environment variable to your choosing and set "HASTE_KEY" in DevSettings.properties for the bot to the same value
- GitHub [personal token](https://github.com/settings/tokens) (give it the repo scope)
  - This is used to automatically update the [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data) repo, which you should clone and have your own. This is most definitely required and will require additional set up later
  - This is also used to authenticate the GitHub raw endpoints (though I'm not sure if that's even necessary)
- Some place soft to bang your head against; a wall is fine too, I won't judge

### Steps
If you made it this far, good job & good luck! If you are stuck or encounter a problem, create an issue and I will try to help you in 3-5 business days!
1. Clone this repository
2. Rename ExampleDevSettings.properties to DevSettings.properties and move it to the project root (or use environment variables) and follow the comments in there and the guide below to fill it out
   - If you don't plan on using linked roles, you can probably leave CLIENT_SECRET blank (haven't tested)
   - Feel free to set the API_USERNAME and API_PASSWORD to some gibberish. Just don't leave it blank because you don't want some random person to access the private endpoints (server settings, linked accounts, etc)
   - Same thing as above with JACOB_KEY, you probably won't ever use that endpoint, but you don't want some random person to be able to POST data using it
   - Make sure the AUCTION_API_KEY is the same as the ADMIN_API_KEY of your self-hosted rust-query-api
   - If you set up my wacky hastebin, then HASTE_KEY should be the same as the KEY set on there
   - AUCTION_FLIPPER_WEBHOOK isn't required but who does not want a very ~~not~~ accurate auction flipper
   - BOT_STATUS_WEBHOOK might be required but for your sake just make it
3. Databases (yikes!). So the database situation is not ideal. For very not so smart reasons, there are two databases instead of a single one. You might be able to combine it into one without needing to modify code by setting both databases to the same URL and praying (haven't tested). Refer setup-resources for the schemas and database dumps. You will also need the [pg_trgm module](https://www.postgresql.org/docs/current/pgtrgm.html). 
   - Server settings schema should be automatically generated by Spring (thank god for that because the schema is horrendous). If you have to manually create the schema, use the database dump in setup-resources.
   - Linked accounts schema is quite nice actually
   - Leaderboard schemas (another yikes!). I designed leaderboards in a very interesting (and inefficient) way. There are 4 leaderboards: "all_lb", "ironman_lb", "stranded_lb", and "selected_lb". The first 3 are self-explanatory. The last one stores players' last played profile and is a cache used for automatic nickname updates, automatic roles claim, etc. All four of these have identical schemas.
   - Cache schemas are also not that bad. There is `guild` (used for tracking guilds for "/serverlb"), `json_cache` (used to cache json responses from the Hypixel API), and `json_storage` (persistent cache)
4. Emojis (yikes again!). So right now there are ~90 emoji servers. If you want to use the emojis, you will have to make your own servers and emojis.
   - Delete all emojis from "IdToEmojiMappings.json"
   - Create around 90 servers with the name "Skyblock Plus - Emoji Server xx" where xx is the server number starting at 1 and invite the bot to it
   - Copy the glint_images from [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data) to "src/main/java/com/skyblockplus/json/glint_images/"
   - Optional step & might break in the future: copy the CIT folder from the latest furfsky reborn to "src/main/java/com/skyblockplus/json/cit" 
   - Set `DEV = true` and run the bot
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.processAll()`
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.runEmojis(json)` where json is a haste link to the output JSON you got from processAll() above
   - Paste the JSON output of runEmojis into the "IdToEmojiMappings.json"
5. Hardcoded constants you will need to change:
   - You will need to update all emoji maps in the Constants.json from [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data/blob/main/Constants.json) with new emojis
   - When I say "update guild" below, the guild ID you are setting it to is **your** primary server
   - In `com.skyblockplus.utils.utils.Utils` update:
     - botLogChannel guild and channel assignment on L206 and L228 (this is where command uses are logged to)
     - networthBugReportChannel guild and channel assignment on L372
   - Create 2 messages in a channel to be used for the scuffed event system. Update the assignment of messageId in `com.skyblockplus.features.event.EventHandler` on L46. Update the guild and channel on L56 and L57
   - Update guild in L203 of `com.skyblockplus.features.listeners.AutomaticGuild` and channel in L205 (this is the channel where the BOT_STATUS_WEBHOOK is in)
   - Update guild in L1180 of `com.skyblockplus.features.listeners.AutomaticGuild`
   - Update errorLogChannel in L39, L63, and L94 of `com.skyblockplus.utils.utils.Utils.errorLogChannel` to a channel for error logs
   - Update guild in L48 com.skyblockplus.features.listeners.AutomaticGuild
   - More will be added as I find/remember them
6. Everything is finally set up (I hope) and you're ready to run the bot (yay!)
   - Use gradle or gradelw to build: `gradle build`
   - It will create a jar: "build/libs/SkyblockPlus-0.0.1.jar"
   - Run the jar using Java 17. I suggest using systemctl to keep it running in the background (see sample in setup-resources/skyblock-plus.service)

## Bug reports
Feel free to make an issue or report the bug using the support server linked below.

## Contributing
Contributions are greatly appreciated. Feel free to make a pull request!

## Contact
You can contact me through the Discord server linked below.

## Useful links
- Website: https://sbplus.codes
- Invite: https://sbplus.codes/invite
- Support: https://sbplus.codes/support
