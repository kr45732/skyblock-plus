package com.skyblockplus.eventlisteners.apply;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.MainClassUtils.getApplyGuildUsersCache;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

public class ApplyGuild {

	public List<ApplyUser> applyUserList = new ArrayList<>();
	public Message reactMessage;
	public JsonElement currentSettings;
	public boolean enable = true;
	public TextChannel waitInviteChannel = null;

	public ApplyGuild(Message reactMessage, JsonElement currentSettings) {
		this.reactMessage = reactMessage;
		this.currentSettings = currentSettings;
		this.applyUserList = getApplyGuildUsersCache(reactMessage.getGuild().getId(), higherDepth(currentSettings, "name").getAsString());
		try {
			this.waitInviteChannel = jda.getTextChannelById(higherDepth(currentSettings, "waitingChannelId").getAsString());
		} catch (Exception ignored) {}
	}

	public ApplyGuild(Message reactMessage, JsonElement currentSettings, List<ApplyUser> prevApplyUsers) {
		this(reactMessage, currentSettings);
		applyUserList.addAll(prevApplyUsers);
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

		// if (onMessageReactionAdd_NewApplyUser(event)) {
		// 	return;
		// }

		if (onMessageReactionAdd_ExistingApplyUser(event)) {
			return;
		}

		onMessageReactionAdd_WaitingForInviteApplyUser(event);
	}

	private void onMessageReactionAdd_WaitingForInviteApplyUser(MessageReactionAddEvent event) {
		if (event.getUser().isBot()) {
			return;
		}

		if (!event.getChannel().equals(waitInviteChannel)) {
			return;
		}

		if (!event.getReactionEmote().getName().equals("✅")) {
			return;
		}

		Message msg = waitInviteChannel.retrieveMessageById(event.getMessageId()).complete();

		if (!msg.getAuthor().getId().equals(jda.getSelfUser().getId())) {
			return;
		}

		msg.clearReactions().complete();

		msg.delete().queueAfter(3, TimeUnit.SECONDS);
	}

	public boolean onMessageReactionAdd_ExistingApplyUser(MessageReactionAddEvent event) {
		ApplyUser findApplyUser = applyUserList
			.stream()
			.filter(applyUser -> applyUser.getMessageReactId().equals(event.getMessageId()))
			.findFirst()
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
		if (event.getUser().isBot()) {
			return false;
		}

		if (event.getMessageIdLong() != reactMessage.getIdLong()) {
			return false;
		}

		event.getReaction().removeReaction(event.getUser()).queue();
		if (!event.getReactionEmote().getName().equals("✅")) {
			return true;
		}

		if (
			event
				.getGuild()
				.getTextChannelsByName(
					higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-" + event.getUser().getName().replace(" ", "-"),
					true
				)
				.size() >
			0
		) {
			return true;
		}

		JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getUserId());

		if (linkedAccount.isJsonNull() || !higherDepth(linkedAccount, "discordId").getAsString().equals(event.getUserId())) {
			PrivateChannel dmChannel = event.getUser().openPrivateChannel().complete();
			if (linkedAccount.isJsonNull()) {
				dmChannel
					.sendMessage(
						defaultEmbed("Error")
							.setDescription("You are not linked to the bot. Please run `+link [IGN]` and try again.")
							.build()
					)
					.queue();
			} else {
				dmChannel
					.sendMessage(
						defaultEmbed("Error")
							.setDescription(
								"Account " +
								higherDepth(linkedAccount, "minecraftUsername").getAsString() +
								" is linked with the discord tag " +
								jda.retrieveUserById(higherDepth(linkedAccount, "discordId").getAsString()).complete().getAsTag() +
								"\nYour current discord tag is " +
								event.getUser().getAsTag() +
								".\nPlease relink and try again"
							)
							.build()
					)
					.queue();
			}
			return true;
		}

		Player player = new Player(higherDepth(linkedAccount, "minecraftUsername").getAsString());

