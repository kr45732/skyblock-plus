package com.skyblockplus.apply;

import com.google.gson.JsonElement;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import static com.skyblockplus.reload.ReloadEventWatcher.addApplyGuild;
import static com.skyblockplus.utils.Utils.higherDepth;

public class ApplyGuild extends ListenerAdapter {
    private final Message reactMessage;
    private final JsonElement currentSettings;

    public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
        this.reactMessage = reactMessage;
        this.currentSettings = currentSettings;
        addApplyGuild(reactMessage.getGuild().getId(), this);
    }

    public ApplyGuild(String guildId) {
        this.reactMessage = null;
        this.currentSettings = null;
        addApplyGuild(guildId, this);
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (reactMessage == null) {
            return;
        }

        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        event.getReaction().removeReaction(event.getUser()).queue();
        if (!event.getReactionEmote().getName().equals("✅")) {
            return;
        }

        if (event.getGuild().getTextChannelsByName(higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-"
                + event.getUser().getName().replace(" ", "-"), true).size() > 0) {
            return;
        }

        event.getJDA().addEventListener(new ApplyUser(event, event.getUser(), currentSettings));
    }
}
