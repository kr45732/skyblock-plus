package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.Main.waiter;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.defaultPaginator;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.structs.PaginatorExtras;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;

public class GetAllGuildsIn extends Command {

	public GetAllGuildsIn() {
		this.name = "d-servers";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					if (args[1].equals("list")) {
						CustomPaginator.Builder paginateBuilder = defaultPaginator(waiter, event.getAuthor())
							.setColumns(1)
							.setItemsPerPage(10);

						for (Guild guild : jda.getGuilds()) {
							if (guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
								continue;
							}

							try {
								List<Invite> invites = guild.retrieveInvites().complete();

								if (invites.size() > 0) {
									paginateBuilder.addItems(
										"**" +
										guild.getName() +
										"**\nInvite Link: " +
										invites.get(0).getUrl() +
										"\nId: " +
										guild.getId() +
										"\nOwner: " +
										guild.getOwnerId() +
										"\n"
									);
								} else {
									paginateBuilder.addItems(
										"**" +
										guild.getName() +
										"**\nInvite Link: " +
										guild.getChannels().get(0).createInvite().setMaxAge(0).complete().getUrl() +
										"\nId: " +
										guild.getId() +
										"\nOwner: " +
										guild.getOwnerId() +
										"\n"
									);
								}
							} catch (Exception e) {
								paginateBuilder.addItems(
									"**" + guild.getName() + "**\nId: " + guild.getId() + "\nOwner: " + guild.getOwnerId() + "\n"
								);
							}
						}

						paginateBuilder.setPaginatorExtras(new PaginatorExtras().setEveryPageTitle("Server List"));

						ebMessage.delete().queue();
						paginateBuilder.build().paginate(event.getChannel(), 0);
						return;
					} else if (args[1].equals("count")) {
						eb = defaultEmbed("Server Count").addField("Total guild count", jda.getGuilds().size() + " servers", false);

						int guildCount = 0;
						for (Guild guild : jda.getGuilds()) {
							if (!guild.getName().startsWith("Skyblock Plus - Emoji Server")) {
								guildCount++;
							}
						}

						eb.addField("Total guild count without emoji servers", guildCount + " servers", false);
						embed(eb);
						return;
					}
				}

				sendErrorEmbed();
			}
		}
			.submit();
	}
}
