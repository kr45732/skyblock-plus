package com.skyblockplus.link;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.api.linkedaccounts.LinkedAccountModel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

public class LinkAccountCommand extends Command {
    CommandEvent event;

    public LinkAccountCommand() {
        this.name = "link";
        this.cooldown = globalCooldown;
    }

    public static EmbedBuilder linkAccount(String username, User user, Guild guild) {
        String[] playerInfo = getPlayerDiscordInfo(username);
        if (playerInfo != null && playerInfo[0] != null) {
            if (!user.getAsTag().equals(playerInfo[0])) {
                EmbedBuilder eb = defaultEmbed("Discord tag mismatch");
                eb.setDescription("Account " + playerInfo[1] + " is linked with the discord tag "
                        + playerInfo[0] + "\nYour current discord tag is " + user.getAsTag());
                return eb;
            }

            LinkedAccountModel toAdd = new LinkedAccountModel("" + Instant.now().toEpochMilli(), user.getId(), playerInfo[2], playerInfo[1]);

            if (database.addLinkedUser(toAdd) == 200) {

                try {
                    if (!higherDepth(database.getVerifySettings(guild.getId()), "verifiedNickname").getAsString().equalsIgnoreCase("none")) {
                        String nicknameTemplate = higherDepth(database.getVerifySettings(guild.getId()), "verifiedNickname").getAsString();
                        nicknameTemplate = nicknameTemplate.replace("[IGN]", playerInfo[1]);
                        guild.getMember(user).modifyNickname(nicknameTemplate).queue();
                    }
                } catch (Exception ignored) {
                }

                try {
                    Role role = guild.getRoleById(higherDepth(database.getVerifySettings(guild.getId()), "verifiedRole").getAsString());
                    guild.addRoleToMember(guild.getMember(user), role).queue();
                } catch (Exception ignored) {
                }

                return defaultEmbed("Success").setDescription("Account " + playerInfo[1] + " linked with " + user.getAsTag());
            } else {
                return defaultEmbed("Error linking " + playerInfo[1]);
            }
        }

        return defaultEmbed("Error finding discord tag linked with account");
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

            if (args.length == 2) {
                ebMessage.editMessage(linkAccount(args[1], event.getAuthor(), event.getGuild()).build()).queue();
                return;
            } else if (args.length == 1) {
                ebMessage.editMessage(getLinkedAccount().build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder getLinkedAccount() {
        JsonElement userInfo = database.getLinkedUserByDiscordId(event.getAuthor().getId());

        if (!userInfo.isJsonNull()) {
            return defaultEmbed("You are linked to " + (higherDepth(userInfo, "minecraftUsername").getAsString()));
        } else {
            return defaultEmbed("You are not linked");
        }
    }
}
