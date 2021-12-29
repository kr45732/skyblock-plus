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

package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.slashcommand.SlashCommand;
import com.skyblockplus.utils.slashcommand.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class RolesSlashCommand extends SlashCommand {

    public RolesSlashCommand() {
        this.name = "roles";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        event.logCommand();

        switch (event.getSubcommandName()) {
            case "claim" -> event.embed(RolesCommand.updateRoles(event.getOptionStr("profile"), event.getGuild(), event.getMember()));
            case "list" -> event.paginate(RolesCommand.listRoles(new PaginatorEvent(event)));
            default -> event.invalidCommandMessage();
        }
    }

    @Override
    public CommandData getCommandData() {
        return new CommandData(name, "Main roles command")
                .addSubcommands(
                        new SubcommandData("claim", "Claim automatic Skyblock roles. The player must be linked to the bot")
                                .addOption(OptionType.STRING, "profile", "Profile name"),
                        new SubcommandData("list", "List all roles that can be claimed through the bot")
                );
    }
}
