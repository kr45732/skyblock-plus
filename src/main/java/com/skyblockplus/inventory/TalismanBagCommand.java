package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class TalismanBagCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;
    private String missingEmoji;

    public TalismanBagCommand(EventWaiter waiter) {
        this.name = "talisman";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");
        this.event = event;

        logCommand(event.getGuild(), event.getAuthor(), content);

        if ((args.length == 3 || args.length == 4) && args[1].equals("list")) {
            if (args.length == 4) {
                eb = getPlayerTalismans(args[2], args[3]);

            } else {
                eb = getPlayerTalismans(args[2], null);
            }

            if (eb == null) {
                ebMessage.delete().queue();
            } else {
                ebMessage.editMessage(eb.build()).queue();
            }
            return;
        } else if (args.length == 2 || args.length == 3) {
            List<String[]> playerEnderChest;
            if (args.length == 3) {
                playerEnderChest = getPLayerTalismansEmoji(args[1], args[2]);
            } else {
                playerEnderChest = getPLayerTalismansEmoji(args[1], null);
            }

            if (playerEnderChest != null) {
                ebMessage.delete().queue();
                if(missingEmoji.length() > 0) {
                    ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(missingEmoji).build()).queue();
                }

                jda.addEventListener(new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
            } else {
                ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch data").build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private List<String[]> getPLayerTalismansEmoji(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            List<String[]> talismanBagPages = player.getTalismanBag();
            if (talismanBagPages != null) {
                this.missingEmoji = player.invMissing;
                return talismanBagPages;
            }
        }
        return null;
    }

    private EmbedBuilder getPlayerTalismans(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, String> talismanBagMap = player.getTalismanBagMap();
            if (talismanBagMap != null) {
                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1)
                        .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).setColor(botColor)
                        .setCommandUser(event.getAuthor());

                for (Map.Entry<Integer, String> currentTalisman : talismanBagMap.entrySet()) {
                    pageTitles.add("Talisman bag for " + player.getUsername());
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
