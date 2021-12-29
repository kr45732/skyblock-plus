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

package com.skyblockplus.inventory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.command.CommandExecute;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.PaginatorEvent;
import com.skyblockplus.utils.structs.PaginatorExtras;
import net.dv8tion.jda.api.EmbedBuilder;

import java.util.Comparator;
import java.util.Map;

import static com.skyblockplus.utils.Utils.*;

public class SacksCommand extends Command {

    public SacksCommand() {
        this.name = "sacks";
        this.cooldown = globalCooldown;
        this.botPermissions = defaultPerms();
    }

    public static EmbedBuilder getPlayerSacks(String username, String profileName, boolean useNpcPrice, PaginatorEvent event) {
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (player.isValid()) {
            Map<String, Integer> sacksMap = player.getPlayerSacks();
            if (sacksMap == null) {
                return invalidEmbed("Inventory API disabled");
            }

            CustomPaginator.Builder paginateBuilder = defaultPaginator(event.getUser()).setColumns(1).setItemsPerPage(20);

            JsonElement bazaarPrices = higherDepth(getBazaarJson(), "products");

            final double[] total = {0, 0};
            JsonObject missing = new JsonObject();
            sacksMap
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(
                            Comparator.comparingDouble(entry -> {
                                double npcPrice = -1;
                                if (useNpcPrice) {
                                    npcPrice = getNpcSellPrice(entry.getKey());
                                }

                                return (
                                        -(
                                                npcPrice != -1
                                                        ? npcPrice
                                                        : higherDepth(bazaarPrices, entry.getKey() + ".sell_summary.[0].pricePerUnit", 0.0)
                                        ) *
                                                entry.getValue()
                                );
                            })
                    )
                    .forEach(currentSack -> {
                        double npcPrice = -1;
                        if (useNpcPrice) {
                            npcPrice = getNpcSellPrice(currentSack.getKey());
                        }
                        double sackPrice =
                                (
                                        npcPrice != -1
                                                ? npcPrice
                                                : higherDepth(bazaarPrices, currentSack.getKey() + ".sell_summary.[0].pricePerUnit", 0.0)
                                ) *
                                        currentSack.getValue();

                        String emoji = higherDepth(getEmojiMap(), currentSack.getKey(), null);
                        if (emoji == null && currentSack.getKey().equals("MUSHROOM_COLLECTION")) {
                            emoji = higherDepth(getEmojiMap(), "RED_MUSHROOM", null);
                        }

                        if (emoji == null) {
                            missing.addProperty(currentSack.getKey(), idToName(currentSack.getKey()));
                        }
                        paginateBuilder.addItems(
                                (emoji != null ? emoji + " " : "") +
                                        "**" +
                                        convertSkyblockIdName(currentSack.getKey()) +
                                        ":** " +
                                        formatNumber(currentSack.getValue()) +
                                        " ➜ " +
                                        simplifyNumber(sackPrice)
                        );
                        total[npcPrice != -1 ? 1 : 0] += sackPrice;
                    });

            paginateBuilder.setPaginatorExtras(
                    new PaginatorExtras()
                            .setEveryPageTitle(player.getUsername())
                            .setEveryPageThumbnail(player.getThumbnailUrl())
                            .setEveryPageTitleUrl(player.skyblockStatsLink())
                            .setEveryPageText(
                                    "**Total value:** " +
                                            roundAndFormat(total[0] + total[1]) +
                                            (useNpcPrice ? " (" + roundAndFormat(total[1]) + " npc + " + roundAndFormat(total[0]) + " bazaar)" : "") +
                                            "\n"
                            )
            );
            System.out.println(makeHastePost(missing.toString()));
            event.paginate(paginateBuilder);
            return null;
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
                    boolean useNpc = getBooleanArg("--npc");

                    if (getMentionedUsername(args.length == 1 ? -1 : 1)) {
                        return;
                    }

                    paginate(getPlayerSacks(username, args.length == 3 ? args[2] : null, useNpc, new PaginatorEvent(event)));
                    return;
                }

                sendErrorEmbed();
            }
        }
                .queue();
    }
}
