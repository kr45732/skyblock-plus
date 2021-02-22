package com.skyblockplus.miscellaneous;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.guilds.UsernameUuidStruct;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import static com.skyblockplus.utils.BotUtils.*;

public class HypixelCommand extends Command {

    public HypixelCommand() {
        this.name = "hypixel";
        this.cooldown = globalCooldown;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...", null);
        Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();

        String content = event.getMessage().getContentRaw();

        String[] args = content.split(" ");
        if (args.length != 3) {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(content);

        if (args[1].equals("player")) {
            eb = getHypixelStats(args[2]);
        } else if (args[1].equals("parkour")) {
            eb = getParkourStats(args[2]);
        } else {
            eb = defaultEmbed(errorMessage(this.name));
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        ebMessage.editMessage(eb.build()).queue();
    }

    private EmbedBuilder getParkourStats(String username) {
        UsernameUuidStruct usernameUuid = usernameToUuidUsername(username);
        if (usernameUuid != null) {
            JsonElement hypixelJson = getJson(
                    "https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuid.playerUuid);
            if (higherDepth(hypixelJson, "player") != null) {
                hypixelJson = higherDepth(hypixelJson, "player");

                try {
                    EmbedBuilder eb = defaultEmbed("Lobby Parkour for " + usernameUuid.playerUsername);
                    StringBuilder parkourCompletionString = new StringBuilder();
                    for (String parkourLocation : getJsonKeys(higherDepth(hypixelJson, "parkourCompletions"))) {
                        int fastestTime = -1;
                        for (JsonElement parkourTime : higherDepth(higherDepth(hypixelJson, "parkourCompletions"),
                                parkourLocation).getAsJsonArray()) {
                            if (higherDepth(parkourTime, "timeTook").getAsInt() > fastestTime) {
                                fastestTime = higherDepth(parkourTime, "timeTook").getAsInt();
                            }
                        }
                        if (fastestTime != -1) {
                            parkourCompletionString.append("• ").append(parkourLocation).append(": ")
                                    .append(fastestTime / 1000).append("s\n");
                        }
                    }

                    if (parkourCompletionString.length() > 0) {
                        eb.setDescription("**Fastest Parkour Times:**\n" + parkourCompletionString);
                        return eb;
                    }
                } catch (Exception ignored) {
                    return defaultEmbed("Unable to get any completed parkour");
                }
            }
        }
        return defaultEmbed("Invalid username");
    }

    private EmbedBuilder getHypixelStats(String username) {
        UsernameUuidStruct usernameUuid = usernameToUuidUsername(username);
        if (usernameUuid != null) {
            JsonElement hypixelJson = getJson(
                    "https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&uuid=" + usernameUuid.playerUuid);
            if (higherDepth(hypixelJson, "player") != null) {
                hypixelJson = higherDepth(hypixelJson, "player");

                EmbedBuilder eb = defaultEmbed("Hypixel Stats for " + usernameUuid.playerUsername);
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.US)
                        .withZone(ZoneId.systemDefault());
                DateTimeFormatter dateFormatterMedium = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        .withLocale(Locale.US).withZone(ZoneId.systemDefault());
                DateTimeFormatter dateFormatterShort = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
                        .withLocale(Locale.US).withZone(ZoneId.systemDefault());

                try {
                    if (higherDepth(hypixelJson, "lastLogin").getAsInt() > higherDepth(hypixelJson, "lastLogout")
                            .getAsInt()) {
                        eb.addField("Online", "🟢", true);
                    } else {
                        eb.addField("Status", "🔴", true);

                        eb.addField("Last Updated",
                                dateTimeFormatter.format(
                                        Instant.ofEpochMilli(higherDepth(hypixelJson, "lastLogout").getAsLong())),
                                true);
                    }
                } catch (Exception ignored) {
                }

                try {
                    eb.addField("First Login", dateFormatterMedium
                            .format(Instant.ofEpochMilli(higherDepth(hypixelJson, "firstLogin").getAsLong())), true);
                } catch (Exception ignored) {
                }

                double networkLevel = (Math.sqrt((2 * higherDepth(hypixelJson, "networkExp").getAsLong()) + 30625) / 50)
                        - 2.5;
                eb.addField("Hypixel Level", roundSkillAverage(networkLevel), true);
                eb.addField("Hypixel Rank", "WIP", true);

                try {
                    for (String socialMedia : getJsonKeys(
                            higherDepth(higherDepth(hypixelJson, "socialMedia"), "links"))) {
                        String currentSocialMediaLink = higherDepth(
                                higherDepth(higherDepth(hypixelJson, "socialMedia"), "links"), socialMedia)
                                .getAsString();
                        eb.addField(
                                socialMedia.equals("HYPIXEL") ? "Hypixel Forums"
                                        : capitalizeString(socialMedia.toLowerCase()),
                                currentSocialMediaLink.contains("http") ? "[Link](" + currentSocialMediaLink + ")"
                                        : currentSocialMediaLink,
                                true);
                    }
                } catch (Exception ignored) {
                }

                try {
                    eb.addField("Most Recent Lobby", capitalizeString(
                            higherDepth(hypixelJson, "mostRecentGameType").getAsString().toLowerCase()), true);
                } catch (Exception ignored) {
                }

                try {
                    JsonElement guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player="
                            + usernameUuid.playerUuid);

                    eb.addField("Guild", higherDepth(higherDepth(guildJson, "guild"), "name").getAsString(), true);

                    for (JsonElement member : higherDepth(higherDepth(guildJson, "guild"), "members")
                            .getAsJsonArray()) {
                        if (higherDepth(member, "uuid").getAsString().equals(usernameUuid.playerUuid)) {
                            eb.addField("Guild Rank",
                                    higherDepth(member, "rank").getAsString().equals("GUILDMASTER") ? "Guild Master"
                                            : higherDepth(member, "rank").getAsString(),
                                    true);
                            eb.addField("Guild Joined", dateFormatterMedium
                                    .format(Instant.ofEpochMilli(higherDepth(member, "joined").getAsLong())), true);
                        }
                    }

                } catch (Exception ignored) {
                }

                eb.addField("Karma", formatNumber(higherDepth(hypixelJson, "karma").getAsLong()), true);
                try {
                    eb.addField("Achievement Points",
                            formatNumber(higherDepth(hypixelJson, "achievementPoints").getAsLong()), true);
                } catch (Exception ignored) {
                }

                StringBuilder namesString = new StringBuilder();
                for (JsonElement name : getJson(
                        "https://api.mojang.com/user/profiles/" + usernameUuid.playerUuid + "/names")
                        .getAsJsonArray()) {
                    if (!higherDepth(name, "name").getAsString().equals(usernameUuid.playerUsername)) {
                        namesString.append("• ").append(higherDepth(name, "name").getAsString()).append("\n");
                    }
                }
                if (namesString.length() > 0) {
                    eb.addField("Aliases", namesString.toString(), true);
                }

                String skyblockItems = "";
                if (higherDepth(hypixelJson, "skyblock_free_cookie") != null) {
                    skyblockItems += "• Free booster cookie: "
                            + dateFormatterShort.format(
                            Instant.ofEpochMilli(higherDepth(hypixelJson, "skyblock_free_cookie").getAsLong()))
                            + "\n";
                }

                if (higherDepth(hypixelJson, "scorpius_bribe_96") != null) {
                    skyblockItems += "• Scorpius Bribe (Year 96): "
                            + dateFormatterShort.format(
                            Instant.ofEpochMilli(higherDepth(hypixelJson, "scorpius_bribe_96").getAsLong()))
                            + "\n";
                }

                if (higherDepth(hypixelJson, "claimed_potato_talisman") != null) {
                    skyblockItems += "• Potato Talisman: "
                            + dateFormatterShort.format(Instant
                            .ofEpochMilli(higherDepth(hypixelJson, "claimed_potato_talisman").getAsLong()))
                            + "\n";
                }

                if (higherDepth(hypixelJson, "claimed_potato_basket") != null) {
                    skyblockItems += "• Potato Basket: "
                            + dateFormatterShort.format(
                            Instant.ofEpochMilli(higherDepth(hypixelJson, "claimed_potato_basket").getAsLong()))
                            + "\n";
                }

                if (higherDepth(hypixelJson, "claim_potato_war_crown") != null) {
                    skyblockItems += "• Potato War Crown: "
                            + dateFormatterShort.format(Instant
                            .ofEpochMilli(higherDepth(hypixelJson, "claim_potato_war_crown").getAsLong()))
                            + "\n";
                }

                if (skyblockItems.length() > 0) {
                    eb.addField("Skyblock Misc", skyblockItems, true);
                }

                int fillGap = 3 - ((eb.getFields().size() % 3) == 0 ? 3 : (eb.getFields().size() % 3));
                for (int i = 0; i < fillGap; i++) {
                    eb.addBlankField(true);
                }
                return eb;
            }
        }
        return defaultEmbed("Invalid username");
    }
}
