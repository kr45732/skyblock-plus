package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.InvItemStruct;
import com.skyblockplus.utils.structs.PaginatorExtras;
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
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            this.event = event;

            logCommand(event.getGuild(), event.getAuthor(), content);

            if (((args.length == 3) && args[2].startsWith("slot")) || ((args.length == 4) && args[3].startsWith("slot"))) {
                if (args.length == 4) {
                    eb = getPlayerTalismansList(args[1], args[2], args[3]);

                } else {
                    eb = getPlayerTalismansList(args[1], null, args[2]);
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
                    playerEnderChest = getPlayerTalismansEmoji(args[1], args[2]);
                } else {
                    playerEnderChest = getPlayerTalismansEmoji(args[1], null);
                }

                if (playerEnderChest != null) {
                    ebMessage.delete().queue();
                    if (missingEmoji.length() > 0) {
                        ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(missingEmoji).build()).queue();
                    }

                    jda.addEventListener(new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
                } else {
                    ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch data").build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private List<String[]> getPlayerTalismansEmoji(String username, String profileName) {
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

    private EmbedBuilder getPlayerTalismansList(String username, String profileName, String slotNum) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, InvItemStruct> talismanBagMap = player.getTalismanBagMap();
            if (talismanBagMap != null) {
                List<String> pageTitles = new ArrayList<>();
                List<String> pageThumbnails = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1)
                        .setItemsPerPage(1).showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                            try {
                                m.clearReactions().queue();
                            } catch (PermissionException ex) {
                                m.delete().queue();
                            }
                        }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).setColor(botColor)
                        .setUsers(event.getAuthor());

                for (Map.Entry<Integer, InvItemStruct> currentTalisman : talismanBagMap.entrySet()) {
                    InvItemStruct currentTalismanStruct = currentTalisman.getValue();

                    if (currentTalismanStruct == null) {
                        pageTitles.add("Empty");
                        pageThumbnails.add(null);
                        paginateBuilder.addItems("**Slot:** " + (currentTalisman.getKey() + 1));
                    } else {
                        pageTitles.add(currentTalismanStruct.getName() + " x" + currentTalismanStruct.getCount());
                        pageThumbnails.add("https://sky.lea.moe/item.gif/" + currentTalismanStruct.getId());
                        String itemString = "";
                        itemString += "**Slot:** " + (currentTalisman.getKey() + 1);
                        itemString += "\n\n**Lore:**\n" + currentTalismanStruct.getLore();
                        if (currentTalismanStruct.isRecombobulated()) {
                            itemString += "\n(Recombobulated)";
                        }

                        itemString += "\n\n**Item Creation:** " + currentTalismanStruct.getCreationTimestamp();
                        paginateBuilder.addItems(itemString);
                    }
                }
                paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));

                int slotNumber = 1;
                try {
                    slotNumber = Integer.parseInt(slotNum.replace("slot-", ""));
                } catch (Exception ignored) {
                }
                paginateBuilder.build().paginate(event.getChannel(), slotNumber);
                return null;
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
