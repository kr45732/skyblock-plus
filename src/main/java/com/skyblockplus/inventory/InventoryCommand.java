package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.ArmorStruct;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class InventoryCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public InventoryCommand(EventWaiter waiter) {
        this.name = "inventory";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"inv"};
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");
        this.event = event;

        System.out.println(content);

        if ((args.length == 3 || args.length == 4) && args[1].equals("armor")) {
            if (args.length == 4) {
                ebMessage.editMessage(getPlayerEquippedArmor(args[2], args[3]).build()).queue();
            } else {
                ebMessage.editMessage(getPlayerEquippedArmor(args[2], null).build()).queue();
            }
            return;
//        } else if ((args.length == 4 || args.length == 5) && args[1].equals("slot")) {
//            if (args.length == 5) {
//                eb = getInventorySlot(args[2], args[3], args[4]);
//            } else {
//                eb = getInventorySlot(args[2], args[3], null);
//            }
//            if (eb == null) {
//                ebMessage.delete().queue();
//            } else {
//                ebMessage.editMessage(eb.build()).queue();
//            }
//            return;
        } else if (args.length == 2 || args.length == 3) {
            String[] playerInventory;
            if (args.length == 3) {
                playerInventory = getPlayerInventory(args[1], args[2]);
            } else {
                playerInventory = getPlayerInventory(args[1], null);
            }

            if (playerInventory != null) {
                ebMessage.delete().queue();
                ebMessage.getChannel().sendMessage(playerInventory[0]).queue();
                ebMessage.getChannel().sendMessage(playerInventory[1]).queue();
                ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(playerInventory[2]).build()).queue();
            } else {
                ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch data").build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getInventorySlot(String itemName, String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, String[]> itemArr = player.getInventoryItem(itemName);

            List<String> pages = new ArrayList<>();
            List<String> pageTitles = new ArrayList<>();

            for (Map.Entry<Integer, String[]> item : itemArr.entrySet()) {
                pageTitles.add("[" + item.getValue()[0] + " " + itemName + "] Slot " + item.getKey());
                pages.add(item.getValue()[1]);
            }

            CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                    .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                        try {
                            m.clearReactions().queue();
                        } catch (PermissionException ex) {
                            m.delete().queue();
                        }
                    }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                    .setPageTitles(pageTitles.toArray(new String[0])).setCommandUser(event.getAuthor());
            paginateBuilder.addItems(pages.toArray(new String[0]));
            paginateBuilder.build().paginate(event.getChannel(), 0);
            return null;
        }
        return defaultEmbed("Unable to fetch player data");
    }

    private EmbedBuilder getPlayerEquippedArmor(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            ArmorStruct inventoryArmor = player.getInventoryArmor();
            if (inventoryArmor != null) {
                EmbedBuilder eb = defaultEmbed("Equipped armor for " + player.getUsername());
                eb.addField("Equipped", inventoryArmor.getHelmet() + "\n" + inventoryArmor.getChestplate() + "\n"
                        + inventoryArmor.getLeggings() + "\n" + inventoryArmor.getBoots(), false);
                return eb;
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }

    private String[] getPlayerInventory(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            String[] temp = player.getInventory();
            if (temp != null) {
                return new String[]{
                        temp[0], temp[1], player.invMissing
                };
            }
        }
        return null;
    }
}
