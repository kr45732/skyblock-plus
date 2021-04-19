package com.skyblockplus.auctionbaz;

import static com.skyblockplus.utils.Player.getGenericInventoryMap;
import static com.skyblockplus.utils.Utils.capitalizeString;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.errorMessage;
import static com.skyblockplus.utils.Utils.getEnchantsJson;
import static com.skyblockplus.utils.Utils.getJsonKeys;
import static com.skyblockplus.utils.Utils.getPetJson;
import static com.skyblockplus.utils.Utils.getPetUrl;
import static com.skyblockplus.utils.Utils.globalCooldown;
import static com.skyblockplus.utils.Utils.higherDepth;
import static com.skyblockplus.utils.Utils.loadingEmbed;
import static com.skyblockplus.utils.Utils.logCommand;
import static com.skyblockplus.utils.Utils.simplifyNumber;
import static com.skyblockplus.utils.Utils.uuidToUsername;

import java.io.InputStreamReader;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.structs.InvItemStruct;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

public class QueryAuctionCommand extends Command {
    public QueryAuctionCommand() {
        this.name = "query";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String[] args = event.getMessage().getContentRaw().split(" ", 2);

            logCommand(event.getGuild(), event.getAuthor(), event.getMessage().getContentRaw());

            if (args.length == 2) {
                ebMessage.editMessage(queryAuctions(args[1]).build()).queue();
                return;
            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }

    private EmbedBuilder queryAuctions(String query) {
        List<String> enchantsList = getJsonKeys(higherDepth(getEnchantsJson(), "enchants_min_level"));
        for (String enchant : enchantsList) {
            if (query.replace(" ", "_").toLowerCase().contains(enchant)) {
                String enchantName = enchant + ";";
                try {
                    enchantName += Integer.parseInt(query.replaceAll("\\D+", ""));
                } catch (Exception e) {
                    enchantName += "5";
                }

                JsonArray ahQueryArr = queryAhApi("enchanted book");

                if (ahQueryArr == null) {
                    return defaultEmbed("Error").setDescription("Error fetching auctions data");
                }

                EmbedBuilder eb = defaultEmbed("Query Auctions");

                for (JsonElement lowestBinAh : ahQueryArr) {
                    Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
                    Duration duration = Duration.between(Instant.now(), endingAt);
                    if (duration.getSeconds() <= 0) {
                        continue;
                    }

                    try {
                        NBTCompound nbtData = NBTReader
                                .readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
                        InvItemStruct itemStruct = getGenericInventoryMap(nbtData).get(0);
                        if (itemStruct.getId().equals("ENCHANTED_BOOK")) {
                            if (itemStruct.getEnchantsFormatted().contains(enchantName)) {
                                String lowestBinStr = "";
                                lowestBinStr += "**Name:** " + itemStruct.getName();
                                lowestBinStr += "\n**Price:** "
                                        + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
                                lowestBinStr += "\n**Seller:** "
                                        + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString());
                                lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString()
                                        + "`";

                                long daysUntil = duration.toMinutes() / 1400;
                                long hoursUntil = duration.toMinutes() / 60 % 24;
                                long minutesUntil = duration.toMinutes() % 60;
                                String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
                                timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
                                timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";
                                lowestBinStr += "\n**Ends in:** " + timeUntil;

                                eb.addField("Lowest Bin", lowestBinStr, false);
                                eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
                                return eb;
                            }
                        }
                    } catch (Exception ignored) {
                    }

                }
                return eb.setDescription("No lowest bin found for `" + query + "` enchant");
            }
        }

        List<String> petsList = getJsonKeys(higherDepth(getPetJson(), "pet_types"));

        for (String pet : petsList) {
            if (query.replace(" ", "_").toUpperCase().contains(pet)) {
                query = query.toLowerCase();
                String rarity = "ANY";
                if (query.contains("common")) {
                    rarity = "COMMON";
                    query = query.replace("common", "").trim();
                } else if (query.contains("uncommon")) {
                    rarity = "UNCOMMON";
                    query = query.replace("uncommon", "").trim();
                } else if (query.contains("rare")) {
                    rarity = "RARE";
                    query = query.replace("rare", "").trim();
                } else if (query.contains("epic")) {
                    rarity = "EPIC";
                    query = query.replace("epic", "").trim();
                } else if (query.contains("legendary")) {
                    rarity = "LEGENDARY";
                    query = query.replace("legendary", "").trim();
                } else if (query.contains("mythic")) {
                    rarity = "MYTHIC";
                    query = query.replace("mythic", "").trim();
                }

                JsonArray ahQueryArr = queryAhApi(query);

                if (ahQueryArr == null) {
                    return defaultEmbed("Error").setDescription("Error fetching auctions data");
                }

                EmbedBuilder eb = defaultEmbed("Query Auctions");

                for (JsonElement lowestBinAh : ahQueryArr) {
                    Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
                    Duration duration = Duration.between(Instant.now(), endingAt);
                    if (duration.getSeconds() <= 0) {
                        continue;
                    }

                    if (!rarity.equals("ANY") || !higherDepth(lowestBinAh, "tier").getAsString().equals(rarity)) {
                        continue;
                    }

                    try {
                        NBTCompound nbtData = NBTReader
                                .readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
                        InvItemStruct itemStruct = getGenericInventoryMap(nbtData).get(0);
                        if (itemStruct.getId().equals("PET")) {
                            String lowestBinStr = "";
                            lowestBinStr += "**Name:** " + capitalizeString(rarity.toLowerCase()) + " "
                                    + higherDepth(lowestBinAh, "item_name").getAsString();
                            lowestBinStr += "\n**Price:** "
                                    + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
                            lowestBinStr += "\n**Seller:** "
                                    + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString());
                            lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString()
                                    + "`";

                            long daysUntil = duration.toMinutes() / 1400;
                            long hoursUntil = duration.toMinutes() / 60 % 24;
                            long minutesUntil = duration.toMinutes() % 60;
                            String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
                            timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
                            timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";
                            lowestBinStr += "\n**Ends in:** " + timeUntil;

                            eb.setThumbnail(getPetUrl(pet));
                            eb.addField("Lowest Bin", lowestBinStr, false);
                            return eb;
                        }
                    } catch (Exception ignored) {
                    }
                }
                return eb.setDescription("No lowest bin found for `" + query + "` pet");
            }
        }

