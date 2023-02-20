/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.entities.RoleConnectionMetadata;
import org.springframework.stereotype.Component;

@Component
public class LinkedRolesMetadataCommand extends Command {

	public LinkedRolesMetadataCommand() {
		this.name = "d-linked-roles";
		this.ownerCommand = true;
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				List<RoleConnectionMetadata> metadataList = new ArrayList<>();
				metadataList.add(
					new RoleConnectionMetadata(
						RoleConnectionMetadata.MetadataType.BOOLEAN_EQUAL,
						"Verified",
						"verified",
						"Hypixel account linked to the bot"
					)
				);
				metadataList.add(generateNumericRole("level", "Skyblock Level"));
				metadataList.add(generateNumericRole("networth", "Networth"));
				metadataList.add(generateNumericRole("weight", "Senither Weight"));
				metadataList.add(generateNumericRole("lily_weight", "Lily Weight"));

				jda
					.getShardById(0)
					.updateRoleConnectionMetadata(metadataList)
					.queue(s ->
						event.getChannel().sendMessageEmbeds(defaultEmbed("Success - added " + s.size() + " linked roles").build()).queue()
					);
			}
		}
			.queue();
	}

	private static RoleConnectionMetadata generateNumericRole(String key, String name) {
		return new RoleConnectionMetadata(
			RoleConnectionMetadata.MetadataType.INTEGER_GREATER_THAN_OR_EQUAL,
			name,
			key,
			name + " (keep this disabled)"
		);
	}
}
