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

This is the source code for the Skyblock Plus Discord bot. A full list of commands and features as well as additional information can be found on the website linked below. This bot was developed over 3 years with a lot of head banging against my wall so please consider dropping a star ❤️

## Self Hosting
### Preface
You may need basic Discord, Gradle, Java, and Postgres knowledge to set up and maintain the bot. There are hardcoded constants that you will need to replace, databases you will need to create, and more. Note that I did not design the bot with the intention of others self-hosting it, so the process isn't very straightforward.

### Prerequisites
Some of these prerequisites aren't necessary required but having all of them will make setting up bot without modification much easier.
- A Discord Bot (created using the [Discord Developers Portal](https://discord.com/developers/applications)). You will also need to enable the server members and message content intents found in Discord Developer Portal --> Bot --> Privileged Gateway Intents
- 2 Postgres databases (using one might be possible but may require code modifications)
- Self-hosted [rust-query-api](https://github.com/kr45732/rust-query-api) (used in lowest bin, average bin, average auction, querying the auction house, auction flipper, etc)
- Self-hosted hastebin (my haste implementation is wacky, so I would suggest replace it with your own (which will require some code modification) otherwise self-hosting steps are below)
  - Clone [hste](https://github.com/kr45732/hste)
  - Obtain a [fauna database]([hste](https://github.com/kr45732/hste))
  - Set FAUNA_ADMIN_KEY environment variable (might be called secret)
  - Set FAUNA_DB_DOMAIN environment variable (might be called endpoint)
  - Set KEY environment variable to your choosing
- GitHub [personal token](https://github.com/settings/tokens) with the repo scope
  - This is used to automatically update the [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data) repo, which you should clone your own of

### Steps
If you are stuck or encounter a problem in the steps below, create an issue and I will try to help you in 3-5 business days!
1. Clone this repository
2. Rename ExampleDevSettings.properties to DevSettings.properties and move it to the project root (or use environment variables) and follow the instructions there and below to fill it out:
   - If you don't plan on using linked roles, you can probably leave CLIENT_SECRET blank (haven't tested)
   - Feel free to set the API_USERNAME and API_PASSWORD to some gibberish, but don't leave it blank because you don't want anyone to be able to access the private endpoints (server settings, linked accounts, etc)
   - Same thing as above with JACOB_KEY, you probably won't ever use that endpoint, but you don't want anyone to be able to POST data to it
   - Ensure the AUCTION_API_KEY is the same as the ADMIN_API_KEY of your self-hosted rust-query-api
   - If you are self-hosted [hste](https://github.com/kr45732/hste), then HASTE_KEY should be the same as the KEY set on there
3. Databases: At the moment, there are two databases instead of a single one. You might be able to combine it into one without needing to modify code by setting both databases to the same URL, but I haven't tested that. Refer to [setup-resources](https://github.com/kr45732/skyblock-plus/tree/master/setup-resources) for schemas and database dumps. You will also need to install the [pg_trgm module](https://www.postgresql.org/docs/current/pgtrgm.html) on your databases. 
   - [Server Settings Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#server-settings-schema)
   - [Linked Accounts Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#linked-accounts-schema)
   - [Leaderboards Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#leaderboards-schema)
   - [Cache Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#cache-schema)
4. Emojis: At the moment, there are around 90 custom emoji servers. To create your own:
   - Make "IdToEmojiMappings.json" an empty JSON object
   - Create and add the bot to 90 servers with the name "Skyblock Plus - Emoji Server xx" (xx is the server number starting at 1)
   - Copy all glint_images from [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data) to "src/main/java/com/skyblockplus/json/glint_images/"
   - Optionally (may not work in the future), copy the CIT folder from the latest FurfSky Reborn to "src/main/java/com/skyblockplus/json/cit" 
   - Set `DEV = true` and run the bot
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.processAll()`
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.runEmojis(json)` where json is a haste link to the output JSON you got from processAll() above
   - Paste the JSON output of runEmojis into the "IdToEmojiMappings.json"
5. Data repository: you will need to make your own clone of the skyblock-plus-data repository so your bot can automatically update it:
   - Clone [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data)
   - Create a new channel in your PRIMARY_GUILD and set "NEU_REPO_UPDATE_CHANNEL_ID" in DevSettings.properties
   - Join [Moulberry's Bush](https://discord.gg/moulberry)
   - Follow "#item-repo-github" and send it to the channel you just created
6. Hardcoded constants you will need to change:
   - You will need to update all emoji maps in the Constants.json from [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data/blob/main/Constants.json) with your own emojis
   - Create 2 messages in a channel to be used for the scuffed event system (they will be constantly edited, so they must not be deleted). These messages Update the assignment of messageId in `com.skyblockplus.features.event.EventHandler` on L46. Update the channel on L57
   - More will be added as I find/remember them
7. Running the bot:
   - Build using gradle or gradlew: `gradle build` to create a jar at "build/libs/SkyblockPlus-0.0.1.jar"
   - Run the jar using Java 17 (example using systemctl to run it in the background [here](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/skyblock-plus.service))
   - Register slash commands using the `d-slash` prefix command

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
