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

package com.skyblockplus.utils.utils;

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HttpUtils.*;
import static com.skyblockplus.utils.utils.HypixelUtils.getNpcSellPrice;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.features.apply.ApplyGuild;
import com.skyblockplus.features.apply.ApplyUser;
import com.skyblockplus.features.listeners.AutomaticGuild;
import com.skyblockplus.utils.ExposeExclusionStrategy;
import com.skyblockplus.utils.command.CustomPaginator;
import com.skyblockplus.utils.command.SlashCommandClient;
import com.skyblockplus.utils.database.Database;
import com.skyblockplus.utils.exceptionhandler.ExceptionExecutor;
import com.skyblockplus.utils.exceptionhandler.ExceptionScheduler;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.oauth.OAuthClient;
import com.skyblockplus.utils.structs.HypixelKeyRecord;
import com.skyblockplus.utils.structs.InvItem;
import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import me.nullicorn.nedit.type.TagType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

public class Utils {

	public static final int GLOBAL_COOLDOWN = 3;
	public static final String DISCORD_SERVER_INVITE_LINK = "https://discord.gg/Z4Fn3eNDXT";
	public static final String BOT_INVITE_LINK =
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=395541081169&scope=bot%20applications.commands";
	public static final String FORUM_POST_LINK = "https://hypixel.net/threads/3980092";
	public static final ExecutorService executor = new ExceptionExecutor(
		10,
		Integer.MAX_VALUE,
		15L,
		TimeUnit.SECONDS,
		new SynchronousQueue<>()
	);
	public static final ScheduledExecutorService scheduler = new ExceptionScheduler(7);
	public static final ExceptionExecutor playerRequestExecutor = new ExceptionExecutor(
		3,
		3,
		45L,
		TimeUnit.SECONDS,
		new LinkedBlockingQueue<>()
	)
		.setAllowCoreThreadTimeOut(true);
	public static final ExceptionExecutor leaderboardDbInsertQueue = new ExceptionExecutor(
		20,
		20,
		45L,
		TimeUnit.SECONDS,
		new LinkedBlockingQueue<>()
	)
		.setAllowCoreThreadTimeOut(true);
	public static final ConcurrentHashMap<String, HypixelKeyRecord> keyCooldownMap = new ConcurrentHashMap<>();
	public static final List<String> hypixelGuildQueue = Collections.synchronizedList(new ArrayList<>());
	public static final Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExposeExclusionStrategy()).create();
	public static final Gson formattedGson = new GsonBuilder()
		.setPrettyPrinting()
		.addSerializationExclusionStrategy(new ExposeExclusionStrategy())
		.create();
	public static final Consumer<Object> ignore = ignored -> {};
	public static final AtomicInteger remainingLimit = new AtomicInteger(240);
	public static final AtomicInteger timeTillReset = new AtomicInteger(0);
	public static final Pattern nicknameTemplatePattern = Pattern.compile("\\[(GUILD|PLAYER)\\.(\\w+)(?:\\.\\{(.*?)})?]");
	public static final JDAWebhookClient botStatusWebhook = new WebhookClientBuilder(
		"https://discord.com/api/webhooks/957659234827374602/HLXDdqX5XMaH2ZDX5HRHifQ6i71ISoCNcwVmwPQCyCvbKv2l0Q7NLj_lmzwfs4mdcOM1"
	)
		.setExecutorService(scheduler)
		.setHttpClient(okHttpClient)
		.buildJDA();
	/* Constants */
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	private static final Color BOT_COLOR = new Color(223, 5, 5);
	private static final Pattern neuTexturePattern = Pattern.compile("Properties:\\{textures:\\[0:\\{Value:\"(.*)\"}]}");
	/* Environment */
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String DATABASE_URL = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String GITHUB_TOKEN = "";
	public static String DEFAULT_PREFIX = "";
	public static String AUCTION_API_KEY = "";
	public static String PLANET_SCALE_URL = "";
	public static String SBZ_SCAMMER_DB_KEY = "";
	public static String LEADERBOARD_DB_URL = "";
	public static String CLIENT_SECRET = "";
	public static String BASE_URL = "";
	public static TextChannel errorLogChannel;
	public static ShardManager jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;
	public static CommandClient client;
	public static SlashCommandClient slashCommandClient;
	public static OAuthClient oAuthClient;
	public static JsonObject allServerSettings;
	public static ConfigurableApplicationContext springContext;
	public static String selfUserId;
	/* Miscellaneous */
	private static TextChannel botLogChannel;
	private static TextChannel networthBugReportChannel;
	private static Instant userCountLastUpdated = Instant.now();
	private static int userCount = -1;

	/* Logging */
	public static void logCommand(Guild guild, User user, String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);

		if (guild != null) {
			eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
		}

		if (commandInput.length() > 1024) {
			eb.addField(user.getName() + " (" + user.getId() + ")", makeHastePost(commandInput), false);
		} else {
			eb.addField(user.getName() + " (" + user.getId() + ")", "`" + commandInput + "`", false);
		}

		botLogChannel.sendMessageEmbeds(eb.build()).queue();
	}

	public static void logCommand(Guild guild, String commandInput) {
		System.out.println(commandInput);

		if (botLogChannel == null) {
			botLogChannel = jda.getGuildById("796790757947867156").getTextChannelById("818469899848515624");
		}

		EmbedBuilder eb = defaultEmbed(null);
		eb.setAuthor(guild.getName() + " (" + guild.getId() + ")", null, guild.getIconUrl());
		eb.setDescription(commandInput);
		botLogChannel.sendMessageEmbeds(eb.build()).queue();
	}

	/* Embeds and paginators */
	public static EmbedBuilder defaultEmbed(String title, String titleUrl) {
		EmbedBuilder eb = new EmbedBuilder()
			.setColor(BOT_COLOR)
			.setFooter("By CrypticPlasma • dsc.gg/sb+", null)
			.setTimestamp(Instant.now());
		if (titleUrl != null && titleUrl.length() <= MessageEmbed.URL_MAX_LENGTH && EmbedBuilder.URL_PATTERN.matcher(titleUrl).matches()) {
			eb.setTitle(title, titleUrl);
		} else {
			eb.setTitle(title);
		}
		return eb;
	}

	public static EmbedBuilder defaultEmbed(String title) {
		return defaultEmbed(title, null);
	}

	public static EmbedBuilder errorEmbed(String failCause) {
		return defaultEmbed("Error").setDescription(failCause);
	}

	public static EmbedBuilder loadingEmbed() {
		return defaultEmbed(null).setImage("https://cdn.discordapp.com/attachments/803419567958392832/825768516636508160/sb_loading.gif");
	}

	public static CustomPaginator.Builder defaultPaginator(User... eventAuthor) {
		return new CustomPaginator.Builder()
			.setEventWaiter(waiter)
			.setColumns(1)
			.setItemsPerPage(1)
			.setFinalAction(m -> {
				if (!m.getActionRows().isEmpty()) {
					List<Button> buttons = m
						.getButtons()
						.stream()
						.filter(b -> b.getStyle() == ButtonStyle.LINK)
						.collect(Collectors.toCollection(ArrayList::new));
					if (buttons.isEmpty()) {
						m.editMessageComponents().queue(ignore, ignore);
					} else {
						m.editMessageComponents(ActionRow.of(buttons)).queue(ignore, ignore);
					}
				}
			})
			.setTimeout(1, TimeUnit.MINUTES)
			.setColor(BOT_COLOR)
			.setUsers(eventAuthor);
	}

	public static void initialize() {
		try (FileInputStream fs = new FileInputStream("DevSettings.properties")) {
			Properties appProps = new Properties();
			appProps.load(fs);
			HYPIXEL_API_KEY = (String) appProps.get("HYPIXEL_API_KEY");
			BOT_TOKEN = (String) appProps.get("BOT_TOKEN");
			DATABASE_URL = ((String) appProps.get("DATABASE_URL"));
			GITHUB_TOKEN = (String) appProps.get("GITHUB_TOKEN");
			API_USERNAME = (String) appProps.get("API_USERNAME");
			API_PASSWORD = (String) appProps.get("API_PASSWORD");
			DEFAULT_PREFIX = (String) appProps.get("DEFAULT_PREFIX");
			AUCTION_API_KEY = (String) appProps.get("AUCTION_API_KEY");
			PLANET_SCALE_URL = (String) appProps.get("PLANET_SCALE_URL");
			SBZ_SCAMMER_DB_KEY = (String) appProps.get("SBZ_SCAMMER_DB_KEY");
			LEADERBOARD_DB_URL = (String) appProps.get("LEADERBOARD_DB_URL");
			CLIENT_SECRET = (String) appProps.get("CLIENT_SECRET");
			BASE_URL = (String) appProps.get("BASE_URL");
		} catch (IOException e) {
			HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
			BOT_TOKEN = System.getenv("BOT_TOKEN");
			DATABASE_URL = System.getenv("DATABASE_URL");
			GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
			API_USERNAME = System.getenv("API_USERNAME");
			API_PASSWORD = System.getenv("API_PASSWORD");
			DEFAULT_PREFIX = System.getenv("DEFAULT_PREFIX");
			AUCTION_API_KEY = System.getenv("AUCTION_API_KEY");
			PLANET_SCALE_URL = System.getenv("PLANET_SCALE_URL");
			SBZ_SCAMMER_DB_KEY = System.getenv("SBZ_SCAMMER_DB_KEY");
			LEADERBOARD_DB_URL = System.getenv("LEADERBOARD_DB_URL");
			CLIENT_SECRET = System.getenv("CLIENT_SECRET");
			BASE_URL = System.getenv("BASE_URL");
		}
	}

	public static String getEmojiWithName(String id, String name) {
		return getEmoji(id).replaceAll("(?<before>:).*(?<after>:)", "${before}" + name + "${after}");
	}

	public static String getEmoji(String id) {
		return getEmoji(id, "");
	}

	public static String getEmoji(String id, String defaultValue) {
		return higherDepth(getEmojiMap(), id, defaultValue);
	}

	public static Emoji getEmojiObj(String id) {
		return Emoji.fromFormatted(getEmoji(id, null));
	}

	public static String getHasteUrl() {
		return "https://haste.skyblock-plus.ml/api/";
	}

	public static String makeHastePost(Object body) {
		try {
			return getHasteUrl() + higherDepth(postUrl(getHasteUrl() + "?key=cab35a7a9b1242beeaf0e6dfb69404d5", body), "key").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	/* Miscellaneous */
	public static TextChannel getNetworthBugReportChannel() {
		if (networthBugReportChannel == null) {
			networthBugReportChannel = jda.getGuildById("796790757947867156").getTextChannelById("1017573342288564264");
		}
		return networthBugReportChannel;
	}

	/**
	 * @return scammer JSON only if marked as scammer otherwise null
	 */
	public static JsonElement getScammerJson(String uuid) {
		JsonElement scammerJson = getJson("https://jerry.robothanzo.dev/v1/scammers/" + stringToUuid(uuid) + "?key=" + SBZ_SCAMMER_DB_KEY);
		return higherDepth(scammerJson, "scammer", false) ? scammerJson : null;
	}

	public static EmbedBuilder checkHypixelKey(String hypixelKey) {
		return checkHypixelKey(hypixelKey, true);
	}

	public static EmbedBuilder checkHypixelKey(String hypixelKey, boolean checkRatelimit) {
		if (hypixelKey == null) {
			return errorEmbed("You must set a valid Hypixel API key to use this feature or command");
		}

		try {
			HttpGet httpGet = new HttpGet("https://api.hypixel.net/key?key=" + hypixelKey);
			httpGet.addHeader("content-type", "application/json; charset=UTF-8");

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {
				int remainingLimit = Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Remaining").getValue());
				int timeTillReset = Integer.parseInt(httpResponse.getFirstHeader("RateLimit-Reset").getValue());
				if (checkRatelimit && remainingLimit < 10) {
					return errorEmbed(
						"That command is on cooldown for " + timeTillReset + " more second" + (timeTillReset == 1 ? "" : "s")
					);
				}

				try (InputStreamReader in = new InputStreamReader(httpResponse.getEntity().getContent())) {
					higherDepth(JsonParser.parseReader(in), "record.key").getAsString();
				}

				keyCooldownMap.put(hypixelKey, new HypixelKeyRecord(new AtomicInteger(remainingLimit), new AtomicInteger(timeTillReset)));
			}
		} catch (Exception e) {
			return errorEmbed("You must set a valid Hypixel API key to use this feature or command");
		}
		return null;
	}

	public static Map<Integer, InvItem> getGenericInventoryMap(NBTCompound parsedContents) {
		try {
			NBTList items = parsedContents.getList("i");
			Map<Integer, InvItem> itemsMap = new HashMap<>();

			for (int i = 0; i < items.size(); i++) {
				try {
					NBTCompound item = items.getCompound(i);
					if (!item.isEmpty()) {
						InvItem itemInfo = new InvItem();
						itemInfo.setName(item.getString("tag.display.Name", "None"));
						itemInfo.setLore(item.getList("tag.display.Lore"));
						itemInfo.setCount(Integer.parseInt(item.getString("Count", "0").replace("b", " ")));
						itemInfo.setId(item.getString("tag.ExtraAttributes.id", "None"));
						itemInfo.setCreationTimestamp(item.getString("tag.ExtraAttributes.timestamp", "None"));
						itemInfo.setHpbCount(item.getInt("tag.ExtraAttributes.hot_potato_count", 0));
						itemInfo.setRecombobulated(item.getInt("tag.ExtraAttributes.rarity_upgrades", 0) == 1);
						itemInfo.setModifier(item.getString("tag.ExtraAttributes.modifier", "None"));
						itemInfo.setDungeonFloor(Integer.parseInt(item.getString("tag.ExtraAttributes.item_tier", "-1")));
						itemInfo.setNbtTag(item);
						itemInfo.setDarkAuctionPrice(item.getLong("tag.ExtraAttributes.winning_bid", -1L));
						itemInfo.setMuseum(item.getInt("tag.ExtraAttributes.donated_museum", 0) == 1);

						if (
							(itemInfo.getId().equals("PARTY_HAT_CRAB") || itemInfo.getId().equals("PARTY_HAT_CRAB_ANIMATED")) &&
							item.containsKey("tag.ExtraAttributes.party_hat_color")
						) {
							itemInfo.setId(itemInfo.getId() + "_" + item.getString("tag.ExtraAttributes.party_hat_color").toUpperCase());
						}

						if (itemInfo.getId().equals("NEW_YEAR_CAKE") && item.containsKey("tag.ExtraAttributes.new_years_cake")) {
							itemInfo.setId(itemInfo.getId() + "_" + item.get("tag.ExtraAttributes.new_years_cake"));
						}

						if (
							item.containsKey("tag.ExtraAttributes.price") &&
							item.containsKey("tag.ExtraAttributes.auction") &&
							item.containsKey("tag.ExtraAttributes.bid")
						) {
							itemInfo.setShensAuctionPrice(item.getLong("tag.ExtraAttributes.price", -1));
						}

						if (item.containsTag("tag.ExtraAttributes.enchantments", TagType.COMPOUND)) {
							List<String> enchantsList = new ArrayList<>();
							for (Map.Entry<String, Object> enchant : item.getCompound("tag.ExtraAttributes.enchantments").entrySet()) {
								if (
									enchant.getKey().equals("efficiency") &&
									!itemInfo.getId().equals("PROMISING_SPADE") &&
									(int) enchant.getValue() > 5
								) {
									itemInfo.addExtraValues(
										(int) enchant.getValue() - (itemInfo.getId().equals("STONK_PICKAXE") ? 6 : 5),
										"SIL_EX"
									);
								}
								enchantsList.add(enchant.getKey() + ";" + enchant.getValue());
							}
							itemInfo.setEnchantsFormatted(enchantsList);
						}

						if (item.containsTag("tag.ExtraAttributes.attributes", TagType.COMPOUND)) {
							for (Map.Entry<String, Object> attribute : item.getCompound("tag.ExtraAttributes.attributes").entrySet()) {
								itemInfo.addExtraValues(
									(int) Math.pow(2, (Integer) attribute.getValue() - 1),
									"ATTRIBUTE_SHARD_" + attribute.getKey().toUpperCase()
								);
							}
						}

						if (item.containsKey("tag.ExtraAttributes.skin")) {
							itemInfo.setSkin(
								(itemInfo.getId().equals("PET") ? "PET_SKIN_" : "") + item.getString("tag.ExtraAttributes.skin")
							);
						}

						if (item.containsKey("tag.ExtraAttributes.dye_item")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.dye_item"));
						}

						if (item.containsKey("tag.ExtraAttributes.talisman_enrichment")) {
							itemInfo.addExtraValue("TALISMAN_ENRICHMENT_" + item.getString("tag.ExtraAttributes.talisman_enrichment"));
						}

						if (item.containsTag("tag.ExtraAttributes.ability_scroll", TagType.LIST)) {
							for (Object scroll : item.getList("tag.ExtraAttributes.ability_scroll")) {
								itemInfo.addExtraValue((String) scroll);
							}
						}

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.wood_singularity_count", 0), "WOOD_SINGULARITY");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.thunder_charge", 0) / 50000, "THUNDER_IN_A_BOTTLE");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.art_of_war_count", 0), "THE_ART_OF_WAR");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.artOfPeaceApplied", 0), "THE_ART_OF_PEACE");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.tuned_transmission", 0), "TRANSMISSION_TUNER");

						// Master stars
						if (
							(
								item.getInt("tag.ExtraAttributes.dungeon_item_level", 0) > 5 ||
								item.getInt("tag.ExtraAttributes.upgrade_level", 0) > 5
							)
						) {
							String essenceType = higherDepth(getEssenceCostsJson(), itemInfo.getId() + ".type", null);
							if (essenceType != null && !essenceType.equals("Crimson")) {
								int masterStarCount =
									Math.max(
										item.getInt("tag.ExtraAttributes.dungeon_item_level", 0),
										item.getInt("tag.ExtraAttributes.upgrade_level", 0)
									) -
									5;
								switch (masterStarCount) {
									case 5:
										itemInfo.addExtraValue("FIFTH_MASTER_STAR");
									case 4:
										itemInfo.addExtraValue("FOURTH_MASTER_STAR");
									case 3:
										itemInfo.addExtraValue("THIRD_MASTER_STAR");
									case 2:
										itemInfo.addExtraValue("SECOND_MASTER_STAR");
									case 1:
										itemInfo.addExtraValue("FIRST_MASTER_STAR");
								}
							}
						}

						// Regular or crimson essence
						if (
							item.containsKey("tag.ExtraAttributes.dungeon_item_level") ||
							item.containsKey("tag.ExtraAttributes.upgrade_level")
						) {
							JsonElement essenceUpgrades = higherDepth(getEssenceCostsJson(), itemInfo.getId());
							if (higherDepth(essenceUpgrades, "type") != null) {
								int totalEssence = 0;
								int itemLevel = Math.max(
									item.getInt("tag.ExtraAttributes.dungeon_item_level", 0),
									item.getInt("tag.ExtraAttributes.upgrade_level", 0)
								);
								for (int j = 0; j <= itemLevel; j++) {
									if (j == 0) {
										totalEssence += higherDepth(essenceUpgrades, "dungeonize", 0);
									} else {
										totalEssence += higherDepth(essenceUpgrades, "" + j, 0);
									}
								}
								itemInfo.setEssence(totalEssence, higherDepth(essenceUpgrades, "type").getAsString().toUpperCase());
							}
						}

						// Crimson item upgrades
						if (
							item.containsKey("tag.ExtraAttributes.upgrade_level") &&
							higherDepth(getEssenceCostsJson(), itemInfo.getId() + ".type", "").equals("Crimson")
						) {
							JsonElement itemUpgrades = higherDepth(getEssenceCostsJson(), itemInfo.getId() + ".items");
							if (itemUpgrades != null) {
								int crimsonStar = item.getInt("tag.ExtraAttributes.upgrade_level", 0);
								for (Map.Entry<String, JsonElement> entry : itemUpgrades.getAsJsonObject().entrySet()) {
									if (Integer.parseInt(entry.getKey()) > crimsonStar) {
										break;
									}

									for (JsonElement itemUpgrade : entry.getValue().getAsJsonArray()) {
										String[] itemUpgradeSplit = itemUpgrade.getAsString().split(":");
										itemInfo.addExtraValues(Integer.parseInt(itemUpgradeSplit[1]), itemUpgradeSplit[0]);
									}
								}
							}
						}

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.farming_for_dummies_count", 0), "FARMING_FOR_DUMMIES");

						itemInfo.addExtraValues(
							Integer.parseInt(item.getString("tag.ExtraAttributes.ethermerge", "0").replace("b", " ")),
							"ETHERWARP_CONDUIT"
						);

						if (item.containsKey("tag.ExtraAttributes.drill_part_upgrade_module")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_upgrade_module").toUpperCase());
						}
						if (item.containsKey("tag.ExtraAttributes.drill_part_fuel_tank")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_fuel_tank").toUpperCase());
						}
						if (item.containsKey("tag.ExtraAttributes.drill_part_engine")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.drill_part_engine").toUpperCase());
						}

						if (item.containsKey("tag.ExtraAttributes.petInfo")) {
							JsonElement petInfoJson = JsonParser.parseString(item.getString("tag.ExtraAttributes.petInfo"));
							if (higherDepth(petInfoJson, "heldItem", null) != null) {
								itemInfo.addExtraValue(higherDepth(petInfoJson, "heldItem").getAsString());
							}
							if (higherDepth(petInfoJson, "tier", null) != null) {
								itemInfo.setRarity(higherDepth(petInfoJson, "tier").getAsString());
							}
						}

						if (item.containsTag("tag.ExtraAttributes.gems", TagType.COMPOUND)) {
							NBTCompound gems = item.getCompound("tag.ExtraAttributes.gems");

							// Slot unlock costs
							JsonElement sbItemData = higherDepth(getSkyblockItemsJson().get(itemInfo.getId()), "gemstone_slots");
							if (sbItemData != null && gems.containsTag("unlocked_slots", TagType.LIST)) {
								List<String> unlockedSlots = gems
									.getList("unlocked_slots")
									.stream()
									.map(slot -> (String) slot)
									.collect(Collectors.toCollection(ArrayList::new));
								for (JsonElement gemstoneSlot : sbItemData.getAsJsonArray()) {
									if (higherDepth(gemstoneSlot, "costs") != null) {
										for (int unlockedSlotIdx = unlockedSlots.size() - 1; unlockedSlotIdx >= 0; unlockedSlotIdx--) {
											if (
												unlockedSlots
													.get(unlockedSlotIdx)
													.startsWith(higherDepth(gemstoneSlot, "slot_type").getAsString())
											) {
												for (JsonElement gemstoneSlotCost : higherDepth(gemstoneSlot, "costs").getAsJsonArray()) {
													if (higherDepth(gemstoneSlotCost, "type").getAsString().equals("COINS")) {
														itemInfo.addExtraValues(
															higherDepth(gemstoneSlotCost, "coins").getAsInt(),
															"SKYBLOCK_COIN"
														);
													} else {
														itemInfo.addExtraValues(
															higherDepth(gemstoneSlotCost, "amount").getAsInt(),
															higherDepth(gemstoneSlotCost, "item_id").getAsString()
														);
													}
												}
												unlockedSlots.remove(unlockedSlotIdx);
												break;
											}
										}
									}
								}
							}

							for (Map.Entry<String, Object> gem : gems.entrySet()) {
								if (!gem.getKey().endsWith("_gem")) {
									if (gems.containsKey(gem.getKey() + "_gem")) { // "COMBAT_0": "PERFECT" & "COMBAT_0_gem": "JASPER"
										itemInfo.addExtraValue(
											(
												gem.getValue() instanceof NBTCompound gemQualityNbt
													? gemQualityNbt.getString("quality", "UNKNOWN")
													: gem.getValue()
											) +
											"_" +
											gems.get(gem.getKey() + "_gem") +
											"_GEM"
										);
									} else if (!gem.getKey().equals("unlocked_slots")) { // "RUBY_0": "PERFECT"
										itemInfo.addExtraValue(
											(
												gem.getValue() instanceof NBTCompound gemQualityNbt
													? gemQualityNbt.getString("quality", "UNKNOWN")
													: gem.getValue()
											) +
											"_" +
											gem.getKey().split("_")[0] +
											"_GEM"
										);
									}
								}
							}
						}

						// Armor prestige costs
						if (
							itemInfo
								.getId()
								.matches(
									"(HOT|BURNING|FIERY|INFERNAL)_(CRIMSON|FERVOR|HOLLOW|TERROR|AURORA)_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"
								)
						) {
							List<String> prestigeOrder = List.of("HOT", "BURNING", "FIERY", "INFERNAL");
							for (int j = 0; j <= prestigeOrder.indexOf(itemInfo.getId().split("_")[0]); j++) {
								itemInfo.addExtraValues(
									higherDepth(ARMOR_PRESTIGE_COST.get(prestigeOrder.get(j)), "KUUDRA_TEETH", 0),
									"KUUDRA_TEETH"
								);
							}
						}

						// Gemstone slots for armors other than divan
						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.gemstone_slots", 0), "GEMSTONE_CHAMBER");

						try (
							ByteArrayInputStream backpackStream = new ByteArrayInputStream(
								item.getByteArray("tag.ExtraAttributes." + itemInfo.getId().toLowerCase() + "_data")
							)
						) {
							itemInfo.setBackpackItems(getGenericInventoryMap(NBTReader.read(backpackStream)).values());
						} catch (Exception ignored) {}

						itemsMap.put(i, itemInfo);
						continue;
					}
				} catch (Exception ignored) {}
				itemsMap.put(i, null);
			}

			return itemsMap;
		} catch (Exception ignored) {}

		return null;
	}

	public static InvItem nbtToItem(String rawContents) {
		try {
			return getGenericInventoryMap(NBTReader.readBase64(rawContents)).get(0);
		} catch (Exception e) {
			return null;
		}
	}

	public static void cacheApplyGuildUsers() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching Apply Users");
		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			List<ApplyGuild> applySettings = automaticGuild.getValue().applyGuild;
			for (ApplyGuild applySetting : applySettings) {
				try {
					String name = higherDepth(applySetting.currentSettings, "guildName").getAsString();
					List<ApplyUser> applyUserList = applySetting.applyUserList
						.stream()
						.filter(a -> {
							try {
								return jda.getTextChannelById(a.applicationChannelId) != null;
							} catch (Exception e) {
								return false;
							}
						})
						.collect(Collectors.toCollection(ArrayList::new));
					database.setApplyCacheSettings(automaticGuild.getKey(), name, gson.toJson(applyUserList));

					if (applyUserList.size() > 0) {
						log.info(
							"Cached ApplyUser - size={" +
							applyUserList.size() +
							"}, guildId={" +
							automaticGuild.getKey() +
							"}, name={" +
							name +
							"}"
						);
					}
				} catch (Exception e) {
					log.error(
						"guildId={" +
						automaticGuild.getKey() +
						"}, name={" +
						higherDepth(applySetting.currentSettings, "guildName", "null") +
						"}",
						e
					);
				}
			}
		}
		log.info("Cached apply users in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
	}

	public static List<ApplyUser> getApplyGuildUsersCache(String guildId, String name) {
		if (!isMainBot()) {
			return new ArrayList<>();
		}

		JsonArray applyUsersCache = database.getApplyCacheSettings(guildId, name);

		try {
			List<ApplyUser> applyUsersCacheList = streamJsonArray(applyUsersCache)
				.map(u -> gson.fromJson(u, ApplyUser.class))
				.filter(u -> {
					try {
						return jda.getTextChannelById(u.applicationChannelId) != null;
					} catch (Exception e) {
						return false;
					}
				})
				.collect(Collectors.toCollection(ArrayList::new));

			if (applyUsersCacheList.size() > 0) {
				log.info(
					"Retrieved ApplyUser cache - size={" + applyUsersCacheList.size() + "}, guildId={" + guildId + "}, name={" + name + "}"
				);
				return applyUsersCacheList;
			}
		} catch (Exception e) {
			log.error("guildId={" + guildId + "}, name={" + name + "}", e);
		}

		return new ArrayList<>();
	}

	public static double getPriceOverride(String itemId) {
		return higherDepth(getPriceOverrideJson(), itemId, -1.0);
	}

	public static double getMin(double val1, double val2) {
		val1 = val1 < 0 ? -1 : val1;
		val2 = val2 < 0 ? -1 : val2;

		if (val1 != -1 && val2 != -1) {
			return Math.max(Math.min(val1, val2), 0);
		} else if (val1 != -1) {
			return val1;
		} else {
			return val2;
		}
	}

	public static Permission[] defaultPerms() {
		return new Permission[] { Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS };
	}

	public static boolean isMainBot() {
		return DEFAULT_PREFIX.equals("+");
	}

	public static Map<String, Integer> getCommandUses() {
		Map<String, Integer> commandUses = client
			.getCommands()
			.stream()
			.filter(command -> !command.isOwnerCommand())
			.collect(Collectors.toMap(Command::getName, command -> client.getCommandUses(command)));
		slashCommandClient.getCommandUses().forEach((key, value) -> commandUses.compute(key, (k, v) -> (v != null ? v : 0) + value));
		return commandUses;
	}

	public static int getUserCount() {
		if (userCount == -1 || Duration.between(userCountLastUpdated, Instant.now()).toMinutes() >= 60) {
			userCount = jda.getGuilds().stream().mapToInt(Guild::getMemberCount).sum();
			userCountLastUpdated = Instant.now();
		}

		return userCount;
	}

	public static void updateItemMappings() {
		try {
			File neuDir = new File("src/main/java/com/skyblockplus/json/neu");
			if (neuDir.exists()) {
				FileUtils.deleteDirectory(neuDir);
			}
			neuDir.mkdir();

			File skyblockPlusDir = new File("src/main/java/com/skyblockplus/json/skyblock_plus");
			if (skyblockPlusDir.exists()) {
				FileUtils.deleteDirectory(skyblockPlusDir);
			}
			skyblockPlusDir.mkdir();

			Git neuRepo = Git
				.cloneRepository()
				.setURI("https://github.com/NotEnoughUpdates/NotEnoughUpdates-REPO.git")
				.setDirectory(neuDir)
				.call();

			Git skyblockPlusDataRepo = Git
				.cloneRepository()
				.setURI("https://github.com/kr45732/skyblock-plus-data.git")
				.setDirectory(skyblockPlusDir)
				.call();

			JsonElement currentPriceOverrides = JsonParser.parseReader(
				new FileReader("src/main/java/com/skyblockplus/json/skyblock_plus/PriceOverrides.json")
			);
			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/PriceOverrides.json")) {
				formattedGson.toJson(getUpdatedPriceOverridesJson(currentPriceOverrides), writer);
				writer.flush();
			}

			try (Writer writer = new FileWriter("src/main/java/com/skyblockplus/json/skyblock_plus/InternalNameMappings.json")) {
				formattedGson.toJson(getUpdatedItemMappingsJson(), writer);
				writer.flush();
			}

			try {
				skyblockPlusDataRepo.add().addFilepattern("InternalNameMappings.json").addFilepattern("PriceOverrides.json").call();
				skyblockPlusDataRepo
					.commit()
					.setAllowEmpty(false)
					.setAuthor("kr45632", "52721908+kr45732@users.noreply.github.com")
					.setCommitter("kr45632", "52721908+kr45732@users.noreply.github.com")
					.setMessage("Automatic update (" + neuRepo.log().setMaxCount(1).call().iterator().next().getName() + ")")
					.call();
				skyblockPlusDataRepo.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(GITHUB_TOKEN, "")).call();
			} catch (Exception ignored) {}

			FileUtils.deleteDirectory(neuDir);
			FileUtils.deleteDirectory(skyblockPlusDir);
			neuRepo.close();
			skyblockPlusDataRepo.close();

			resetSbPlusData();
		} catch (Exception e) {
			log.error("Exception while automatically updating item mappings", e);
		}
	}

	public static JsonElement getUpdatedItemMappingsJson() {
		JsonObject outputObj = new JsonObject();
		Map<String, JsonElement> npcBuyCosts = new HashMap<>();

		for (File child : Arrays
			.stream(new File("src/main/java/com/skyblockplus/json/neu/items").listFiles())
			.sorted(Comparator.comparing(File::getName))
			.collect(Collectors.toCollection(ArrayList::new))) {
			try {
				JsonElement itemJson = JsonParser.parseReader(new FileReader(child));
				String itemName = parseMcCodes(higherDepth(itemJson, "displayname").getAsString()).replace("�", "");
				String itemId = higherDepth(itemJson, "internalname").getAsString();
				if (
					itemId.endsWith("_MINIBOSS") ||
					itemId.endsWith("_MONSTER") ||
					itemId.endsWith("_ANIMAL") ||
					itemId.endsWith("_SC") ||
					itemId.endsWith("_BOSS") ||
					itemId.endsWith("_NPC")
				) {
					if (itemId.endsWith("_NPC") && higherDepth(itemJson, "recipes") != null) {
						for (JsonElement recipe : higherDepth(itemJson, "recipes").getAsJsonArray()) {
							String[] result = higherDepth(recipe, "result").getAsString().split(":");
							JsonObject buyCostObj = new JsonObject();
							JsonArray buyCostArr = higherDepth(recipe, "cost").getAsJsonArray();
							for (int i = 0; i < buyCostArr.size(); i++) {
								String buyCostArrItem = buyCostArr.get(i).getAsString();
								if (!buyCostArrItem.matches(".+:\\d+")) {
									buyCostArr.set(i, new JsonPrimitive(buyCostArrItem + ":1"));
								}
							}
							buyCostObj.add("cost", buyCostArr);
							if (result.length == 2) {
								buyCostObj.addProperty("count", Integer.parseInt(result[1]));
							}
							npcBuyCosts.put(result[0], buyCostObj);
						}
					}
					continue;
				}

				if (itemName.startsWith("[Lvl")) {
					itemName = capitalizeString(NUMBER_TO_RARITY_MAP.get(itemId.split(";")[1])) + " " + itemName.split("] ")[1];
				}
				if (itemName.equals("Enchanted Book")) {
					itemName = parseMcCodes(higherDepth(itemJson, "lore.[0]").getAsString());
				}
				if (itemId.contains("-")) {
					itemId = itemId.replace("-", ":");
				}

				JsonObject toAdd = new JsonObject();
				toAdd.addProperty("name", itemName);
				if (higherDepth(itemJson, "recipe") != null) {
					toAdd.add("recipe", higherDepth(itemJson, "recipe"));
				}
				if (PET_NAMES.contains(itemId.split(";")[0])) {
					try {
						Matcher matcher = neuTexturePattern.matcher(higherDepth(itemJson, "nbttag").getAsString());
						if (matcher.find()) {
							toAdd.addProperty(
								"texture",
								higherDepth(
									JsonParser.parseString(new String(Base64.getDecoder().decode(matcher.group(1)))),
									"textures.SKIN.url"
								)
									.getAsString()
									.split("http://textures.minecraft.net/texture/")[1]
							);
						}
					} catch (Exception ignored) {}
				}

				if (higherDepth(itemJson, "infoType", "").equals("WIKI_URL")) {
					for (JsonElement info : higherDepth(itemJson, "info").getAsJsonArray()) {
						String wikiUrl = info.getAsString();
						toAdd.addProperty("wiki", wikiUrl);
						// Allows for falling back on unofficial wiki if official wiki link doesn't exist
						if (wikiUrl.startsWith("https://wiki.hypixel.net")) {
							break;
						}
					}
				}

				if (higherDepth(itemJson, "recipes") != null) {
					for (JsonElement recipe : higherDepth(itemJson, "recipes").getAsJsonArray()) {
						if (higherDepth(recipe, "type").getAsString().equals("forge")) {
							toAdd.add("forge", higherDepth(recipe, "duration"));
							break;
						}
					}
				}

				if (higherDepth(CONSTANTS, "EXTRA_INTERNAL_MAPPINGS." + itemId) != null) {
					for (Map.Entry<String, JsonElement> entry : higherDepth(CONSTANTS, "EXTRA_INTERNAL_MAPPINGS." + itemId)
						.getAsJsonObject()
						.entrySet()) {
						toAdd.add(entry.getKey(), entry.getValue());
					}
				}

				outputObj.add(itemId, toAdd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (Map.Entry<String, JsonElement> entry : npcBuyCosts.entrySet()) {
			if (outputObj.has(entry.getKey())) {
				outputObj.getAsJsonObject(entry.getKey()).add("npc_buy", entry.getValue());
			}
		}

		return outputObj;
	}

	public static JsonElement getUpdatedPriceOverridesJson(JsonElement currentPriceOverrides) {
		JsonObject outputObject = new JsonObject();
		JsonObject bazaarJson = getBazaarJson();

		for (File child : Arrays
			.stream(new File("src/main/java/com/skyblockplus/json/neu/items").listFiles())
			.sorted(Comparator.comparing(File::getName))
			.collect(Collectors.toCollection(ArrayList::new))) {
			try (FileReader reader = new FileReader(child)) {
				JsonElement itemJson = JsonParser.parseReader(reader);
				String id = higherDepth(itemJson, "internalname").getAsString().replace("-", ":");
				if (
					!bazaarJson.has(id) &&
					(
						higherDepth(itemJson, "vanilla", false) ||
						(
							higherDepth(itemJson, "lore.[0]", "").equals("§8Furniture") &&
							!higherDepth(itemJson, "internalname", "").startsWith("EPOCH_CAKE_")
						)
					)
				) {
					outputObject.addProperty(id, Math.max(0, getNpcSellPrice(id)));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		JsonObject finalOutput = new JsonObject();
		finalOutput.add("manual", higherDepth(currentPriceOverrides, "manual"));
		finalOutput.add("automatic", outputObject);
		return finalOutput;
	}
}
