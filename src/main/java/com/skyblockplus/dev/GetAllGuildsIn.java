/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.dev;

import static com.skyblockplus.Main.jda;
import static com.skyblockplus.utils.Utils.*;

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
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				if (args.length == 2) {
					if (args[1].equals("list")) {
						CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getAuthor()).setColumns(1).setItemsPerPage(10);

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
