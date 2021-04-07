package com.skyblockplus.inventory;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.ArmorStruct;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.PaginatorExtras;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

public class WardrobeCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;
    private String missingEmoji;

    public WardrobeCommand(EventWaiter waiter) {
        this.name = "wardrobe";
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

            if ((args.length == 3 || args.length == 4 ) && args[1].equals("list")) {
                if (args.length == 4) {
                    eb = getPlayerWardrobeList(args[2], args[3]);
                } else {
                    eb = getPlayerWardrobeList(args[2], null);
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
                    playerEnderChest = getPlayerWardrobe(args[1], args[2]);
                } else {
                    playerEnderChest = getPlayerWardrobe(args[1], null);
                }

                if (playerEnderChest != null) {
                    ebMessage.delete().queue();
                    if (missingEmoji.length() > 0) {
                        ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(missingEmoji).build()).queue();
                    }

                    jda.addEventListener(new InventoryPaginator(playerEnderChest, ebMessage.getChannel(), event.getAuthor()));
                } else {
                    ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable toUnable to fetch player data").build()).queue();
                }
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private List<String[]> getPlayerWardrobe(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            List<String[]> talismanBagPages = player.getWardrobe();

            if (talismanBagPages != null) {
                this.missingEmoji = player.invMissing;
                return talismanBagPages;
            }
        }
        return null;
    }

    private EmbedBuilder getPlayerWardrobeList(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<Integer, ArmorStruct> armorStructMap = player.getWardrobeList();
            if (armorStructMap != null) {
                ArrayList<String> pageTitles = new ArrayList<>();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(4);

                for (Map.Entry<Integer, ArmorStruct> currentArmour : armorStructMap.entrySet()) {
                    pageTitles.add(player.getUsername());
                    paginateBuilder.addItems("**__Slot " + (currentArmour.getKey() + 1) + "__**\n"
                            + currentArmour.getValue().getHelmet() + "\n" + currentArmour.getValue().getChestplate()
                            + "\n" + currentArmour.getValue().getLeggings() + "\n" + currentArmour.getValue().getBoots()
                            + "\n");
                }
                paginateBuilder.setPaginatorExtras(new PaginatorExtras().setTitles(pageTitles));
                paginateBuilder.build().paginate(event.getChannel(), 0);
                return null;
            }
        }
        return defaultEmbed("Unable to fetch player data");
    }
}
