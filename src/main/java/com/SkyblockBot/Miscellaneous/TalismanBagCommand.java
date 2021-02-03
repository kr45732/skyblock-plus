package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Utils.CustomPaginator;
import com.SkyblockBot.Utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.SkyblockBot.Utils.BotUtils.*;

public class TalismanBagCommand extends Command {
    final EventWaiter waiter;
    Message ebMessage;
    CommandEvent event;

    public TalismanBagCommand(EventWaiter waiter) {
        this.name = "talisman";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        this.event = event;
        if (args[1].equals("player")) {

            if (args.length == 4) {
                eb = getPlayerTalismans(args[2], args[3]);

            } else
                eb = getPlayerTalismans(args[2], null);

            if (eb == null) {
                ebMessage.delete().queue();
                return;
            }
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerTalismans(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, String> talismanBagMap = player.getTalismanBag();
            if (talismanBagMap != null) {
                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(20).showPageNumbers(true)
                        .useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor).setCommandUser(event.getAuthor());

                for (Map.Entry<Integer, String> currentTalisman : talismanBagMap.entrySet()) {
                    pageTitles.add("Player talisman bag for " + player.getUsername());
                    paginateBuilder.addItems("**Slot " + (currentTalisman.getKey() + 1) + "**: " + currentTalisman.getValue());
                }
                paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
            }
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
