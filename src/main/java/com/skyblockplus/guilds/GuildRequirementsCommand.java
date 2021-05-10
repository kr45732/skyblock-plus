package com.skyblockplus.guilds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.discordserversettings.automatedapplication.ApplyRequirements;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;
import static com.skyblockplus.utils.Utils.errorMessage;

public class GuildRequirementsCommand extends Command {

    public GuildRequirementsCommand() {
        this.name = "guild-requirements";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"g-reqs", "guild-reqs"};
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();

            logCommand(event.getGuild(), event.getAuthor(), content);

            JsonArray guildReqs = null;
            try{
                guildReqs = database.getApplyReqs(event.getGuild().getId()).getAsJsonArray();
            }catch (Exception ignored){
            }

            if(guildReqs == null || guildReqs.size() == 0){
                ebMessage.editMessage(defaultEmbed("Error").setDescription("No requirements set for this server").build()).queue();
                return;
            }

            eb = defaultEmbed("Guild Requirements");
            for(JsonElement req:guildReqs){
                eb.addField("Requirement", "Slayer: " + formatNumber(higherDepth(req, "slayerReq").getAsInt()) + "\nSkill Average: " + formatNumber(higherDepth(req, "skillsReq").getAsInt()) + "\nCatacombs: " + formatNumber(higherDepth(req, "catacombsReq").getAsInt()) + "\nWeight: " + formatNumber(higherDepth(req, "weightReq").getAsInt()),false);
            }

            ebMessage.editMessage(eb.build()).queue();
        }).start();
    }
}
