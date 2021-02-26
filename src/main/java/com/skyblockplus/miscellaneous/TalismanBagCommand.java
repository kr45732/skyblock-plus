package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.BotUtils.*;

public class TalismanBagCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public TalismanBagCommand(EventWaiter waiter) {
        this.name = "talisman";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");
        this.event = event;

        System.out.println(content);

        if (args.length == 2 || args.length == 3) {
            if (args.length == 3) {
                eb = getPlayerTalismans(args[1], args[2]);

            } else {
                eb = getPlayerTalismans(args[1], null);
            }

            if (eb == null) {
                ebMessage.delete().queue();
                return;
            }
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getPlayerTalismans(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, String> talismanBagMap = player.getTalismanBag();
            if (talismanBagMap != null) {
                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1)
                        .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                        .setCommandUser(event.getAuthor());

                for (Map.Entry<Integer, String> currentTalisman : talismanBagMap.entrySet()) {
                    pageTitles.add("Player talisman bag for " + player.getUsername());
                    paginateBuilder
                            .addItems("**Slot " + (currentTalisman.getKey() + 1) + "**: " + currentTalisman.getValue());
                }
                paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
