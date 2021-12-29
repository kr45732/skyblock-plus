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

package com.skyblockplus.dungeons;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Set;

import static com.skyblockplus.utils.Constants.DUNGEON_META_ITEMS;
import static com.skyblockplus.utils.Utils.*;

public class PartyFinderCommand extends Command {

    public PartyFinderCommand() {
        this.name = "partyfinder";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"pf"};
        this.botPermissions = defaultPerms();
    }

    public static EmbedBuilder getPartyFinderInfo(String username, String profileName) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            EmbedBuilder eb = player.defaultPlayerEmbed();
            eb.setDescription("**Catacombs Level:** " + roundAndFormat(player.getCatacombs().getProgressLevel()));
            eb.appendDescription("\n**Secrets:** " + formatNumber(player.getDungeonSecrets()));
            eb.appendDescription("\n**Selected Class:** " + player.getSelectedDungeonClass());
            eb.appendDescription(player.getFastestF7Time());
            Set<String> necronBlade = player.getItemsPlayerHas(DUNGEON_META_ITEMS);
            eb.appendDescription(
                    "\n**Meta Items player has:** " +
                            (necronBlade != null ? (necronBlade.size() > 0 ? String.join(", ", necronBlade) : "None") : "Inventory API disabled")
            );
            return eb;
        }

        return player.getFailEmbed();
    }

    @Override
    protected void execute(CommandEvent event) {
        new CommandExecute(this, event) {
            @Override
            protected void execute() {
                logCommand();

                if (args.length == 3 || args.length == 2 || args.length == 1) {
                    if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
                        return;
                    }

                    embed(getPartyFinderInfo(username, args.length == 3 ? args[2] : null));
                    return;
                }

                sendErrorEmbed();
            }
        }
                .queue();
    }
}
