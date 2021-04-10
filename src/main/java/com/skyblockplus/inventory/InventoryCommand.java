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
import java.util.List;
import java.util.Map;

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
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");
            this.event = event;

            logCommand(event.getGuild(), event.getAuthor(), content);

            if ((args.length == 3 || args.length == 4) && args[1].equals("armor")) {
                if (args.length == 4) {
                    eb = getPlayerEquippedArmor(args[2], args[3]);
                } else {
                    eb = getPlayerEquippedArmor(args[2], null);
                }

                if (eb == null) {
                    ebMessage.delete().queue();
                } else {
                    ebMessage.editMessage(eb.build()).queue();
                }
                return;
            } else if (((args.length == 3) && args[2].startsWith("slot")) || ((args.length == 4) && args[3].startsWith("slot"))) {
                if (args.length == 4) {
                    eb = getPlayerInventoryList(args[1], args[2], args[3]);

                } else {
                    eb = getPlayerInventoryList(args[1], null, args[2]);
                }

                if (eb == null) {
                    ebMessage.delete().queue();
                } else {
                    ebMessage.editMessage(eb.build()).queue();
                }
                return;
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
                    if (playerInventory[2].length() > 0) {
                        ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(playerInventory[2]).build()).queue();
                    }

                } else {
                    ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable toUnable to fetch player data").build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerInventoryList(String username, String profileName, String slotNum) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, InvItemStruct> inventoryMap = player.getInventoryMap();
            if (inventoryMap != null) {
                List<String> pageTitles = new ArrayList<>();
                List<String> pageThumbnails = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

                for (Map.Entry<Integer, InvItemStruct> currentInvSlot : inventoryMap.entrySet()) {
                    InvItemStruct currentInvStruct = currentInvSlot.getValue();

                    if (currentInvStruct == null) {
                        pageTitles.add("Empty");
                        pageThumbnails.add(null);
                        paginateBuilder.addItems("**Slot:** " + (currentInvSlot.getKey() + 1));
                    } else {
                        pageTitles.add(currentInvStruct.getName() + " x" + currentInvStruct.getCount());
                        pageThumbnails.add("https://sky.lea.moe/item.gif/" + currentInvStruct.getId());
                        String itemString = "";
                        itemString += "**Slot:** " + (currentInvSlot.getKey() + 1);
                        itemString += "\n\n**Lore:**\n" + currentInvStruct.getLore();
                        if (currentInvStruct.isRecombobulated()) {
                            itemString += "\n(Recombobulated)";
                        }

                        itemString += "\n\n**Item Creation:** " + currentInvStruct.getCreationTimestamp();
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

    private EmbedBuilder getPlayerEquippedArmor(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, InvItemStruct> inventoryMap = player.getInventoryArmorMap();
            if (inventoryMap != null) {
                List<String> pageTitles = new ArrayList<>();
                List<String> pageThumbnails = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

                for (Map.Entry<Integer, InvItemStruct> currentInvSlot : inventoryMap.entrySet()) {
                    InvItemStruct currentInvStruct = currentInvSlot.getValue();

                    if (currentInvStruct == null) {
                        pageTitles.add("Empty");
                        pageThumbnails.add(null);

                        String slotName = "";
                        switch ((currentInvSlot.getKey())) {
                            case 4:
                                slotName = "Boots";
                                break;
                            case 3:
                                slotName = "Leggings";
                                break;
                            case 2:
                                slotName = "Chestplate";
                                break;
                            case 1:
                                slotName = "Helmet";
                                break;
                        }

                        paginateBuilder.addItems("**Slot:** " + slotName);
                    } else {
                        pageTitles.add(currentInvStruct.getName() + " x" + currentInvStruct.getCount());
                        pageThumbnails.add("https://sky.lea.moe/item.gif/" + currentInvStruct.getId());
                        String itemString = "";

                        String slotName = "";
                        switch ((currentInvSlot.getKey())) {
                            case 4:
                                slotName = "Boots";
                                break;
                            case 3:
                                slotName = "Leggings";
                                break;
                            case 2:
                                slotName = "Chestplate";
                                break;
                            case 1:
                                slotName = "Helmet";
                                break;
                        }

                        itemString += "**Slot:** " + slotName;
                        itemString += "\n\n**Lore:**\n" + currentInvStruct.getLore();
                        if (currentInvStruct.isRecombobulated()) {
                            itemString += "\n(Recombobulated)";
                        }

                        itemString += "\n\n**Item Creation:** " + currentInvStruct.getCreationTimestamp();
                        paginateBuilder.addItems(itemString);
                    }
                }
                paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles).setThumbnails(pageThumbnails));


                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
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