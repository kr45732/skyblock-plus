package com.skyblockplus.roles;

import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.skyblockplus.utils.BotUtils.*;

public class RoleCommands extends Command {

    public RoleCommands() {
        this.name = "roles";
        this.cooldown = 15;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = defaultEmbed("Loading...", null);
        eb.setDescription("**NOTE: This may take some time**");

        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        User user = event.getAuthor();
        Message ebMessage = channel.sendMessage(eb.build()).complete();

        String[] args = content.split(" ");
        if (args.length < 3 || args.length > 4) {
            eb.setTitle(errorMessage(this.name));
            eb.setDescription("");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        System.out.println(content);

        if (getPlayerInfo(args[2]) == null) {
            eb = defaultEmbed("Discord tag mismatch", null);
            eb.setDescription("Unable to get Discord tag linked with Hypixel account");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        DiscordStruct playerInfo = getPlayerInfo(args[2]);

        if (!user.getAsTag().equals(playerInfo.discordTag)) {
            eb = defaultEmbed("Discord tag mismatch", null);
            eb.setDescription("Account **" + playerInfo.username + "** is linked with the discord tag `"
                    + playerInfo.discordTag + "`\nYour current discord tag is `" + user.getAsTag() + "`");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        String username = playerInfo.username;
        String profileName = args.length == 4 ? args[3] : null;
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (!player.isValid()) {
            eb = defaultEmbed("Error fetching data", null);
            eb.setDescription("**Please check given username and profile**");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        if (args[1].equals("claim")) {
            try {
                JsonElement settings = JsonParser
                        .parseReader(new FileReader("src/main/java/com/skyblockplus/json/GuildSettings.json"));
                if (higherDepth(settings, guild.getId()) != null) {
                    JsonElement rolesJson = higherDepth(higherDepth(settings, guild.getId()), "automated_roles");
                    if (!higherDepth(rolesJson, "enable").getAsBoolean()) {
                        eb = defaultEmbed("Automatic roles not enabled for this server", null);
                        ebMessage.editMessage(eb.build()).queue();
                        return;
                    }
                    List<String> rolesID = rolesJson.getAsJsonObject().entrySet().stream().map(Map.Entry::getKey)
                            .collect(Collectors.toCollection(ArrayList::new));

                    eb = defaultEmbed("Automatic roles for " + player.getUsername(),
                            skyblockStatsLink(player.getUsername(), player.getProfileName()));
                    StringBuilder addedRoles = new StringBuilder();
                    StringBuilder removedRoles = new StringBuilder();
                    StringBuilder errorRoles = new StringBuilder();

                    for (String currentRoleName : rolesID) {
                        JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
                        switch (currentRoleName) {
                            case "guild_member":
                            case "related_guild_member": {
                                JsonElement guildJson = getJson("https://api.hypixel.net/findGuild?key="
                                        + HYPIXEL_API_KEY + "&byUuid=" + player.getUuid());
                                if (guildJson != null) {
                                    Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                                    if (!higherDepth(guildJson, "guild").isJsonNull()) {
                                        if (higherDepth(currentRole, "guild_id").getAsString()
                                                .equals(higherDepth(guildJson, "guild").getAsString())) {
                                            if (!guild.getMember(user).getRoles().contains(curRole)) {
                                                guild.addRoleToMember(guild.getMember(user), curRole).queue();
                                                addedRoles.append(roleChangeString(curRole.getName()));
                                            }
                                        } else {
                                            if (guild.getMember(user).getRoles().contains(curRole)) {
                                                removedRoles.append(roleChangeString(curRole.getName()));
                                                guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "sven":
                            case "rev":
                            case "tara": {
                                boolean isStackable = higherDepth(currentRole, "stackable") != null;
                                currentRole = higherDepth(currentRole, "levels");

                                int slayerPlayer = player.getSlayer(currentRoleName);
                                if (slayerPlayer == -1 && !errorRoles.toString().contains("Slayer")) {
                                    errorRoles.append(roleChangeString("Slayer API disabled"));
                                    break;
                                }

                                List<Integer> currentSlayerLevels = currentRole.getAsJsonObject().entrySet().stream()
                                        .map(i -> Integer.parseInt(i.getKey())).sorted()
                                        .collect(Collectors.toCollection(ArrayList::new));
                                Collections.reverse(currentSlayerLevels);

                                for (int i = 0; i < currentSlayerLevels.size(); i++) {
                                    long levelSlayerXp = higherDepth(
                                            higherDepth(currentRole, "" + currentSlayerLevels.get(i)), "xp")
                                            .getAsLong();
                                    if (slayerPlayer >= levelSlayerXp) {
                                        Role playerSlayerLevelRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + currentSlayerLevels.get(i)),
                                                        "id").getAsString());

                                        if (!guild.getMember(user).getRoles().contains(playerSlayerLevelRole)) {
                                            guild.addRoleToMember(guild.getMember(user), playerSlayerLevelRole).queue();
                                            addedRoles.append(roleChangeString(playerSlayerLevelRole.getName()));
                                        }

                                        if (isStackable) {
                                            currentSlayerLevels.remove(i);
                                            for (int removeLevels : currentSlayerLevels) {
                                                Role removeSlayerLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + removeLevels), "id")
                                                                .getAsString());
                                                if (guild.getMember(user).getRoles().contains(removeSlayerLevelRole)) {
                                                    guild.removeRoleFromMember(guild.getMember(user),
                                                            removeSlayerLevelRole).queue();
                                                    removedRoles
                                                            .append(roleChangeString(removeSlayerLevelRole.getName()));
                                                }
                                            }
                                        } else {
                                            List<Integer> lowerRoles = currentSlayerLevels.subList(i + 1,
                                                    currentSlayerLevels.size());
                                            for (int addLowerRole : lowerRoles) {
                                                Role removeSlayerLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + addLowerRole), "id")
                                                                .getAsString());
                                                if (!guild.getMember(user).getRoles().contains(removeSlayerLevelRole)) {
                                                    guild.addRoleToMember(guild.getMember(user), removeSlayerLevelRole)
                                                            .queue();
                                                    addedRoles
                                                            .append(roleChangeString(removeSlayerLevelRole.getName()));
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case "bank_coins": {
                                boolean isStackable = higherDepth(currentRole, "stackable") != null;
                                currentRole = higherDepth(currentRole, "levels");

                                double playerBankCoins = player.getBankBalance();
                                if (playerBankCoins == -1 && !errorRoles.toString().contains("Banking")) {
                                    errorRoles.append(roleChangeString("Banking API disabled"));
                                }

                                List<Integer> bankLevels = currentRole.getAsJsonObject().entrySet().stream()
                                        .map(i -> Integer.parseInt(i.getKey())).sorted()
                                        .collect(Collectors.toCollection(ArrayList::new));
                                Collections.reverse(bankLevels);

                                for (int i = 0; i < bankLevels.size(); i++) {
                                    long levelCoins = higherDepth(higherDepth(currentRole, "" + bankLevels.get(i)),
                                            "coins").getAsLong();
                                    if (playerBankCoins >= levelCoins) {
                                        Role playerBankLevelRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + bankLevels.get(i)), "id")
                                                        .getAsString());

                                        if (!guild.getMember(user).getRoles().contains(playerBankLevelRole)) {
                                            guild.addRoleToMember(guild.getMember(user), playerBankLevelRole).queue();
                                            addedRoles.append(roleChangeString(playerBankLevelRole.getName()));
                                        }

                                        if (isStackable) {
                                            bankLevels.remove(i);
                                            for (int removeLevels : bankLevels) {
                                                Role removeBankLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + removeLevels), "id")
                                                                .getAsString());
                                                if (guild.getMember(user).getRoles().contains(removeBankLevelRole)) {
                                                    guild.removeRoleFromMember(guild.getMember(user),
                                                            removeBankLevelRole).queue();
                                                    removedRoles
                                                            .append(roleChangeString(removeBankLevelRole.getName()));
                                                }
                                            }
                                        } else {
                                            List<Integer> lowerRoles = bankLevels.subList(i + 1, bankLevels.size());
                                            for (int addLowerRole : lowerRoles) {
                                                Role removeBankLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + addLowerRole), "id")
                                                                .getAsString());
                                                if (!guild.getMember(user).getRoles().contains(removeBankLevelRole)) {
                                                    guild.addRoleToMember(guild.getMember(user), removeBankLevelRole)
                                                            .queue();
                                                    addedRoles.append(roleChangeString(removeBankLevelRole.getName()));
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case "alchemy":
                            case "combat":
                            case "fishing":
                            case "farming":
                            case "foraging":
                            case "carpentry":
                            case "mining":
                            case "taming":
                            case "enchanting":
                            case "catacombs": {
                                boolean isStackable = higherDepth(currentRole, "stackable") != null;
                                currentRole = higherDepth(currentRole, "levels");

                                int currentSkill = -1;
                                if (currentRoleName.equals("catacombs")) {
                                    if (player.getCatacombsSkill() != null) {
                                        currentSkill = player.getCatacombsSkill().skillLevel;
                                    }
                                } else {
                                    if (player.getSkill(currentRoleName) != null) {
                                        currentSkill = player.getSkill(currentRoleName).skillLevel;
                                    }
                                }
                                if (currentSkill == -1 && !errorRoles.toString().contains("Skills")) {
                                    errorRoles.append(roleChangeString("Skills API is disabled"));
                                    break;
                                }

                                List<Integer> skillLevels = currentRole.getAsJsonObject().entrySet().stream()
                                        .map(i -> Integer.parseInt(i.getKey())).sorted()
                                        .collect(Collectors.toCollection(ArrayList::new));
                                Collections.reverse(skillLevels);
                                for (int i = 0; i < skillLevels.size(); i++) {
                                    long levelCoins = higherDepth(higherDepth(currentRole, "" + skillLevels.get(i)),
                                            "level").getAsLong();
                                    if (currentSkill >= levelCoins) {
                                        Role playerSkillLevelRole = guild.getRoleById(
                                                higherDepth(higherDepth(currentRole, "" + skillLevels.get(i)), "id")
                                                        .getAsString());

                                        if (!guild.getMember(user).getRoles().contains(playerSkillLevelRole)) {
                                            guild.addRoleToMember(guild.getMember(user), playerSkillLevelRole).queue();
                                            addedRoles.append(roleChangeString(playerSkillLevelRole.getName()));
                                        }

                                        if (isStackable) {
                                            skillLevels.remove(i);
                                            for (int removeLevels : skillLevels) {
                                                Role removeSkillLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + removeLevels), "id")
                                                                .getAsString());
                                                if (guild.getMember(user).getRoles().contains(removeSkillLevelRole)) {
                                                    guild.removeRoleFromMember(guild.getMember(user),
                                                            removeSkillLevelRole).queue();
                                                    removedRoles
                                                            .append(roleChangeString(removeSkillLevelRole.getName()));
                                                }
                                            }
                                        } else {
                                            List<Integer> lowerRoles = skillLevels.subList(i + 1, skillLevels.size());
                                            for (int addLowerRole : lowerRoles) {
                                                Role removeSkillLevelRole = guild.getRoleById(
                                                        higherDepth(higherDepth(currentRole, "" + addLowerRole), "id")
                                                                .getAsString());
                                                if (!guild.getMember(user).getRoles().contains(removeSkillLevelRole)) {
                                                    guild.addRoleToMember(guild.getMember(user), removeSkillLevelRole)
                                                            .queue();
                                                    addedRoles.append(roleChangeString(removeSkillLevelRole.getName()));
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                                break;
                            }
                            case "fairy": {
                                int fairySouls = player.getFairySouls();
                                if (fairySouls != -1) {
                                    Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                                    if (fairySouls >= higherDepth(currentRole, "souls").getAsInt()) {
                                        if (!guild.getMember(user).getRoles().contains(curRole)) {
                                            guild.addRoleToMember(guild.getMember(user), curRole).queue();

                                            addedRoles.append(roleChangeString(curRole.getName()));
                                        }
                                    } else {
                                        if (guild.getMember(user).getRoles().contains(curRole)) {
                                            removedRoles.append(roleChangeString(curRole.getName()));
                                            guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                        }
                                    }
                                }
                                break;
                            }
                            case "doom_slayer": {
                                int[] curSlayer = new int[]{player.getWolfXp(), player.getZombieXp(),
                                        player.getSpiderXp()};
                                Role curRole = guild.getRoleById(higherDepth(currentRole, "id").getAsString());
                                boolean shouldHaveDoomSlayer = false;
                                for (int curType : curSlayer) {
                                    if (curType >= 1000000) {
                                        shouldHaveDoomSlayer = true;
                                        if (!guild.getMember(user).getRoles().contains(curRole)) {
                                            guild.addRoleToMember(guild.getMember(user), curRole).queue();
                                            addedRoles.append(roleChangeString(curRole.getName()));
                                            break;
                                        }
                                    }
                                }
                                if (guild.getMember(user).getRoles().contains(curRole) && !shouldHaveDoomSlayer) {
                                    removedRoles.append(roleChangeString(curRole.getName()));
                                    guild.removeRoleFromMember(guild.getMember(user), curRole).queue();
                                }
                                break;
                            }
                            case "pet_enthusiast": {
                                JsonArray playerPets = player.getPets();
                                ArrayList<String> excludedPets = new ArrayList<>();
                                higherDepth(currentRole, "excluded_pets").getAsJsonArray()
                                        .forEach(o1 -> excludedPets.add(o1.getAsString()));

                                boolean isPetEnthusiast = false;
                                Role petEnthusiastRole = guild
                                        .getRoleById(higherDepth(currentRole, "id").getAsString());
                                for (JsonElement currentPet : playerPets) {
                                    String currentPetRarity = higherDepth(currentPet, "tier").getAsString()
                                            .toLowerCase();
                                    if (currentPetRarity.equals("epic") || currentPetRarity.equals("legendary")) {
                                        if (!excludedPets.contains(
                                                higherDepth(currentPet, "type").getAsString().toLowerCase())) {
                                            long currentPetExp = higherDepth(currentPet, "exp").getAsLong();
                                            if (player.petLevelFromXp(currentPetExp, currentPetRarity) == 100) {
                                                isPetEnthusiast = true;
                                                if (!guild.getMember(user).getRoles().contains(petEnthusiastRole)) {
                                                    guild.addRoleToMember(guild.getMember(user), petEnthusiastRole)
                                                            .queue();
                                                    addedRoles.append(roleChangeString(petEnthusiastRole.getName()));
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (guild.getMember(user).getRoles().contains(petEnthusiastRole) && !isPetEnthusiast) {
                                    removedRoles.append(roleChangeString(petEnthusiastRole.getName()));
                                    guild.removeRoleFromMember(guild.getMember(user), petEnthusiastRole).queue();
                                }
                                break;
                            }
                            case "slot_collector": {
                                Role slotCollectorRole = guild
                                        .getRoleById(higherDepth(currentRole, "id").getAsString());
                                if (player.getNumberMinionSlots() >= higherDepth(currentRole, "num_minions")
                                        .getAsInt()) {
                                    if (!guild.getMember(user).getRoles().contains(slotCollectorRole)) {
                                        guild.addRoleToMember(guild.getMember(user), slotCollectorRole).queue();
                                        addedRoles.append(roleChangeString(slotCollectorRole.getName()));
                                    }
                                } else if (guild.getMember(user).getRoles().contains(slotCollectorRole)) {
                                    removedRoles.append(roleChangeString(slotCollectorRole.getName()));
                                    guild.removeRoleFromMember(guild.getMember(user), slotCollectorRole).queue();
                                }
                                break;
                            }
                        }
                    }
                    eb.setDescription("**Added Roles:**\n"
                            + (addedRoles.length() > 0 ? addedRoles.toString() : "• None\n") + "\n**Removed Roles:**\n"
                            + (removedRoles.length() > 0 ? removedRoles.toString() : "• None"));
                    if (errorRoles.length() > 0) {
                        eb.addField("Disabled APIs:", errorRoles.toString(), false);
                    }
                    eb.setTimestamp(Instant.now());
                } else {
                    eb = defaultEmbed("Error fetching server's settings", null);
                }
            } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
                eb = defaultEmbed("Error fetching data", null);
            }
        } else {
            eb = defaultEmbed(errorMessage(this.name), null);
            eb.setDescription("");
            ebMessage.editMessage(eb.build()).queue();
            return;

        }
        ebMessage.editMessage(eb.build()).queue();
    }

    private String roleChangeString(String name) {
        return "• " + name + "\n";
    }

    private DiscordStruct getPlayerInfo(String username) {
        JsonElement playerJson = getJson("https://api.hypixel.net/player?key=" + HYPIXEL_API_KEY + "&name=" + username);

        if (playerJson == null) {
            return null;
        }

        if (higherDepth(playerJson, "player").isJsonNull()) {
            return null;
        }
        try {
            String discordID = higherDepth(
                    higherDepth(higherDepth(higherDepth(playerJson, "player"), "socialMedia"), "links"), "DISCORD")
                    .getAsString();
            return new DiscordStruct(discordID,
                    higherDepth(higherDepth(playerJson, "player"), "displayname").getAsString());
        } catch (Exception e) {
            return null;
        }
    }
}