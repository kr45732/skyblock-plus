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
You may need basic Discord, Gradle, Java, and Postgres knowledge to set up and maintain the bot. There are hardcoded constants that you will need to replace, databases you will need to create, and more. Note that I did not design the bot with the intention of others self-hosting it, so the process may not be straightforward. Please give me credit if you are self-hosting the bot!

### Prerequisites
Some of these prerequisites are not necessarily required but having all of them will make setting up bot without modification much easier.
- A Discord Bot (created using the [Discord Developers Portal](https://discord.com/developers/applications)). Under Discord Developer Portal → Application:
  - Under Installation → Authorization Methods, check "Guild Install"
  - Under Installation → Install Link, set it to "Discord Provided Link"
  - Under Installation → Default Install Settings → Guild Install, add "applications.commands" & "bot" scopes
  - Under Installation → Default Install Settings → Guild Install, add these recommended permissions: `Add Reactions, Attach Files, Connect Create, Instant Invite, Create Private Threads, Create Public Threads, Embed Links, Manage Channels, Manage Messages, Manage Nicknames, Manage Roles, Manage Threads, Read Message History, Read Messages/View Channels, Send Messages, Send Messages in Threads, Use External Emojis`
  - Under Bot → Authorization Flow, enable "Public Bot" if you want others to be able to invite the bot to their server
  - Under Bot → Privileged Gateway Intents, enable "Server Members Intent" & "Message Content Intent"  
- 2 Postgres databases (using one might be possible but may require code modifications)
- Self-hosted [rust-query-api](https://github.com/kr45732/rust-query-api) (used in lowest bin, average bin, average auction, querying the auction house, auction flipper, etc)
- Self-hosted hastebin (my haste implementation is wacky, so I would suggest to replace it with your own (which will require some code modification) otherwise self-hosting steps are below)
  1. Clone [hste](https://github.com/kr45732/hste)
  2. Obtain a [fauna database]([hste](https://github.com/kr45732/hste))
  3. Set FAUNA_ADMIN_KEY environment variable (might be called secret)
  4. Set FAUNA_DB_DOMAIN environment variable (might be called endpoint)
  5. Set KEY environment variable to your choosing
- GitHub [personal token](https://github.com/settings/tokens) with the repo scope
  - This is used to automatically update the [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data) repo, which you should clone your own of

### Steps
If you are stuck or encounter a problem in the steps below, create an issue and I will try to help you in 3-5 business days!
1. Clone this repository
2. Rename DevSettingsTemplate.properties to DevSettings.properties and move it to the project root and follow the instructions there and below to fill it out:
   - Feel free to set the API_USERNAME and API_PASSWORD to some gibberish, but do not leave it blank because you do not want anyone to be able to access the private endpoints (server settings, linked accounts, etc)
   - Same thing as above with JACOB_KEY, you will probably never use that endpoint, but you do not want anyone to be able to POST data to it
   - Ensure the AUCTION_API_KEY is the same as the ADMIN_API_KEY of your self-hosted rust-query-api
   - If you self-hosted [hste](https://github.com/kr45732/hste), then HASTE_KEY should be the same as the KEY set on there
3. Databases: At the moment, there are two databases instead of a single one. You might be able to combine it into one without needing to modify code by setting both databases to the same URL, but I have not tested that. Refer to [setup-resources](https://github.com/kr45732/skyblock-plus/tree/master/setup-resources) for schemas and database dumps. You will also need to install the [pg_trgm module](https://www.postgresql.org/docs/current/pgtrgm.html) on your databases. This can be done by executing `CREATE EXTENSION pg_trgm` when connected to your database  
   - [Server Settings Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#server-settings-schema)
   - [Linked Accounts Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#linked-accounts-schema)
   - [Leaderboards Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#leaderboards-schema)
   - [Cache Schema](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/schemas.md#cache-schema)
4. Emojis: At the moment, there are over 90 custom emoji servers. To create your own:
   - Make "IdToEmojiMappings.json" an empty JSON object
   - Create and add the bot to 90 servers with the name "Skyblock Plus - Emoji Server xx" (xx is the server number starting at 1)
   - Optionally (may not work in the future), copy the CIT folder from the latest FurfSky Reborn to "src/main/java/com/skyblockplus/json/cit" 
   - Set `DEV = true` and run the bot
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.processAll()`
   - Using the evaluate command run `ev com.skyblockplus.utils.EmojiUpdater.runEmojis(json)` where json is a haste link to the output JSON you got from processAll() above
   - Paste the JSON output of runEmojis into the "IdToEmojiMappings.json"
5. Data repository: you will need to make your own clone of the skyblock-plus-data repository so your bot can automatically update it:
   - Fork or clone [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data)
   - Set "DATA_REPO_GITHUB" in DevSettings to point to your repository **on GitHub**
   - Create a new channel in your PRIMARY_GUILD and set "NEU_REPO_UPDATE_CHANNEL_ID" in DevSettings
   - Join [Moulberry's Bush](https://discord.gg/moulberry) and follow "#item-repo-github" (send it to the channel you just created)
6. Hardcoded constants you will need to change:
   - You will need to update all emoji maps in the Constants.json from [skyblock-plus-data](https://github.com/kr45732/skyblock-plus-data/blob/main/Constants.json) with your own emojis
   - Update L145 of `com.skyblockplus.Main` to `.enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)`
   - Create 2 messages in a channel to be used for the scuffed event system (they will be constantly edited, so they must not be deleted). These messages Update the assignment of messageId in `com.skyblockplus.features.event.EventHandler` on L46. Update the channel on L57
   - More will be added as I find/remember them
7. Running the bot:
   - Build using gradle or gradlew (`gradle build`) to create a jar at "build/libs/SkyblockPlus-0.0.1.jar"
   - Run the jar using Java 17 (example using systemctl to run it in the background [here](https://github.com/kr45732/skyblock-plus/blob/master/setup-resources/skyblock-plus.service))
   - To register slash commands, run the `d-slash global` prefix command
   - If you choose to set up linked roles, then you will need a domain (localhost will not work):
     - Set Discord Developer Portal → Application → General Information → Linked Roles Verification URL to "ht<span>tps://</span>verify.[DOMAIN]/"
     - Set Discord Developer Portal → Application → OAuth2 → Redirects to "ht<span>tps://</span>verify.[BASE_URL]/callback"
     - Run the `d-linked-roles` prefix command to register the linked roles

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
