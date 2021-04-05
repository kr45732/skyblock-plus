package com.skyblockplus.miscellaneous;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class ProfilesCommand extends Command {
    private final EventWaiter waiter;
    private CommandEvent event;

    public ProfilesCommand(EventWaiter waiter) {
        this.waiter = waiter;
        this.name = "profiles";
        this.cooldown = globalCooldown;
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

            if (args.length == 2){
                ebMessage.editMessage(getPlayerProfiles(args[1]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getPlayerProfiles(String username) {
        UsernameUuidStruct usernameUuidStruct =  usernameToUuid(username);
        if (usernameUuidStruct != null) {
            try {
                JsonArray profileArray = higherDepth(
                        getJson("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuidStruct.playerUuid), "profiles").getAsJsonArray();

                CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(1).setItemsPerPage(1);

                for(JsonElement profile:profileArray){

                }
            } catch (Exception ignored) {
            }
        }

        return defaultEmbed("Unable to fetch player data");
    }
}
