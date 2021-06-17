package com.skyblockplus.miscellaneous;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Player;
import com.skyblockplus.utils.structs.DiscordInfoStruct;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

public class RoleCommands extends Command {

	public RoleCommands() {
		this.name = "roles";
		this.cooldown = 10;
		this.aliases = new String[] { "role" };
	}

	public static EmbedBuilder updateRoles(String profile, Guild guild, User user, Member member) {
		EmbedBuilder eb;

		if (database.getLinkedUserByDiscordId(user.getId()).isJsonNull()) {
			return defaultEmbed("You must be linked to run this command. Use `" + BOT_PREFIX + "link [IGN]` to link");
		}

		JsonElement linkedInfo = database.getLinkedUserByDiscordId(user.getId());
		DiscordInfoStruct playerInfo = getPlayerDiscordInfo(higherDepth(linkedInfo, "minecraftUuid").getAsString());

		if (playerInfo == null) {
			eb = defaultEmbed("Discord tag error");
			eb.setDescription("Unable to get Discord tag linked with Hypixel account");
			return eb;
		}

		if (!user.getAsTag().equals(playerInfo.discordTag)) {
			eb = defaultEmbed("Discord tag mismatch");
			eb.setDescription(
				"Account **" +
				playerInfo.minecraftUsername +
				"** is linked with the discord tag `" +
				playerInfo.discordTag +
				"`\nYour current discord tag is `" +
				user.getAsTag() +
				"`"
			);
			return eb;
		}

		String username = playerInfo.minecraftUsername;
		Player player = profile == null ? new Player(username) : new Player(username, profile);
		if (!player.isValid()) {
			eb = defaultEmbed("Error fetching data");
			eb.setDescription("**Please check linked username or given profile**");
			eb.appendDescription("\nFormat for command is `" + BOT_PREFIX + "roles claim <profile>`");
			return eb;
		}

		try {
			JsonElement rolesJson = database.getRolesSettings(guild.getId());
			if (rolesJson != null) {
				if ((higherDepth(rolesJson, "enable") == null) || !higherDepth(rolesJson, "enable").getAsBoolean()) {
					eb = defaultEmbed("Automatic roles not enabled for this server");
					return eb;
				}
				List<String> rolesID = getJsonKeys(rolesJson);
				rolesID.remove("enable");
				Role botRole = guild.getBotRole();

				eb = player.defaultPlayerEmbed();
				StringBuilder addedRoles = new StringBuilder();
				StringBuilder removedRoles = new StringBuilder();
				StringBuilder disabledAPI = new StringBuilder();
				StringBuilder errorRoles = new StringBuilder();

				List<Role> toAdd = new ArrayList<>();
				List<Role> toRemove = new ArrayList<>();
				JsonElement guildJson = null;

				for (String currentRoleName : rolesID) {
					JsonElement currentRole = higherDepth(rolesJson, currentRoleName);
					if ((higherDepth(currentRole, "enable") == null) || !higherDepth(currentRole, "enable").getAsBoolean()) {
						continue;
					}

					switch (currentRoleName) {
						case "guild_member":
							{
								if (guildJson == null) {
									guildJson =
										getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + player.getUuid());
								}

								if (guildJson != null && !higherDepth(guildJson, "guild").isJsonNull()) {
									JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();
									String playerGuildId = higherDepth(guildJson, "guild._id").getAsString();

									for (JsonElement currentLevel : levelsArray) {
										String currentLevelValue = higherDepth(currentLevel, "value").getAsString();
										Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());
										if (playerGuildId.equals(currentLevelValue)) {
											if (!member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													toAdd.add(currentLevelRole);
													addedRoles.append(roleChangeString(currentLevelRole.getName()));
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}
										} else {
											if (member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													removedRoles.append(roleChangeString(currentLevelRole.getName()));
													toRemove.add(currentLevelRole);
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}
										}
									}
								}
								break;
							}
						case "guild_ranks":
							{
								if (guildJson == null) {
									guildJson =
										getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&player=" + player.getUuid());
								}

								if (guildJson != null && !higherDepth(guildJson, "guild").isJsonNull()) {
									JsonArray curLevels = higherDepth(currentRole, "levels").getAsJsonArray();
									List<JsonElement> guildRoles = new ArrayList<>();
									for (JsonElement curLevel : curLevels) {
										guildRoles.add(
											database.getGuildRoleSettings(guild.getId(), higherDepth(curLevel, "value").getAsString())
										);
									}

									for (JsonElement guildRoleSettings : guildRoles) {
										if (
											higherDepth(guildRoleSettings, "guildId")
												.getAsString()
												.equals(higherDepth(guildJson, "guild._id").getAsString())
										) {
											JsonArray guildRanks = higherDepth(guildRoleSettings, "guildRanks").getAsJsonArray();

											JsonArray guildMembers = higherDepth(guildJson, "guild.members").getAsJsonArray();

											for (JsonElement guildMember : guildMembers) {
												if (higherDepth(guildMember, "uuid").getAsString().equals(player.getUuid())) {
													String guildMemberRank = higherDepth(guildMember, "rank")
														.getAsString()
														.replace(" ", "_");

													for (JsonElement guildRank : guildRanks) {
														Role currentLevelRole = guild.getRoleById(
															higherDepth(guildRank, "discordRoleId").getAsString()
														);
														if (
															higherDepth(guildRank, "minecraftRoleName")
																.getAsString()
																.equalsIgnoreCase(guildMemberRank)
														) {
															if (!member.getRoles().contains(currentLevelRole)) {
																if (botRole.canInteract(currentLevelRole)) {
																	toAdd.add(currentLevelRole);
																	addedRoles.append(roleChangeString(currentLevelRole.getName()));
																} else {
																	errorRoles.append(roleChangeString(currentLevelRole.getName()));
																}
															}
														} else {
															if (member.getRoles().contains(currentLevelRole)) {
																if (botRole.canInteract(currentLevelRole)) {
																	removedRoles.append(roleChangeString(currentLevelRole.getName()));
																	toRemove.add(currentLevelRole);
																} else {
																	errorRoles.append(roleChangeString(currentLevelRole.getName()));
																}
															}
														}
													}
													break;
												}
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
						case "skill_average":
						case "pet_score":
						case "dungeon_secrets":
						case "slot_collector":
						case "enderman":
							{
								double roleAmount = -1;
								switch (currentRoleName) {
									case "sven":
									case "rev":
									case "tara":
									case "enderman":
										{
											roleAmount = player.getSlayer(currentRoleName);
											break;
										}
									case "bank_coins":
										{
											roleAmount = player.getBankBalance();
											if (roleAmount == -1 && !disabledAPI.toString().contains("Banking")) {
												disabledAPI.append(roleChangeString("Banking API disabled"));
												continue;
											}
											break;
										}
									case "skill_average":
										{
											roleAmount = player.getSkillAverage();
											if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
												disabledAPI.append(roleChangeString("Skills API disabled"));
												continue;
											}
											break;
										}
									case "pet_score":
										{
											roleAmount = player.getPetScore();
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
										{
											if (player.getSkill(currentRoleName) != null) {
												roleAmount = player.getSkill(currentRoleName).skillLevel;
											}
											if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
												disabledAPI.append(roleChangeString("Skills API disabled"));
												continue;
											}
											break;
										}
									case "catacombs":
										{
											if (player.getCatacombsSkill() != null) {
												roleAmount = player.getCatacombsSkill().skillLevel;
											}
											if (roleAmount == -1 && !disabledAPI.toString().contains("Skills")) {
												disabledAPI.append(roleChangeString("Skills API disabled"));
												continue;
											}
											break;
										}
									case "fairy_souls":
										{
											roleAmount = player.getFairySouls();
											if (roleAmount == -1) {
												continue;
											}
											break;
										}
									case "slot_collector":
										{
											roleAmount = player.getNumberMinionSlots();
											break;
										}
									case "dungeon_secrets":
										roleAmount = player.getDungeonSecrets();
										break;
									default:
										{
											continue;
										}
								}

								JsonArray levelsArray = higherDepth(currentRole, "levels").getAsJsonArray();

								if (higherDepth(currentRole, "stackable").getAsBoolean()) {
									for (JsonElement currentLevel : levelsArray) {
										int currentLevelValue = higherDepth(currentLevel, "value").getAsInt();
										Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());

										if (roleAmount >= currentLevelValue) {
											if (!member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													toAdd.add(currentLevelRole);
													addedRoles.append(roleChangeString(currentLevelRole.getName()));
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}
										} else {
											if (member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													toRemove.add(currentLevelRole);
													removedRoles.append(roleChangeString(currentLevelRole.getName()));
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}
										}
									}
								} else {
									for (int i = levelsArray.size() - 1; i >= 0; i--) {
										JsonElement currentLevel = levelsArray.get(i);

										int currentLevelValue = higherDepth(currentLevel, "value").getAsInt();
										Role currentLevelRole = guild.getRoleById(higherDepth(currentLevel, "roleId").getAsString());

										if (roleAmount < currentLevelValue) {
											if (member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													toRemove.add(currentLevelRole);
													removedRoles.append(roleChangeString(currentLevelRole.getName()));
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}
										} else {
											if (!member.getRoles().contains(currentLevelRole)) {
												if (botRole.canInteract(currentLevelRole)) {
													toAdd.add(currentLevelRole);
													addedRoles.append(roleChangeString(currentLevelRole.getName()));
												} else {
													errorRoles.append(roleChangeString(currentLevelRole.getName()));
												}
											}

											for (int j = i - 1; j >= 0; j--) {
												JsonElement currentLevelRemoveStackable = levelsArray.get(j);
												Role currentLevelRoleRemoveStackable = guild.getRoleById(
													higherDepth(currentLevelRemoveStackable, "roleId").getAsString()
												);

												if (member.getRoles().contains(currentLevelRoleRemoveStackable)) {
													if (botRole.canInteract(currentLevelRole)) {
														toRemove.add(currentLevelRoleRemoveStackable);
														removedRoles.append(roleChangeString(currentLevelRoleRemoveStackable.getName()));
													} else {
														errorRoles.append(roleChangeString(currentLevelRoleRemoveStackable.getName()));
													}
												}
											}
											break;
										}
									}
									break;
								}
							}
						case "doom_slayer":
							{
								Role curRole = guild.getRoleById(
									higherDepth(higherDepth(currentRole, "levels").getAsJsonArray().get(0), "roleId").getAsString()
								);

								if (
									(player.getSlayer("sven") >= 1000000) ||
									(player.getSlayer("rev") >= 1000000) ||
									(player.getSlayer("tara") >= 1000000)
								) {
									if (!member.getRoles().contains(curRole)) {
										if (botRole.canInteract(curRole)) {
											toAdd.add(curRole);
											addedRoles.append(roleChangeString(curRole.getName()));
										} else {
											errorRoles.append(roleChangeString(curRole.getName()));
										}
									}
								} else {
									if (member.getRoles().contains(curRole)) {
										if (botRole.canInteract(curRole)) {
											removedRoles.append(roleChangeString(curRole.getName()));
											toRemove.add(curRole);
										} else {
											errorRoles.append(roleChangeString(curRole.getName()));
										}
									}
								}
								break;
							}
						case "all_slayer_nine":
							{
								Role curRole = guild.getRoleById(
									higherDepth(higherDepth(currentRole, "levels").getAsJsonArray().get(0), "roleId").getAsString()
								);

								if (
									(player.getSlayer("sven") >= 1000000) &&
									(player.getSlayer("rev") >= 1000000) &&
									(player.getSlayer("tara") >= 1000000)
								) {
									if (!member.getRoles().contains(curRole)) {
										if (botRole.canInteract(curRole)) {
											toAdd.add(curRole);
											addedRoles.append(roleChangeString(curRole.getName()));
										} else {
											errorRoles.append(roleChangeString(curRole.getName()));
										}
									}
								} else {
									if (member.getRoles().contains(curRole)) {
										if (botRole.canInteract(curRole)) {
											removedRoles.append(roleChangeString(curRole.getName()));
											toRemove.add(curRole);
										} else {
											errorRoles.append(roleChangeString(curRole.getName()));
										}
									}
								}
								break;
							}
						case "pet_enthusiast":
							{
								JsonArray playerPets = player.getPets();
								ArrayList<String> excludedPets = new ArrayList<>();
								excludedPets.add("guardian");
								excludedPets.add("jellyfish");
								excludedPets.add("parrot");
								excludedPets.add("sheep");

								boolean isPetEnthusiast = false;
								Role petEnthusiastRole = guild.getRoleById(
									higherDepth(higherDepth(currentRole, "levels").getAsJsonArray().get(0), "roleId").getAsString()
								);
								for (JsonElement currentPet : playerPets) {
									String currentPetRarity = higherDepth(currentPet, "tier").getAsString().toLowerCase();
									if (currentPetRarity.equals("epic") || currentPetRarity.equals("legendary")) {
										if (!excludedPets.contains(higherDepth(currentPet, "type").getAsString().toLowerCase())) {
											long currentPetExp = higherDepth(currentPet, "exp").getAsLong();
											if (player.petLevelFromXp(currentPetExp, currentPetRarity) == 100) {
												isPetEnthusiast = true;
												if (!member.getRoles().contains(petEnthusiastRole)) {
													if (botRole.canInteract(petEnthusiastRole)) {
														toAdd.add(petEnthusiastRole);
														addedRoles.append(roleChangeString(petEnthusiastRole.getName()));
													} else {
														errorRoles.append(roleChangeString(petEnthusiastRole.getName()));
													}
													break;
												}
											}
										}
									}
								}
								if (member.getRoles().contains(petEnthusiastRole) && !isPetEnthusiast) {
									if (botRole.canInteract(petEnthusiastRole)) {
										removedRoles.append(roleChangeString(petEnthusiastRole.getName()));
										toRemove.add(petEnthusiastRole);
									} else {
										errorRoles.append(roleChangeString(petEnthusiastRole.getName()));
									}
								}
								break;
							}
					}
				}
				eb.setDescription(
					"**Added Roles (" +
					toAdd.size() +
					")**\n" +
					(addedRoles.length() > 0 ? addedRoles.toString() : "• None\n") +
					"\n**Removed Roles (" +
					toRemove.size() +
					")**\n" +
					(removedRoles.length() > 0 ? removedRoles.toString() : "• None")
				);
				if (disabledAPI.length() > 0) {
					eb.addField("Disabled APIs:", disabledAPI.toString(), false);
				}

				if (errorRoles.length() > 0) {
					eb.addField("Error giving roles:", errorRoles.toString(), false);
				}

				guild.modifyMemberRoles(member, toAdd, toRemove).queue();
			} else {
				eb = defaultEmbed("Error fetching server's settings");
			}
		} catch (JsonIOException | JsonSyntaxException e) {
			eb = defaultEmbed("Error fetching data");
		}

		return eb;
	}

	private static String roleChangeString(String name) {
		return "• " + name + "\n";
	}

	@Override
	protected void execute(CommandEvent event) {
		new Thread(
			() -> {
				EmbedBuilder eb = loadingEmbed();
				String content = event.getMessage().getContentRaw();
				MessageChannel channel = event.getChannel();
				Message ebMessage = channel.sendMessage(eb.build()).complete();

				logCommand(event.getGuild(), event.getAuthor(), content);

				String[] args = content.split(" ");
				if (args.length < 2 || args.length > 3) {
					ebMessage.editMessage(errorMessage(this.name).build()).queue();
					return;
				}

				if (!args[1].equals("claim")) {
					ebMessage.editMessage(errorMessage(this.name).build()).queue();
					return;
				}

				eb = updateRoles(args.length == 3 ? args[2] : null, event.getGuild(), event.getAuthor(), event.getMember());

				ebMessage.editMessage(eb.build()).queue();
			}
		)
			.start();
	}
}
