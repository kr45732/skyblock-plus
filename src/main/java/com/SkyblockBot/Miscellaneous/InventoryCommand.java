package com.SkyblockBot.Miscellaneous;

import com.SkyblockBot.Utils.ArmorStruct;
import com.SkyblockBot.Utils.Player;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.SkyblockBot.Utils.BotUtils.*;

public class InventoryCommand extends Command {
    Message ebMessage;
    final EventWaiter waiter;
    CommandEvent event;

    public InventoryCommand(EventWaiter waiter) {
        this.name = "inventory";
        this.guildOnly = false;
        this.cooldown = globalCooldown;
        this.waiter = waiter;
        this.aliases = new String[]{"inv"};
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading player data...", null);
        this.ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        Message message = event.getMessage();
        String content = message.getContentRaw();

        String[] args = content.split(" ");
        if (args.length <= 2 || args.length > 4) {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        for (String value : args) {
            System.out.print(value + " ");
        }
        System.out.println();

        this.event = event;
        if (args[1].equals("player")) {

            if (args.length == 4) {
                eb = getPlayerEquippedArmor(args[2], args[3]);

            } else
                eb = getPlayerEquippedArmor(args[2], null);

            if (eb == null) {
                ebMessage.delete().queue();
                return;
            }
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    public EmbedBuilder getPlayerEquippedArmor(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            ArmorStruct inventoryArmor = player.getInventoryArmor();
            if (inventoryArmor != null) {
                EmbedBuilder eb = defaultEmbed("Equipped armor for " + player.getUsername(), null);
                eb.addField("Equipped", inventoryArmor.getHelmet() + "\n" + inventoryArmor.getChestplate() + "\n" + inventoryArmor.getLeggings() + "\n" + inventoryArmor.getBoots(), false);
                return eb;
            }
        }
        return defaultEmbed("Unable to fetch player data", null);
    }
}
