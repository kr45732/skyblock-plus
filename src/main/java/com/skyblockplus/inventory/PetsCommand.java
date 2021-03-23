package com.skyblockplus.inventory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class PetsCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public PetsCommand(EventWaiter waiter) {
        this.name = "pets";
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
    }

    private EmbedBuilder getPlayerSacks(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            ArrayList<String> pageTitles = new ArrayList<>();

            CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(2)
                    .setItemsPerPage(20).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                        try {
                            m.clearReactions().queue();
                        } catch (PermissionException ex) {
                            m.delete().queue();
                        }
                    }).setEventWaiter(waiter).setTimeout(15, TimeUnit.SECONDS).setColor(botColor)
                    .setCommandUser(event.getAuthor());

            JsonArray playerPets = player.getPets();
            for (JsonElement pet : playerPets) {
                pageTitles.add("Pets for " + player.getUsername());
                String petItem = null;
                try {
                    petItem = higherDepth(pet, "heldItem").getAsString().replace("PET_ITEM_", "").replace("_", " ").toLowerCase();
                    if (petItem.endsWith(" common")) {
                        petItem = "common " + petItem.replace(" common", "");
                    } else if (petItem.endsWith(" uncommon")) {
                        petItem = "uncommon " + petItem.replace(" uncommon", "");
                    } else if (petItem.endsWith(" rare")) {
                        petItem = "rare " + petItem.replace(" rare", "");
                    } else if (petItem.endsWith(" epic")) {
                        petItem = "epic " + petItem.replace(" epic", "");
                    } else if (petItem.endsWith(" legendary")) {
                        petItem = "legendary " + petItem.replace(" legendary", "");
                    }
                } catch (Exception ignored) {
                }

                paginateBuilder.addItems(
                        "**" + capitalizeString(higherDepth(pet, "type").getAsString().toLowerCase().replace("_", " ")) + " (" + player.petLevelFromXp(higherDepth(pet, "exp").getAsLong(), higherDepth(pet, "tier").getAsString()) + ")**" +
                                "\nTier: " + higherDepth(pet, "tier").getAsString().toLowerCase() +
                                (petItem != null ? "\nItem: " + petItem : ""));

            }
            paginateBuilder.setPageTitles(pageTitles.toArray(new String[0]));
            paginateBuilder.build().paginate(event.getChannel(), 0);
            return null;
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
