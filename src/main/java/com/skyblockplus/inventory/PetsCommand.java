package com.skyblockplus.inventory;

import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.CustomPaginator;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class PetsCommand extends Command {

	public PetsCommand() {
		this.name = "pets";
		this.cooldown = globalCooldown;
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

				if (args.length == 2 || args.length == 3) {
					if (args.length == 3) {
						eb = getPlayerPets(args[1], args[2], event);
					} else eb = getPlayerPets(args[1], null, event);

					if (eb == null) {
						ebMessage.delete().queue();
					} else {
						ebMessage.editMessage(eb.build()).queue();
					}
					return;
				}

				ebMessage.editMessage(errorMessage(this.name).build()).queue();
			}
		)
			.start();
	}

	private EmbedBuilder getPlayerPets(String username, String profileName, CommandEvent event) {
		Player player = profileName == null ? new Player(username) : new Player(username, profileName);
		if (player.isValid()) {
			CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor()).setColumns(3).setItemsPerPage(15);

			JsonArray playerPets = player.getPets();
			for (JsonElement pet : playerPets) {
				String petItem = null;
				try {
					petItem = convertFromInternalName(higherDepth(pet, "heldItem").getAsString()).toLowerCase();
				} catch (Exception ignored) {}

				paginateBuilder.addItems(
					"**" +
					capitalizeString(higherDepth(pet, "type").getAsString().toLowerCase().replace("_", " ")) +
					" (" +
					player.petLevelFromXp(higherDepth(pet, "exp").getAsLong(), higherDepth(pet, "tier").getAsString()) +
					")**" +
					"\nTier: " +
					higherDepth(pet, "tier").getAsString().toLowerCase() +
					(petItem != null ? "\nItem: " + petItem : "")
				);
			}
			paginateBuilder.setPaginatorExtras(
				new PaginatorExtras()
					.setEveryPageTitle(player.getUsername())
					.setEveryPageThumbnail(player.getThumbnailUrl())
					.setEveryPageTitleUrl(player.skyblockStatsLink())
			);
			paginateBuilder.build().paginate(event.getChannel(), 0);
			return null;
		}
		return defaultEmbed("Unable to fetch player data");
	}
}
