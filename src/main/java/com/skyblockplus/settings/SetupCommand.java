package com.skyblockplus.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.concurrent.TimeUnit;

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
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();

        System.out.println(content);

        String[] pageTitles = new String[]{"Overview", "Features", "Automatic Verify", "Automatic Apply",
                "Automatic Roles", " Guild Rank Helper", "More Help"};

        String overview = "This will walk you through how to setup custom settings for the bot!\n\n**Pages:**\n• Page 2 - Features\n• Page 3 - Automatic Verify\n• Page 4 - Automatic Apply\n• Page 5 - Automatic Roles\n• Page 6 - Automatic guild rank helper\n• Page 7 - More Help";

        String features = "**I have many default features including, but not limited to:**\n• Slayer\n• Skills\n• Dungeons\n• Guilds\n• Auction house\n• Much more!\n\nI also have customizable features such as automatic verify, automatic apply, automatic roles, and automatic guild rank helper!";

        String verify = "**Automated verify overview**:\n1) When a user reacts to the verify message, a new channel is created.\n2) They are then prompted for their in game minecraft username.\n3) This is then compared with their linked discord account and if matching they will receive the verified role and the channel will be deleted.\n• NOTE: By default, automatic verify is disabled. In order to enable automatic verify you must specify the following settings.\n\n- `"
                + BOT_PREFIX
                + "settings verify message [message]` - the message that users will see and react to in order to verify.\n- `"
                + BOT_PREFIX
                + "settings verify role [@role]` - the role users will get once verified. This role cannot be @everyone or a managed role.\n- `"
                + BOT_PREFIX
                + "settings verify channel [#channel]` - the channel where the message to react to will be sent.\n- `"
                + BOT_PREFIX
                + "settings verify prefix [prefix]` - the prefix that new verify channels will start with. For example a prefix of __verify__ and a user named __CrypticPlasma__ would result in a new channel called __verify-crypticplasma__.\n- `"
                + BOT_PREFIX
                + "settings verify category [category id]` - the category where new verify channels will be put. In order to get the ID of a category, type `"
                + BOT_PREFIX
                + "categories` and find the ID of the one you want\n\n- Once all these settings are set run `"
                + BOT_PREFIX + "settings verify enable` to enable verify.\n- For more help type `" + BOT_PREFIX
                + "help settings_verify`\n**- IMPORTANT: you __must__ run `" + BOT_PREFIX
                + "reload` in order for the changes to take effect**";

        String apply = "**Automated apply overview**:\n1) When a user reacts to the apply message, a new channel is created.\n2) Next, they are prompted for their in game minecraft username.\n3) This is then verified with their linked discord account\n4) If the discord matches, their stats are fetched and returned\n5) The user can then confirm the stats which sends it to staff\n6) The staff role will be pinged with the stats of the player\n 7) If accepted, the user will receive the accept message; else if denied, the user will receive the deny message.\n• NOTE: By default, automatic apply is disabled. In order to enable automatic apply you must specify the following settings.\n\n- `"
                + BOT_PREFIX
                + "settings apply message [message]` - the message that users will see and react to in order to apply.\n- `"
                + BOT_PREFIX
                + "settings apply staff_role [@role]` - the staff role that will be pinged when a new application is received. This role cannot be @everyone or a managed role.\n- `"
                + BOT_PREFIX
                + "settings apply channel [#channel]` - the channel where the message to react to will be sent.\n- `"
                + BOT_PREFIX
                + "settings apply prefix [prefix]` - the prefix that new apply channels will start with. For example a prefix of __apply__ and a user named __CrypticPlasma__ would result in a new channel called __apply-crypticplasma__.\n- `"
                + BOT_PREFIX
                + "settings apply staff_channel` - the channel where new applications will be sent to be reviewed by staff.\n- `"
                + BOT_PREFIX + "apply accept_message [message]` - message sent if user is accepted.\n- `" + BOT_PREFIX
                + "apply deny_message [message]` - message sent if user is denied.\n- `" + BOT_PREFIX
                + "settings apply category [category id]` - the category where new apply channels will be put. In order to get the ID of a category, type `"
                + BOT_PREFIX
                + "categories` and find the ID of the one you want\n\n- Once all these settings are set run `"
                + BOT_PREFIX + "settings apply enable` to enable automatic apply.\n- For more help type `" + BOT_PREFIX
                + "help settings_apply`\n**- IMPORTANT: you __must__ run `" + BOT_PREFIX
                + "reload` in order for the changes to take effect**";

        String roles = "**Automated roles overview**:\n1) When a user runs `" + BOT_PREFIX
                + "roles claim [ign]` their stats are fetched\n2) Depending on the roles setup for this server and the users stats, the corresponding roles will be given\n• NOTE: By default, automatic roes is disabled. In order to enable there must be __at least__ one role configuration enabled\n\n- `"
                + BOT_PREFIX
                + "settings roles add [roleName] [value] [@role]` - add a level to a role. The role cannot be @everyone or managed.\n- `"
                + BOT_PREFIX + "settings roles remove [roleName] [value]` - remove a level from a role.\n- `"
                + BOT_PREFIX
                + "settings roles stackable [roleName] [true|false]` - make a role stackable or not stackable.\n- `"
                + BOT_PREFIX
                + "settings roles enable [roleName]` - enable a role.\n\n- Once __at least__ one role is enabled run `"
                + BOT_PREFIX + "settings roles enable` to enable automatic roles.\n- For more help type `" + BOT_PREFIX
                + "help settings_roles`";

        String guildRankHelper = "To be added";

        String moreHelp = "If you need any help, have suggestions, or find any bugs for this bot be sure to join the discord server [here](https://discord.gg/DpcCAwMXwp)!";

        CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                    try {
                        m.clearReactions().queue();
                    } catch (PermissionException ex) {
                        m.delete().queue();
                    }
                }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                .setCommandUser(event.getAuthor());

        paginateBuilder.addItems(overview, features, verify, apply, roles, guildRankHelper, moreHelp);
        paginateBuilder.setPageTitles(pageTitles);
        ebMessage.delete().queue();
        paginateBuilder.build().paginate(event.getChannel(), 0);
    }
}