        JsonArray ahQueryArr = queryAhApi(query);

        if (ahQueryArr == null) {
            return defaultEmbed("Error").setDescription("Error fetching auctions data");
        }

        EmbedBuilder eb = defaultEmbed("Query Auctions")
                .setDescription("Found `" + ahQueryArr.size() + "` bins matching `" + query + "`");

        for (JsonElement lowestBinAh : ahQueryArr) {
            Instant endingAt = Instant.ofEpochMilli(higherDepth(lowestBinAh, "end").getAsLong());
            Duration duration = Duration.between(Instant.now(), endingAt);
            if (duration.getSeconds() <= 0) {
                continue;
            }

            String lowestBinStr = "";
            lowestBinStr += "**Name:** " + higherDepth(lowestBinAh, "item_name").getAsString();
            lowestBinStr += "\n**Price:** " + simplifyNumber(higherDepth(lowestBinAh, "starting_bid").getAsDouble());
            lowestBinStr += "\n**Seller:** " + uuidToUsername(higherDepth(lowestBinAh, "auctioneer").getAsString());
            lowestBinStr += "\n**Auction:** `/ah " + higherDepth(lowestBinAh, "uuid").getAsString() + "`";

            long daysUntil = duration.toMinutes() / 1400;
            long hoursUntil = duration.toMinutes() / 60 % 24;
            long minutesUntil = duration.toMinutes() % 60;
            String timeUntil = daysUntil > 0 ? daysUntil + "d " : "";
            timeUntil += hoursUntil > 0 ? hoursUntil + "h " : "";
            timeUntil += minutesUntil > 0 ? minutesUntil + "m " : "";
            lowestBinStr += "\n**Ends in:** " + timeUntil;

            try {
                NBTCompound nbtData = NBTReader.readBase64(higherDepth(lowestBinAh, "item_bytes").getAsString());
                InvItemStruct itemStruct = getGenericInventoryMap(nbtData).get(0);
                if (itemStruct.getId().equals("ENCHANTED_BOOK")) {
                    eb.setThumbnail("https://sky.lea.moe/item.gif/ENCHANTED_BOOK");
                } else if (itemStruct.getId().equals("PET")) {
                    eb.setThumbnail(getPetUrl(itemStruct.getName().split("] ")[1].toUpperCase().replace(" ", "_")));
                } else {
                    eb.setThumbnail("https://sky.lea.moe/item.gif/" + itemStruct.getId());
                }
            } catch (Exception ignored) {
            }

            eb.addField("Lowest Bin", lowestBinStr, false);
            break;
        }

        return eb;
    }

    public JsonArray queryAhApi(String query) {
        JsonArray outputJson = null;
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet("https://api.eastarctica.tk/auctions/");
            httpget.addHeader("content-type", "application/json; charset=UTF-8");

            query = query.replace("[", "\\\\[");
            URI uri = new URIBuilder(httpget.getURI())
                    // .addParameter("query", "{\"item_name\":{\"$regex\":\"" + query
                    // +
                    // "\",\"$options\":\"i\"},\"$or\":[{\"bin\":true},{\"bids\":{\"$lt\":{\"$size\":0}}}]}")
                    .addParameter("query",
                            "{\"item_name\":{\"$regex\":\"" + query + "\",\"$options\":\"i\"},\"bin\":true}")
                    .addParameter("sort", "{\"starting_bid\":1}").build();
            httpget.setURI(uri);

            HttpResponse httpresponse = httpclient.execute(httpget);
            outputJson = JsonParser.parseReader(new InputStreamReader(httpresponse.getEntity().getContent()))
                    .getAsJsonArray();
        } catch (Exception ignored) {
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
                System.out.println("== Stack Trace (Auction Query Close Http Client) ==");
                e.printStackTrace();
            }
        }
        return outputJson;
    }
}