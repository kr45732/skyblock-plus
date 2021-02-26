package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.ArmorStruct;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.BotUtils.*;

public class InventoryCommand extends Command {

    public InventoryCommand() {
        this.name = "inventory";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"inv"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if ((args.length == 3 || args.length == 4) && args[1].equals("armor")) {
            if (args.length == 4) {
                ebMessage.editMessage(getPlayerEquippedArmor(args[2], args[3]).build()).queue();
            } else {
                ebMessage.editMessage(getPlayerEquippedArmor(args[2], null).build()).queue();
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
                ebMessage.getChannel().sendMessage(defaultEmbed("Missing Items").setDescription(playerInventory[2]).build()).queue();
            } else {
                ebMessage.editMessage(defaultEmbed("Error").setDescription("Unable to fetch data").build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
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
