package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.ArrayList;
import java.util.List;

import static com.skyblockplus.utils.Utils.higherDepth;

public class ApplyGuild {
    private final List<ApplyUser> applyUserList = new ArrayList<>();
    private Message reactMessage;
    private JsonElement currentSettings;
    private boolean enable = true;

    public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.currentSettings = currentSettings;
    }

    public ApplyGuild(boolean enable) {
        this.enable = enable;
    }

    public int applyUserListSize() {
        return applyUserList.size();
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
        ApplyUser findApplyUser = applyUserList.stream().filter(applyUser -> applyUser.getMessageReactId().equals(event.getMessageId())).findFirst().orElse(null);
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

        ApplyUser applyUser = new ApplyUser(event, event.getUser(), currentSettings);
        applyUserList.add(applyUser);
        return true;
    }

    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        applyUserList.removeIf(applyUser -> {
            if (applyUser.getApplicationChannel().getId().equals(event.getChannel().getId())) {
                return true;
            } else {
                try {
                    if (applyUser.getStaffChannel().getId().equals(event.getChannel().getId())) {
                        return true;
                    }
                } catch (Exception ignored) {

                }
            }
            return false;
        });
    }
}
