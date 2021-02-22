package com.skyblockplus.miscellaneous;

import static com.skyblockplus.utils.BotUtils.defaultEmbed;
import static com.skyblockplus.utils.BotUtils.errorMessage;
import static com.skyblockplus.utils.BotUtils.globalCooldown;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.ArmorStruct;
import com.skyblockplus.utils.Player;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class InventoryCommand extends Command {

    public InventoryCommand() {
        this.name = "inventory";
        this.cooldown = globalCooldown;
        this.aliases = new String[] { "inv" };
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String content = event.getMessage().getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(content);

        if (args[1].equals("player")) {

            if (args.length == 4) {
                eb = getPlayerEquippedArmor(args[2], args[3]);

            } else
                eb = getPlayerEquippedArmor(args[2], null);

        } else {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
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
}
