package com.skyblockplus.eventlisteners.apply;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.ArrayList;
import java.util.List;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.MainClassUtils.getApplyGuildUsersCache;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

public class ApplyGuild {
    public List<ApplyUser> applyUserList = new ArrayList<>();
    public Message reactMessage;
    public JsonElement currentSettings;
    public boolean enable = true;

    public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.currentSettings = currentSettings;
        this.applyUserList = getApplyGuildUsersCache(reactMessage.getGuild().getId(),
                higherDepth(currentSettings, "name").getAsString());
    }

    public ApplyGuild() {
        this.enable = false;
    }

    public int applyUserListSize() {
        return applyUserList.size();
    }

    public List<ApplyUser> getApplyUserList() {
        return applyUserList;
    }

    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (!enable) {
            return;
        }

        if (onMessageReactionAdd_NewApplyUser(event)) {
            return;
        }

        onMessageReactionAdd_ExistingApplyUser(event);
    }

    public boolean onMessageReactionAdd_ExistingApplyUser(MessageReactionAddEvent event) {
        ApplyUser findApplyUser = applyUserList.stream()
                .filter(applyUser -> applyUser.getMessageReactId().equals(event.getMessageId())).findFirst()
                .orElse(null);
        if (findApplyUser != null) {
            if (findApplyUser.onMessageReactionAdd(event)) {
                applyUserList.remove(findApplyUser);
            }
            return true;
        }

        return false;
    }

    public boolean onMessageReactionAdd_NewApplyUser(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return false;
        }
        if (event.getUser().isBot()) {
            return false;
        }

        event.getReaction().removeReaction(event.getUser()).queue();
        if (!event.getReactionEmote().getName().equals("✅")) {
            return false;
        }

        if (event.getGuild().getTextChannelsByName(higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-"
                + event.getUser().getName().replace(" ", "-"), true).size() > 0) {
            return false;
        }

        JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getUserId());

        if ((linkedAccount.isJsonNull())
                || !higherDepth(linkedAccount, "discordId").getAsString().equals(event.getUserId())) {
            PrivateChannel dmChannel = event.getUser().openPrivateChannel().complete();
            if (linkedAccount.isJsonNull()) {
                dmChannel.sendMessage(defaultEmbed("Error")
                        .setDescription("You are not linked to the bot. Please run `+link [IGN]` and try again.")
                        .build()).queue();
            } else {
                dmChannel
                        .sendMessage(defaultEmbed("Error")
                                .setDescription(
                                        "Account " + higherDepth(linkedAccount, "minecraftUsername").getAsString()
                                                + " is linked with the discord tag "
                                                + jda.getUserById(higherDepth(linkedAccount, "discordId").getAsString())
                                                        .getAsTag()
                                                + "\nYour current discord tag is " + event.getUser().getAsTag()
                                                + ".\nPlease relink and try again")
                                .build())
                        .queue();
            }
            return false;
        }

        if (!new Player(higherDepth(linkedAccount, "minecraftUsername").getAsString()).isValid()) {
            PrivateChannel dmChannel = event.getUser().openPrivateChannel().complete();
            dmChannel.sendMessage(defaultEmbed("Error").setDescription(
                    "Unable to fetch player data. Please make sure that all APIs are enabled and/or try relinking")
                    .build()).queue();
            return false;
        }

        ApplyUser applyUser = new ApplyUser(event, currentSettings,
                higherDepth(linkedAccount, "minecraftUsername").getAsString());
        applyUserList.add(applyUser);
        return true;
    }

    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        applyUserList.removeIf(applyUser -> {
            if (applyUser.getApplicationChannelId().equals(event.getChannel().getId())) {
                return true;
            } else {
                try {
                    if (applyUser.getStaffChannelId().equals(event.getChannel().getId())) {
                        return true;
                    }
                } catch (Exception ignored) {

                }
            }
            return false;
        });
    }
}
