package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.InteractionHook;

public class BankCommand extends Command {

	public BankCommand() {
		this.name = "bank";
		this.cooldown = globalCooldown;
	}

	public static EmbedBuilder getPlayerBalance(String username, String profileName) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			double playerBankBalance = player.getBankBalance();
			double playerPurseCoins = player.getPurseCoins();

			EmbedBuilder eb = player.defaultPlayerEmbed();
			eb.setDescription("**Total coins:** " + simplifyNumber(playerBankBalance + playerPurseCoins));
			eb.addField(
				"Bank balance",
				playerBankBalance == -1 ? "Banking API disabled" : simplifyNumber(playerBankBalance) + " coins",
				false
			);
			eb.addField(
				"Purse coins",
				playerPurseCoins == -1 ? "Banking API disabled" : simplifyNumber(playerPurseCoins) + " coins",
				false
			);
			return eb;
		}
		return defaultEmbed("Unable to fetch player data");
	}

	public static EmbedBuilder getPlayerBankHistory(
		String username,
		String profileName,
		User user,
		MessageChannel channel,
		InteractionHook hook
	) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			JsonArray bankHistoryArray = player.getBankHistory();
			if (bankHistoryArray != null) {
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter
					.ofLocalizedDate(FormatStyle.MEDIUM)
					.withLocale(Locale.US)
					.withZone(ZoneId.systemDefault());

				CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, user).setColumns(1).setItemsPerPage(20);

				paginateBuilder.addItems(
					"**Last Transaction Time:** " +
					dateTimeFormatter.format(
						Instant.ofEpochMilli(higherDepth(bankHistoryArray.get(bankHistoryArray.size() - 1), "timestamp").getAsLong())
					) +
					"\n"
				);
				for (int i = bankHistoryArray.size() - 1; i >= 0; i--) {
					JsonElement currentTransaction = bankHistoryArray.get(i);
					String valueString =
						simplifyNumber(higherDepth(currentTransaction, "amount").getAsLong()) +
						" " +
						(higherDepth(currentTransaction, "action").getAsString().equals("DEPOSIT") ? "deposited" : "withdrawn") +
						" by " +
						parseMcCodes(higherDepth(currentTransaction, "initiator_name").getAsString());

					String time = dateTimeFormatter.format(Instant.ofEpochMilli(higherDepth(currentTransaction, "timestamp").getAsLong()));
					paginateBuilder.addItems("**" + time + "**: " + valueString);
				}

				paginateBuilder.setPaginatorExtras(
					new PaginatorExtras()
						.setEveryPageTitle(player.getUsername())
						.setEveryPageThumbnail(player.getThumbnailUrl())
						.setEveryPageTitleUrl(player.skyblockStatsLink())
				);

				if (channel != null) {
					paginateBuilder.build().paginate(channel, 0);
				} else {
					paginateBuilder.build().paginate(hook, 0);
				}
				return null;
			} else {
				return defaultEmbed("Player banking API disabled");
			}
		}
		return defaultEmbed("Unable to fetch player data");
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
				String content = event.getMessage().getContentRaw();
				String[] args = content.split(" ");

				logCommand(event.getGuild(), event.getAuthor(), content);

				if ((args.length == 3 || args.length == 4) && args[1].equals("history")) {
					if (args.length == 4) {
						eb = getPlayerBankHistory(args[2], args[3], event.getAuthor(), event.getChannel(), null);
					} else {
						eb = getPlayerBankHistory(args[2], null, event.getAuthor(), event.getChannel(), null);
					}

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				} else if (args.length == 2) {
					ebMessage.editMessage(getPlayerBalance(args[1], null).build()).queue();
					return;
				} else if (args.length == 3) {
					ebMessage.editMessage(getPlayerBalance(args[1], args[2]).build()).queue();
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}
}