		if (!player.isValid()) {
			PrivateChannel dmChannel = event.getUser().openPrivateChannel().complete();
			dmChannel
				.sendMessage(
					defaultEmbed("Error")
						.setDescription("Unable to fetch player data. Please make sure that all APIs are enabled and/or try relinking")
						.build()
				)
				.queue();
			return true;
		} else {
			boolean isIronman = false;
			try {
				isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
			} catch (Exception ignored) {}
			if (isIronman && player.getAllProfileNames(isIronman).length == 0) {
				PrivateChannel dmChannel = event.getUser().openPrivateChannel().complete();
				dmChannel.sendMessage(defaultEmbed("Error").setDescription("You have no ironman profiles created").build()).queue();
				return true;
			}
		}

		// ApplyUser applyUser = new ApplyUser(event, currentSettings, higherDepth(linkedAccount, "minecraftUsername").getAsString());
		// applyUserList.add(applyUser);
		return true;
	}

	public void onTextChannelDelete(TextChannelDeleteEvent event) {
		applyUserList.removeIf(
			applyUser -> {
				if (applyUser.getApplicationChannelId().equals(event.getChannel().getId())) {
					return true;
				} else {
					try {
						if (applyUser.getStaffChannelId().equals(event.getChannel().getId())) {
							return true;
						}
					} catch (Exception ignored) {}
				}
				return false;
			}
		);
	}

	public String onMessageReactionAdd_NewApplyUser(ButtonClickEvent event) {
		if (event.getUser().isBot()) {
			return null;
		}

		if (event.getMessageIdLong() != reactMessage.getIdLong()) {
			return null;
		}

		if (!event.getButton().getId().equals("create_application_button_" + higherDepth(currentSettings, "name").getAsString())) {
			return null;
		}

		List<TextChannel> openApplys = event
			.getGuild()
			.getTextChannelsByName(
				higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-" + event.getUser().getName().replace(" ", "-"),
				true
			);

		if (openApplys.size() > 0) {
			return "❌ There is already an application open in " + openApplys.get(0).getAsMention();
		}

		JsonElement linkedAccount = database.getLinkedUserByDiscordId(event.getUser().getId());

		if (linkedAccount.isJsonNull() || !higherDepth(linkedAccount, "discordId").getAsString().equals((event.getUser().getId()))) {
			if (linkedAccount.isJsonNull()) {
				return "❌ You are not linked to the bot. Please run `+link [IGN]` and try again.";
			} else {
				return (
					"❌ Account " +
					higherDepth(linkedAccount, "minecraftUsername").getAsString() +
					" is linked with the discord tag " +
					jda.retrieveUserById(higherDepth(linkedAccount, "discordId").getAsString()).complete().getAsTag() +
					"\nYour current discord tag is " +
					event.getUser().getAsTag() +
					".\nPlease relink and try again"
				);
			}
		}

		Player player = new Player(higherDepth(linkedAccount, "minecraftUsername").getAsString());

		if (!player.isValid()) {
			return "❌ Unable to fetch player data. Please make sure that all APIs are enabled and/or try relinking";
		} else {
			boolean isIronman = false;
			try {
				isIronman = higherDepth(currentSettings, "ironmanOnly").getAsBoolean();
			} catch (Exception ignored) {}
			if (isIronman && player.getAllProfileNames(isIronman).length == 0) {
				return "❌ You have no ironman profiles created";
			}
		}

		ApplyUser applyUser = new ApplyUser(event, currentSettings, higherDepth(linkedAccount, "minecraftUsername").getAsString());
		applyUserList.add(applyUser);
		openApplys =
			event
				.getGuild()
				.getTextChannelsByName(
					higherDepth(currentSettings, "newChannelPrefix").getAsString() + "-" + event.getUser().getName().replace(" ", "-"),
					true
				);

		return "✅ A new application was created in " + openApplys.get(0).getAsMention();
	}

	public String onButtonClick(ButtonClickEvent event) {
		return onMessageReactionAdd_NewApplyUser(event);
	}
}
