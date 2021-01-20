package com.SkyblockBot.Apply;

import static com.SkyblockBot.Miscellaneous.BotUtils.defaultEmbed;
import static com.SkyblockBot.Miscellaneous.BotUtils.formatNumber;
import static com.SkyblockBot.Miscellaneous.BotUtils.getJson;
import static com.SkyblockBot.Miscellaneous.BotUtils.higherDepth;
import static com.SkyblockBot.Miscellaneous.BotUtils.key;
import static com.SkyblockBot.Miscellaneous.BotUtils.usernameToUuid;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.addChannel;
import static com.SkyblockBot.Miscellaneous.ChannelDeleter.removeChannel;
import static com.SkyblockBot.Skills.SkillsCommands.skillInfoFromExp;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.SkyblockBot.Skills.SkillsStruct;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ApplyUser extends ListenerAdapter {
    Message reactMessage;
    User applyingUser;
    TextChannel applicationChannel;
    int state = 0;
    String username;
    String profile;
    String profileId;
    EmbedBuilder ebMain;
    JsonElement currentSettings;

    public ApplyUser(MessageReactionAddEvent event, User applyingUser, JsonElement currentSettings) {
        this.applyingUser = applyingUser;
        this.currentSettings = currentSettings;

        String channelPrefix = higherDepth(currentSettings, "new_channel_prefix").getAsString();
        Category applyCategory = event.getGuild()
                .getCategoryById(higherDepth(higherDepth(currentSettings, "new_channel_category"), "id").getAsString());
        applyCategory.createTextChannel(channelPrefix + "-" + applyingUser.getName())
                .addPermissionOverride(event.getGuild().getMember(applyingUser), EnumSet.of(Permission.VIEW_CHANNEL),
                        null)
                .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                .complete();

        applicationChannel = event.getGuild().getTextChannelsByName(channelPrefix + "-" + applyingUser.getName(), true)
                .get(0);
        addChannel(applicationChannel);
        applicationChannel.sendMessage("Welcome " + applyingUser.getAsMention() + "!").queue();

        EmbedBuilder welcomeEb = defaultEmbed("Application for " + applyingUser.getName(), null);
        welcomeEb.setDescription(
                "• Please enter your in-game-name followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma Zucchini\n");
        welcomeEb.addField("To submit your LAST message,", "React with ✅", true);
        welcomeEb.addField("To cancel the application,", "React with ❌", true);
        applicationChannel.sendMessage(welcomeEb.build()).queue(message -> {
            message.addReaction("✅").queue();
            message.addReaction("❌").queue();
            this.reactMessage = message;
        });

    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (event.getMessageIdLong() != reactMessage.getIdLong()) {
            return;
        }
        if (event.getUser().isBot()) {
            return;
        }

        if (event.getReactionEmote().getName().equals("❌")) {
            state = 4;
        } else if (!event.getReactionEmote().getName().equals("✅")) {
            reactMessage.clearReactions(event.getReaction().getReactionEmote().getAsReactionCode()).queue();
            return;
        }
        reactMessage.clearReactions().queue();
        switch (state) {
            case 0:
                applicationChannel.retrieveMessageById(applicationChannel.getLatestMessageId()).queue(messageReply -> {
                    if (messageReply.getAuthor().equals(applyingUser)) {
                        try {
                            username = messageReply.getContentDisplay().split(" ")[0];
                            profile = messageReply.getContentDisplay().split(" ")[1];
                            if (checkValid(username, profile)) {
                                EmbedBuilder eb0 = defaultEmbed("Stats for " + username,
                                        "https://sky.shiiyu.moe/stats/" + username + "/" + profile);
                                eb0.addField("Total slayer", getPlayerSlayer(username), true);
                                eb0.addField("True average skill level", getPlayerSkills(username), true);
                                eb0.addField("Catacombs level", getPlayerCatacombs(username), true);
                                Gson gson = new Gson();
                                ebMain = gson.fromJson(gson.toJson(eb0), EmbedBuilder.class);
                                eb0.addField("Are the above stats correct?", "React with ✅ for yes and ❌ to cancel",
                                        false);
                                applicationChannel.sendMessage(eb0.build()).queue(message -> {
                                    message.addReaction("✅").queue();
                                    message.addReaction("❌").queue();
                                    this.reactMessage = message;

                                });
                                state = 1;
                            } else {
                                EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                                applicationChannel.sendMessage(eb.build()).queue(message -> {
                                    message.addReaction("✅").queue();
                                    message.addReaction("❌").queue();
                                    this.reactMessage = message;
                                });
                                state = 2;
                            }
                        } catch (Exception ex) {
                            EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                            applicationChannel.sendMessage(eb.build()).queue(message -> {
                                message.addReaction("✅").queue();
                                message.addReaction("❌").queue();
                                this.reactMessage = message;
                            });
                            state = 2;
                        }

                    } else {
                        EmbedBuilder eb = invalidInput(messageReply.getContentDisplay());
                        applicationChannel.sendMessage(eb.build()).queue(message -> {
                            message.addReaction("✅").queue();
                            message.addReaction("❌").queue();
                            this.reactMessage = message;
                        });
                        state = 2;
                    }
                });
                break;
            case 1:
                EmbedBuilder eb = defaultEmbed("Thank you for applying!", null);
                eb.setDescription(
                        "**Your stats have been subbmited to staff**\nYou will be notified once staff review your stats");
                applicationChannel.sendMessage(eb.build()).queue();
                event.getJDA().removeEventListener(this);

                event.getJDA()
                        .addEventListener(new ApplyStaff(applyingUser, applicationChannel, ebMain, currentSettings));
                break;
            case 2:
                EmbedBuilder eb2 = defaultEmbed("Application for " + applyingUser.getName(), null);
                eb2.setDescription(
                        "• Please enter your in-game-name followed by the skyblock profile you want to apply with.\n• Ex: CrypticPlasma Zucchini\n");
                eb2.addField("To submit your LAST message,", "React with ✅", true);
                eb2.addField("To cancel the application,", "React with ❌", true);
                applicationChannel.sendMessage(eb2.build()).queue(message -> {
                    message.addReaction("✅").queue();
                    message.addReaction("❌").queue();
                    this.reactMessage = message;
                });

                state = 0;
                break;
            case 4:
                EmbedBuilder eb4 = defaultEmbed("Canceling application", null);
                eb4.setDescription("Channel closing in 5 seconds...");
                applicationChannel.sendMessage(eb4.build()).queue();
                event.getJDA().removeEventListener(this);
                event.getGuild().getTextChannelById(event.getChannel().getId()).delete().reason("Application canceled")
                        .queueAfter(5, TimeUnit.SECONDS);
                removeChannel(applicationChannel);
                break;
        }
    }

    public boolean checkValid(String username, String profile) {
        JsonElement playerJson = getJson(
                "https://api.hypixel.net/player?key=" + key + "&uuid=" + usernameToUuid(username));

        if (playerJson == null) {
            return false;
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return false;
        }

        String userProfile = higherDepth(
                higherDepth(higherDepth(higherDepth(playerJson, "player"), "stats"), "SkyBlock"), "profiles")
                        .toString();
        userProfile = userProfile.substring(1, userProfile.length() - 2);
        String[] outputStr = userProfile.split("},");
        String[] profileId = new String[outputStr.length];

        for (int i = 0; i < outputStr.length; i++) {
            outputStr[i] = outputStr[i].substring(outputStr[i].indexOf(":{") + 2);
            profileId[i] = outputStr[i].substring(outputStr[i].indexOf("id") + 5, outputStr[i].indexOf("cute") - 3);
        }

        int profileIndex = -1;
        for (int i = 0; i < outputStr.length; i++) {
            String currentProfile = outputStr[i].substring(outputStr[i].indexOf("name") + 7, outputStr[i].length() - 1);
            if (currentProfile.equalsIgnoreCase(profile)) {
                profileIndex = i;
                break;
            }
        }

        if (profileIndex != -1) {
            this.profileId = profileId[profileIndex];
            return true;

        }
        return false;
    }

    public String getPlayerSlayer(String username) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return "Unable to retrieve player's slayer";
        }
        String uuidPlayer = usernameToUuid(username);
        JsonElement profileSlayer = higherDepth(
                higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer), "slayer_bosses");

        int slayerWolf = higherDepth(higherDepth(profileSlayer, "wolf"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "wolf"), "xp").getAsInt()
                : -1;
        int slayerZombie = higherDepth(higherDepth(profileSlayer, "zombie"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "zombie"), "xp").getAsInt()
                : -1;
        int slayerSpider = higherDepth(higherDepth(profileSlayer, "spider"), "xp") != null
                ? higherDepth(higherDepth(profileSlayer, "spider"), "xp").getAsInt()
                : -1;
        int totalSlayer = ((slayerWolf != -1 ? slayerWolf : 0) + (slayerZombie != -1 ? slayerZombie : 0)
                + (slayerSpider != -1 ? slayerSpider : 0));

        String output = ((slayerWolf != -1 && slayerZombie != -1 && slayerSpider != -1)
                ? formatNumber(totalSlayer) + " XP"
                : "None");

        return output;
    }

    public String getPlayerCatacombs(String username) {

        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return "Unable to retrive player's catacombs";
        }

        String uuidPlayer = usernameToUuid(username);

        double skillExp = higherDepth(higherDepth(higherDepth(
                higherDepth(higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                        "dungeons"),
                "dungeon_types"), "catacombs"), "experience").getAsLong();
        SkillsStruct skillInfo = skillInfoFromExp(skillExp, "catacombs");

        return "" + skillInfo.skillLevel;
    }

    public String getPlayerSkills(String username) {
        String playerUrl = "https://api.hypixel.net/skyblock/profile?key=" + key + "&profile=" + profileId;
        JsonElement skyblockJson = getJson(playerUrl);

        if (skyblockJson == null) {
            return "Unable to retrive player's skills";
        }
        String uuidPlayer = usernameToUuid(username);
        JsonElement levelTabels = getJson(
                "https://raw.githubusercontent.com/Moulberry/NotEnoughUpdates-REPO/master/constants/leveling.json");

        JsonElement skillsCap = higherDepth(levelTabels, "leveling_caps");

        List<String> skills = skillsCap.getAsJsonObject().entrySet().stream().map(i -> i.getKey())
                .collect(Collectors.toCollection(ArrayList::new));
        skills.remove("catacombs");
        skills.remove("runecrafting");
        skills.remove("carpentry");

        double skillAverage = 0;
        for (String skill : skills) {
            double skillExp = higherDepth(
                    higherDepth(higherDepth(higherDepth(skyblockJson, "profile"), "members"), uuidPlayer),
                    "experience_skill_" + skill).getAsLong();
            SkillsStruct skillInfo = skillInfoFromExp(skillExp, skill);
            skillAverage += skillInfo.skillLevel;

        }
        skillAverage /= skills.size();
        return "" + skillAverage;
    }

    public EmbedBuilder invalidInput(String invalidUserInput) {
        EmbedBuilder eb = defaultEmbed("Invalid Arguments", null);
        eb.setDescription("**Please check your input!**");
        eb.addField("Argument(s) given:", invalidUserInput, true);
        eb.addField("Valid Arguments Example:", "CrypticPlasma Zucchini", true);
        eb.addBlankField(true);
        eb.addField("To retry,", "React with ✅", true);
        eb.addField("To cancel the application,", "React with ❌", true);
        eb.addBlankField(true);
        return eb;
    }
}