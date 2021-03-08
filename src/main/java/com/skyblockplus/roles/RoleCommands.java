package com.skyblockplus.roles;

import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static com.skyblockplus.utils.Utils.*;

public class RoleCommands extends Command {

    public RoleCommands() {
        this.name = "roles";
        this.cooldown = 10;
    }

    @Override
    protected void execute(CommandEvent event) {
        EmbedBuilder eb = loadingEmbed();
        String content = event.getMessage().getContentRaw();
        MessageChannel channel = event.getChannel();
        Guild guild = event.getGuild();
        User user = event.getAuthor();
        Member member = guild.getMemberById(user.getId());
        Message ebMessage = channel.sendMessage(eb.build()).complete();

        String[] args = content.split(" ");
        if (args.length < 3 || args.length > 4) {
            ebMessage.editMessage(errorMessage(this.name).build()).queue();
            return;
        }

        logCommand(event.getGuild(), event.getAuthor(), content);

        if (getPlayerInfo(args[2]) == null) {
            eb = defaultEmbed("Discord tag mismatch");
            eb.setDescription("Unable to get Discord tag linked with Hypixel account");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }
        DiscordStruct playerInfo = getPlayerInfo(args[2]);

        if (!user.getAsTag().equals(playerInfo.discordTag)) {
            eb = defaultEmbed("Discord tag mismatch");
            eb.setDescription("Account **" + playerInfo.username + "** is linked with the discord tag `"
                    + playerInfo.discordTag + "`\nYour current discord tag is `" + user.getAsTag() + "`");
            ebMessage.editMessage(eb.build()).queue();
            return;
        }

        String username = playerInfo.username;
        String profileName = args.length == 4 ? args[3] : null;
        Player player = profileName == null ? new Player(username) : new Player(username, profileName);
        if (!player.isValid()) {
            eb = defaultEmbed("Error fetching data");
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
                        eb = defaultEmbed("Automatic roles not enabled for this server");
                        ebMessage.editMessage(eb.build()).queue();
                        return;
                    }
                    List<String> rolesID = getJsonKeys(rolesJson);
                    rolesID.remove("enable");

                    eb = player.defaultPlayerEmbed();
                    StringBuilder addedRoles = new StringBuilder();
                    StringBuilder removedRoles = new StringBuilder();
                    StringBuilder errorRoles = new StringBuilder();

                    for (String currentRoleName : rolesID) {
                        JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
                        if (!higherDepth(currentRole, "enable").getAsBoolean()) {
                            continue;
                        }

                        switch (currentRoleName) {
                            case "guild_member": {
                                JsonElement guildJson = getJson("https://api.hypixel.net/findGuild?key="
                                        + HYPIXEL_API_KEY + "&byUuid=" + player.getUuid());
                                if (guildJson != null && !higherDepth(guildJson, "guild").isJsonNull()) {
                                    JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
                                    String playerGuildId = higherDepth(guildJson, "guild").getAsString();

                                    for (JsonElement currentLevel : levelsArray) {
                                        String currentLevelValue = higherDepth(currentLevel, "value").getAsString();
                                        Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
                                        if (playerGuildId.equals(currentLevelValue)) {
                                            if (!guild.getMember(user).getRoles().contains(currentLevelRole)) {
                                                guild.addRoleToMember(guild.getMember(user), currentLevelRole).queue();
                                                addedRoles.append(roleChangeString(currentLevelRole.getName()));
                                            }
                                        } else {
                                            if (guild.getMember(user).getRoles().contains(currentLevelRole)) {
                                                removedRoles.append(roleChangeString(currentLevelRole.getName()));
                                                guild.removeRoleFromMember(guild.getMember(user), currentLevelRole).queue();
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "sven":
                            case "rev":
                            case "tara":
                            case "bank_coins":
                            case "alchemy":
                            case "combat":
                            case "fishing":
                            case "farming":
                            case "foraging":
                            case "carpentry":
                            case "mining":
                            case "taming":
                            case "enchanting":
                            case "catacombs":
                            case "fairy_souls":
                            case "slot_collector": {
                                double roleAmount = -1;
                                switch (currentRoleName) {
                                    case "sven":
                                    case "rev":
                                    case "tara": {
                                        roleAmount = player.getSlayer(currentRoleName);
                                        break;
                                    }
                                    case "bank_coins": {
                                        roleAmount = player.getBankBalance();
                                        if (roleAmount == -1 && !errorRoles.toString().contains("Banking")) {
                                            errorRoles.append(roleChangeString("Banking API disabled"));
                                            continue;
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
                                    case "enchanting": {
                                        if (player.getSkill(currentRoleName) != null) {
                                            roleAmount = player.getSkill(currentRoleName).skillLevel;
                                        }
                                        if (roleAmount == -1 && !errorRoles.toString().contains("Skills")) {
                                            errorRoles.append(roleChangeString("Skills API disabled"));
                                            continue;
                                        }
                                        break;
                                    }
                                    case "catacombs": {
                                        if (player.getCatacombsSkill() != null) {
                                            roleAmount = player.getCatacombsSkill().skillLevel;
                                        }
                                        if (roleAmount == -1 && !errorRoles.toString().contains("Skills")) {
                                            errorRoles.append(roleChangeString("Skills API disabled"));
                                            continue;
                                        }
                                        break;
                                    }
                                    case "fairy_souls": {
                                        roleAmount = player.getFairySouls();
                                        if (roleAmount == -1) {
                                            continue;
                                        }
                                        break;
                                    }
                                    case "slot_collector": {
                                        roleAmount = player.getNumberMinionSlots();
                                        break;
                                    }
                                    default: {
                                        continue;
                                    }
                                }

                                JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();

                                if (higherDepth(currentRole, "stackable").getAsBoolean()) {
                                    for (JsonElement currentLevel : levelsArray) {
                                        int currentLevelValue = higherDepth(currentLevel, "value").getAsInt();
                                        Role currentLevelRole = event.getGuild().getRoleById(higherDepth(currentLevel, "roleId").getAsString());

                                        if (roleAmount >= currentLevelValue) {
                                            if (!member.getRoles().contains(currentLevelRole)) {
                                                guild.addRoleToMember(member, currentLevelRole).queue();
                                                addedRoles.append(roleChangeString(currentLevelRole.getName()));
                                            }
                                        } else {
                                            if (member.getRoles().contains(currentLevelRole)) {
                                                guild.removeRoleFromMember(member, currentLevelRole).queue();
                                                removedRoles.append(roleChangeString(currentLevelRole.getName()));
                                            }
                                        }
                                    }
                                } else {
                                    for (int i = levelsArray.size() - 1; i >= 0; i--) {
                                        JsonElement currentLevel = levelsArray.get(i);

                                        int currentLevelValue = higherDepth(currentLevel, "value").getAsInt();
                                        Role currentLevelRole = event.getGuild().getRoleById(higherDepth(currentLevel, "roleId").getAsString());

                                        if (roleAmount < currentLevelValue) {
                                            if (member.getRoles().contains(currentLevelRole)) {
                                                guild.removeRoleFromMember(member, currentLevelRole).queue();
                                                removedRoles.append(roleChangeString(currentLevelRole.getName()));
                                            }
                                        } else {
                                            if (!member.getRoles().contains(currentLevelRole)) {
                                                guild.addRoleToMember(member, currentLevelRole).queue();
                                                addedRoles.append(roleChangeString(currentLevelRole.getName()));
                                            }

                                            for (int j = i - 2; j >= 0; j--) {
                                                JsonElement currentLevelRemoveStackable = levelsArray.get(j);
                                                Role currentLevelRoleRemoveStackable = event.getGuild().getRoleById(higherDepth(currentLevelRemoveStackable, "roleId").getAsString());

                                                if (member.getRoles().contains(currentLevelRoleRemoveStackable)) {
                                                    guild.removeRoleFromMember(member, currentLevelRoleRemoveStackable).queue();
                                                    removedRoles.append(roleChangeString(currentLevelRoleRemoveStackable.getName()));
                                                }
                                            }
                                            break;
                                        }
                                    }
                                    break;
                                }
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
                        }
                    }
                    eb.setDescription("**Added Roles:**\n"
                            + (addedRoles.length() > 0 ? addedRoles.toString() : "• None\n") + "\n**Removed Roles:**\n"
                            + (removedRoles.length() > 0 ? removedRoles.toString() : "• None"));
                    if (errorRoles.length() > 0) {
                        eb.addField("Disabled APIs:", errorRoles.toString(), false);
                    }
                } else {
                    eb = defaultEmbed("Error fetching server's settings");
                }
            } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
                eb = defaultEmbed("Error fetching data");
            }
        } else {
            ebMessage.editMessage(errorMessage(this.name).build()).queue();
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