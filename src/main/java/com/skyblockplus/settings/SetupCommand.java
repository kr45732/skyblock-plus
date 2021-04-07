package com.skyblockplus.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class SetupCommand extends Command {
    private final EventWaiter waiter;

    public SetupCommand(EventWaiter waiter) {
        this.name = "setup";
        this.cooldown = globalCooldown;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

            logCommand(event.getGuild(), event.getAuthor(), BOT_PREFIX + "setup");

            String[] pageTitles = new String[]{"Overview", "Features", "Automatic Verify", "Automatic Apply",
                    "Automatic Roles", "More Help"};

            String overview = "This will walk you through how to setup custom settings for the bot!\n\n**Pages:**\n• Page 2 - Features\n• Page 3 - Automatic Verify\n• Page 4 - Automatic Apply\n• Page 5 - Automatic Roles\n• Page 6 - More Help";

            String features = "**I have many default features including, but not limited to:**\n• Slayer\n• Skills\n• Dungeons\n• Guilds\n• Auction house\n• Much more!\n\nI also have customizable features such as automatic verify, automatic apply, and automatic roles!";

            String verify = "**__Overview__**\n" +
                    "1) When a user runs `link [IGN]` their username is compared with the Hypixel API\n" +
                    "2) If it's valid they will be linked, given the verified role, and their nickname will be updated\n" +
                    "• Example video linked [__here__](https://i.imgur.com/VzQUJrE.mp4)\n\n" +
                    "**__Setup__**\n" +
                    "In order to enable automatic verify, all the following settings must be set:\n" +
                    "- `settings verify message [message]` - the message that users will see and react to in order to verify.\n" +
                    "- `settings verify role [@role]` - the role users will get once verified. This role cannot be @everyone or a managed role.\n" +
                    "- `settings verify channel [#channel]` - the channel where the message to react too will be sent.\n" +
                    "- `settings verify nickname <prefix> [IGN] <postfix>` - the nickname template where IGN will be the players name. Can also be set to none.\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/pibmus)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings verify enable` to enable verify*.\n" +
                    "- For more help type `help settings_verify` or watch the video linked above\n" +
                    "\n" +
                    "*You __must__ run `reload` in order for the changes to take effect";

            String apply = "**__Overview__**\n" +
                    "1) When a user reacts to the apply message, a new channel is created.\n" +
                    "2) Next, they are prompted for their in game Minecraft username.\n" +
                    "3) This is then verified with their linked discord account\n" +
                    "4) If the discord matches, their stats are fetched and returned\n" +
                    "5) The user can then confirm the stats which sends it to staff\n" +
                    "6) The staff role will be pinged with the stats of the player\n" +
                    "7) If accepted, the user will receive the accept message; else if denied, the user will receive the deny message\n" +
                    "• Example video linked [__here__](https://i.imgur.com/9XZTbSz.mp4)\n\n" +
                    "**__Setup__**\n" +
                    "In order to enable automatic verify, all the following settings must be set:\n" +
                    "- `settings apply message [message]` - the message that users will see and react to in order to apply.\n" +
                    "- `settings apply staff_role [@role]` - the staff role that will be pinged when a new application is received. This role cannot be @everyone or a managed role.\n" +
                    "- `settings apply channel [#channel]` - the channel where the message to react to will be sent.\n" +
                    "- `settings apply prefix [prefix]` - the prefix that new apply channels will start with. For example a prefix of apply and a user named CrypticPlasma would result in a new channel called apply-crypticplasma.\n" +
                    "- `settings apply staff_channel [#channel]` - the channel where new applications will be sent to be reviewed by staff.\n" +
                    "- `settings apply accept_message [message]` - message sent if user is accepted.\n" +
                    "- `settings apply deny_message [message]` - message sent if user is denied.\n" +
                    "- `settings apply category [category id]` - the category where new apply channels will be put. In order to get the ID of a category, type `categories` and find the ID of the one you want\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/f5cl7r)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings apply enable` to enable apply*.\n" +
                    "- For more help type `help settings_apply` or watch the video linked above\n" +
                    "\n" +
                    "*You __must__ run `reload` in order for the changes to take effect";

            String roles = "**__Overview__**\n" +
                    "1) When a user runs `roles claim [ign]` their stats are fetched\n" +
                    "2) Depending on the roles setup for this server and the users stats, the corresponding roles will be given\n\n" +
                    "**__Setup__**\n" +
                    "- In order to enable automatic roles, there must be at least one role setting enabled:\n" +
                    "- `settings roles add [roleName] [value] [@role]` - add a level to a role. The role cannot be @everyone or managed.\n" +
                    "- `settings roles remove [roleName] [value]` - remove a level from a role.\n" +
                    "- `settings roles stackable [roleName] [true|false]` - make a role stackable or not stackable.\n" +
                    "- `settings roles enable [roleName]` - enable a role.\n" +
                    "- `settings roles set [roleName] [@role]` - set a one level role's role\n" +
                    "• Tutorial video linked [__here__](https://streamable.com/wninsw)\n\n" +
                    "**__Enable__**\n" +
                    "- Once all these settings are set run `settings roles enable` to enable roles*.\n" +
                    "- To view all the roles, their descriptions, and examples, type `settings roles`" +
                    "- For more help type `help settings_roles` or watch the video linked above\n";

            String moreHelp = "If you need any help, have suggestions, or find any bugs for this bot be sure to join the discord server [here](https://discord.gg/DpcCAwMXwp)!\n" +
                    "You can also view the forums post [here](https://hypixel.net/threads/discord-bot-skyblock-plus-skyblock-focused.3980092/)\n";

            CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

            paginateBuilder.addItems(overview, features, verify, apply, roles, moreHelp);
            paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
            ebMessage.delete().queue();
            paginateBuilder.build().paginate(event.getChannel(), 0);
        }).start();
    }
}