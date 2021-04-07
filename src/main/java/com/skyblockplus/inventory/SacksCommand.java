package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.PaginatorExtras;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.Map;

import static com.skyblockplus.utils.Utils.*;

public class SacksCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public SacksCommand(EventWaiter waiter) {
        this.name = "sacks";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            this.event = event;

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (args.length == 2 || args.length == 3) {
                if (args.length == 3) {
                    eb = getPlayerSacks(args[1], args[2]);

                } else
                    eb = getPlayerSacks(args[1], null);

                if (eb == null) {
                    ebMessage.delete().queue();
                } else {
                    ebMessage.editMessage(eb.build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerSacks(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<String, Integer> sacksMap = player.getPlayerSacks();
            if (sacksMap != null) {
                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(20);

                for (Map.Entry<String, Integer> currentSack : sacksMap.entrySet()) {
                    pageTitles.add("Player sacks content for " + player.getUsername());
                    paginateBuilder
                            .addItems("**" + convertSkyblockIdName(currentSack.getKey())
                                    + "**: " + currentSack.getValue());
                }
                paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
