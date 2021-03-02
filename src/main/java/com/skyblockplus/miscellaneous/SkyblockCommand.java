package com.skyblockplus.miscellaneous;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.ArmorStruct;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.skyblockplus.utils.Utils.*;

public class SkyblockCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public SkyblockCommand(EventWaiter waiter) {
        this.name = "skyblock";
        this.cooldown = globalCooldown;
        this.waiter = waiter;
    }

    @Override
    protected void execute(CommandEvent event) {
        this.event = event;
        EmbedBuilder eb = defaultEmbed("Loading...");
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
        String content = event.getMessage().getContentRaw();
        String[] args = content.split(" ");

        System.out.println(content);

        if (args.length == 2 || args.length == 3) {
            if (args.length == 3) {
                eb = getSkyblockStats(args[1], args[2]);
            } else {
                eb = getSkyblockStats(args[1], null);
            }

            if (eb == null) {
                ebMessage.delete().queue();
            } else {
                ebMessage.editMessage(eb.build()).queue();
            }
            return;
        }

        ebMessage.editMessage(errorMessage(this.name).build()).queue();
    }

    private EmbedBuilder getSkyblockStats(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            JsonElement profileJson = player.getProfileJson();
            JsonElement statsJson = higherDepth(player.getProfileJson(), "stats");
            JsonElement jacobJson = higherDepth(player.getProfileJson(), "jacob2");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.US)
                    .withZone(ZoneId.systemDefault());

            // General
            String generalPageString = "";
            generalPageString += "**First Joined:** "
                    + dateFormatter.format(Instant.ofEpochMilli(higherDepth(profileJson, "first_join").getAsLong()))
                    + "\n";
            generalPageString += "**Purse:** " + simplifyNumber(player.getPurseCoins()) + "\n";
            generalPageString += "**Bank Account:** " + (player.getBankBalance() == -1.0 ? "Banking API disabled"
                    : simplifyNumber(player.getBankBalance())) + "\n";
            generalPageString += "**Skill Average:** " + roundSkillAverage(player.getSkillAverage()) + "\n";
            generalPageString += "**Fairy Souls:** " + player.getFairySouls() + " / 220" + "\n";

            // Armor
            String armorPageString = "";
            ArmorStruct invArmor = player.getInventoryArmor();
            armorPageString += "**Equipped Helmet:** " + invArmor.getHelmet() + "\n";
            armorPageString += "**Equipped Chestplate:** " + invArmor.getChestplate() + "\n";
            armorPageString += "**Equipped Leggings:** " + invArmor.getLeggings() + "\n";
            armorPageString += "**Equipped Boots:** " + invArmor.getBoots() + "\n";
            armorPageString += "**Wardrobe:** Use `" + BOT_PREFIX + "wardrobe` to view wardrobe armor" + "\n";

            // Skills
            String skillsPageString = "";
            skillsPageString += "__**Farming**__" + "\n";
            skillsPageString += "• **Contests Attended:** " + getJsonKeys(higherDepth(jacobJson, "contests")).size()
                    + "\n";
            skillsPageString += "• **Unique Golds:** " + higherDepth(jacobJson, "unique_golds2").getAsJsonArray().size()
                    + "\n";
            skillsPageString += "• **Current Golds:** "
                    + (higherDepth(higherDepth(jacobJson, "medals_inv"), "gold") == null ? 0
                    : higherDepth(higherDepth(jacobJson, "medals_inv"), "gold").getAsInt())
                    + "\n";
            skillsPageString += "• **Current Silvers:** "
                    + (higherDepth(higherDepth(jacobJson, "medals_inv"), "silver") == null ? 0
                    : higherDepth(higherDepth(jacobJson, "medals_inv"), "silver").getAsInt())
                    + "\n";
            skillsPageString += "• **Current Bronzes:** "
                    + (higherDepth(higherDepth(jacobJson, "medals_inv"), "bronze") == null ? 0
                    : higherDepth(higherDepth(jacobJson, "medals_inv"), "bronze").getAsInt())
                    + "\n";
            skillsPageString += "\n";

            skillsPageString += "__**Fishing**__" + "\n";
            skillsPageString += "• **Items Fished:** " + formatNumber(higherDepth(statsJson, "items_fished").getAsInt())
                    + "\n";
            skillsPageString += "• **Treasures Fished:** "
                    + formatNumber(higherDepth(statsJson, "items_fished_treasure").getAsInt()) + "\n";
            skillsPageString += "• **Large Treasures Fished:** "
                    + formatNumber(higherDepth(statsJson, "items_fished_large_treasure").getAsInt()) + "\n";
            skillsPageString += "• **Fished With Shredder:** "
                    + formatNumber(higherDepth(statsJson, "shredder_fished").getAsInt()) + "\n";

            // Dungeons
            String dungeonsPageString = "";
            dungeonsPageString += "__**Classes**__" + "\n";
            dungeonsPageString += "• **Healer:** " + roundSkillAverage(player.getDungeonClassLevel("healer")) + "\n";
            dungeonsPageString += "• **Mage:** " + roundSkillAverage(player.getDungeonClassLevel("mage")) + "\n";
            dungeonsPageString += "• **Berserk:** " + roundSkillAverage(player.getDungeonClassLevel("berserk")) + "\n";
            dungeonsPageString += "• **Archer:** " + roundSkillAverage(player.getDungeonClassLevel("archer")) + "\n";
            dungeonsPageString += "• **Tank:** " + roundSkillAverage(player.getDungeonClassLevel("tank")) + "\n";
            dungeonsPageString += "\n";

            dungeonsPageString += "__**Catacombs**__" + "\n";
            dungeonsPageString += "• **Catacombs:** " + roundSkillAverage(player.getCatacombsLevel()) + "\n";

            String[] pageTitles = {"General", "Armor", "Skills", "Dungeons"};
            CustomPaginator.Builder paginateBuilder = new CustomPaginator.Builder().setColumns(1).setItemsPerPage(1)
                    .showPageNumbers(true).useNumberedItems(false).setFinalAction(m -> {
                        try {
                            m.clearReactions().queue();

                        } catch (PermissionException ex) {
                            m.delete().queue();
                        }
                    }).setEventWaiter(waiter).setTimeout(30, TimeUnit.SECONDS).wrapPageEnds(true).setColor(botColor)
                    .setPageTitles(pageTitles).setCommandUser(event.getAuthor());
            paginateBuilder.addItems(generalPageString, armorPageString, skillsPageString, dungeonsPageString);
            paginateBuilder.build().paginate(event.getChannel(), 0);

            return null;
        }
        return defaultEmbed("Invalid username or profile");
    }
}