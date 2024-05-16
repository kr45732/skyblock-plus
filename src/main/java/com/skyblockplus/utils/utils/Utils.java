/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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
import static com.skyblockplus.utils.ApiHandler.getHasteUrl;
import static com.skyblockplus.utils.ApiHandler.getNeuBranch;
import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.HttpUtils.getJson;
import static com.skyblockplus.utils.utils.HttpUtils.postUrl;
import static com.skyblockplus.utils.utils.HypixelUtils.getNpcSellPrice;
import static com.skyblockplus.utils.utils.HypixelUtils.isCrimsonArmor;
import static com.skyblockplus.utils.utils.JsonUtils.*;
import static com.skyblockplus.utils.utils.StringUtils.*;
import static org.springframework.util.StringUtils.countOccurrencesOf;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.google.gson.*;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
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
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import org.apache.commons.collections4.SetUtils;
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
		"https://discord.com/api/oauth2/authorize?client_id=796791167366594592&permissions=395541081169&scope=bot+applications.commands";
	public static final String FORUM_POST_LINK = "https://hypixel.net/threads/3980092";
	public static final String WEBSITE_LINK = "https://sbplus.codes";
	public static final ExceptionExecutor executor = new ExceptionExecutor(
		10,
		Integer.MAX_VALUE,
		15L,
		TimeUnit.SECONDS,
		new SynchronousQueue<>()
	);
	public static final ScheduledExecutorService scheduler = new ExceptionScheduler(7);
	public static final ExceptionExecutor playerRequestExecutor = new ExceptionExecutor(
		2,
		2,
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
	public static final ExceptionExecutor guildRequestExecutor = new ExceptionExecutor(
		1,
		1,
		45L,
		TimeUnit.SECONDS,
		new LinkedBlockingQueue<>()
	)
		.setAllowCoreThreadTimeOut(true);
	public static final ExceptionExecutor updateGuildExecutor = new ExceptionExecutor(
		1,
		1,
		45L,
		TimeUnit.SECONDS,
		new LinkedBlockingQueue<>()
	)
		.setAllowCoreThreadTimeOut(true);
	public static final List<String> hypixelGuildRequestQueue = Collections.synchronizedList(new ArrayList<>());
	public static final List<String> hypixelGuildFetchQueue = Collections.synchronizedList(new ArrayList<>());
	public static final HypixelKeyRecord hypixelRateLimiter = new HypixelKeyRecord(600, 0);
	public static final Gson gson = new GsonBuilder()
		.addSerializationExclusionStrategy(new ExposeExclusionStrategy(true))
		.addDeserializationExclusionStrategy(new ExposeExclusionStrategy(false))
		.create();
	public static final Gson formattedGson = new GsonBuilder()
		.setPrettyPrinting()
		.addSerializationExclusionStrategy(new ExposeExclusionStrategy(true))
		.addDeserializationExclusionStrategy(new ExposeExclusionStrategy(false))
		.create();
	public static final Consumer<Object> ignore = ignored -> {};
	public static final Pattern nicknameTemplatePattern = Pattern.compile("\\[(GUILD|PLAYER)\\.(\\w+)(?:\\.\\{(.*?)})?]");
	/* Constants */
	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	private static final Color BOT_COLOR = new Color(223, 5, 5);
	public static final Pattern neuTexturePattern = Pattern.compile("Properties:\\{textures:\\[0:\\{Value:\"(.*)\"}]}");
	public static final Pattern crimsonArmorRegex = Pattern.compile(
		"(|HOT_|BURNING_|FIERY_|INFERNAL_)(CRIMSON|FERVOR|HOLLOW|TERROR|AURORA)_(HELMET|CHESTPLATE|LEGGINGS|BOOTS)"
	);
	public static final File rendersDirectory = new File("src/main/java/com/skyblockplus/json/renders");
	/* Environment */
	public static String HYPIXEL_API_KEY = "";
	public static String BOT_TOKEN = "";
	public static String DATABASE_URL = "";
	public static String API_USERNAME = "";
	public static String API_PASSWORD = "";
	public static String GITHUB_TOKEN = "";
	public static String DEFAULT_PREFIX = "";
	public static String AUCTION_API_KEY = "";
	public static String SBZ_SCAMMER_DB_KEY = "";
	public static String LEADERBOARD_DB_URL = "";
	public static String CLIENT_SECRET = "";
	public static String BASE_URL = "";
	public static String JACOB_KEY = "";
	public static String JACOB_API = "";
	public static String EXTRAS_API = "";
	public static String HASTE_KEY = "";
	public static String DISCORD_BOT_LIST_TOKEN = "";
	public static String DISCORD_BOTS_GG_TOKEN = "";
	public static String DISCORDS_COM_TOKEN = "";
	public static String TOP_GG_TOKEN = "";
	public static String AUCTION_FLIPPER_WEBHOOK = "";
	public static String BOT_STATUS_WEBHOOK = "";
	public static TextChannel errorLogChannel;
	public static ShardManager jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;
	public static JDAWebhookClient botStatusWebhook;
	public static CommandClient client;
	public static SlashCommandClient slashCommandClient;
	public static OAuthClient oAuthClient;
	public static Map<String, ServerSettingsModel> allServerSettings;
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
			.setFooter("SB+ is open source • sbplus.codes/gh")
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
			DATABASE_URL = (String) appProps.get("DATABASE_URL");
			GITHUB_TOKEN = (String) appProps.get("GITHUB_TOKEN");
			API_USERNAME = (String) appProps.get("API_USERNAME");
			API_PASSWORD = (String) appProps.get("API_PASSWORD");
			DEFAULT_PREFIX = (String) appProps.get("DEFAULT_PREFIX");
			AUCTION_API_KEY = (String) appProps.get("AUCTION_API_KEY");
			SBZ_SCAMMER_DB_KEY = (String) appProps.get("SBZ_SCAMMER_DB_KEY");
			LEADERBOARD_DB_URL = (String) appProps.get("LEADERBOARD_DB_URL");
			CLIENT_SECRET = (String) appProps.get("CLIENT_SECRET");
			BASE_URL = (String) appProps.get("BASE_URL");
			JACOB_KEY = (String) appProps.get("JACOB_KEY");
			JACOB_API = (String) appProps.get("JACOB_API");
			EXTRAS_API = (String) appProps.get("EXTRAS_API");
			HASTE_KEY = (String) appProps.get("HASTE_KEY");
			DISCORD_BOT_LIST_TOKEN = (String) appProps.get("DISCORD_BOT_LIST_TOKEN");
			DISCORD_BOTS_GG_TOKEN = (String) appProps.get("DISCORD_BOTS_GG_TOKEN");
			DISCORDS_COM_TOKEN = (String) appProps.get("DISCORDS_COM_TOKEN");
			TOP_GG_TOKEN = (String) appProps.get("TOP_GG_TOKEN");
			AUCTION_FLIPPER_WEBHOOK = (String) appProps.get("AUCTION_FLIPPER_WEBHOOK");
			BOT_STATUS_WEBHOOK = (String) appProps.get("BOT_STATUS_WEBHOOK");
		} catch (IOException e) {
			HYPIXEL_API_KEY = System.getenv("HYPIXEL_API_KEY");
			BOT_TOKEN = System.getenv("BOT_TOKEN");
			DATABASE_URL = System.getenv("DATABASE_URL");
			GITHUB_TOKEN = System.getenv("GITHUB_TOKEN");
			API_USERNAME = System.getenv("API_USERNAME");
			API_PASSWORD = System.getenv("API_PASSWORD");
			DEFAULT_PREFIX = System.getenv("DEFAULT_PREFIX");
			AUCTION_API_KEY = System.getenv("AUCTION_API_KEY");
			SBZ_SCAMMER_DB_KEY = System.getenv("SBZ_SCAMMER_DB_KEY");
			LEADERBOARD_DB_URL = System.getenv("LEADERBOARD_DB_URL");
			CLIENT_SECRET = System.getenv("CLIENT_SECRET");
			BASE_URL = System.getenv("BASE_URL");
			JACOB_KEY = System.getenv("JACOB_KEY");
			JACOB_API = System.getenv("JACOB_API");
			EXTRAS_API = System.getenv("EXTRAS_API");
			HASTE_KEY = System.getenv("HASTE_KEY");
			DISCORD_BOT_LIST_TOKEN = System.getenv("DISCORD_BOT_LIST_TOKEN");
			DISCORD_BOTS_GG_TOKEN = System.getenv("DISCORD_BOTS_GG_TOKEN");
			DISCORDS_COM_TOKEN = System.getenv("DISCORDS_COM_TOKEN");
			TOP_GG_TOKEN = System.getenv("TOP_GG_TOKEN");
			AUCTION_FLIPPER_WEBHOOK = System.getenv("AUCTION_FLIPPER_WEBHOOK");
			BOT_STATUS_WEBHOOK = System.getenv("BOT_STATUS_WEBHOOK");
		}
	}

	public static String getEmojiWithName(String id, String name) {
		return getEmoji(id).replaceAll("(?<before>:).*(?<after>:)", "${before}" + name + "${after}");
	}

	public static String getEmoji(String id) {
		return getEmoji(id, "");
	}

	public static String getEmoji(String id, String defaultValue) {
		return higherDepth(getEmojiMap(), id.equals("SKYBLOCK_COIN") ? "PIGGY_BANK" : id, defaultValue);
	}

	public static Emoji getEmojiObj(String id) {
		return Emoji.fromFormatted(getEmoji(id, null));
	}

	public static String makeHastePost(Object body) {
		try {
			return getHasteUrl() + higherDepth(postUrl(getHasteUrl() + "?key=" + HASTE_KEY, body), "key").getAsString();
		} catch (Exception e) {
			return null;
		}
	}

	public static String makeJsonPost(Object body) {
		try {
			body = gson.toJsonTree(body);
		} catch (Exception ignored) {}

		return makeHastePost(body);
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
						itemInfo.setCount(item.getInt("Count", 1));
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
							boolean is2022 = itemInfo.getId().equals("PARTY_HAT_CRAB_ANIMATED");
							itemInfo.setId(
								"PARTY_HAT_CRAB_" +
								item.getString("tag.ExtraAttributes.party_hat_color").toUpperCase() +
								(is2022 ? "_ANIMATED" : "")
							);
						}

						if (itemInfo.getId().equals("PARTY_HAT_SLOTH") && item.containsKey("tag.ExtraAttributes.party_hat_emoji")) {
							itemInfo.setId("PARTY_HAT_SLOTH_" + item.getString("tag.ExtraAttributes.party_hat_emoji").toUpperCase());
						}

						if (itemInfo.getId().equals("NEW_YEAR_CAKE") && item.containsKey("tag.ExtraAttributes.new_years_cake")) {
							itemInfo.setId(itemInfo.getId() + "_" + item.get("tag.ExtraAttributes.new_years_cake"));
						}

						itemInfo.setShiny(item.getInt("tag.ExtraAttributes.is_shiny", 0) == 1);

						if (
							item.containsKey("tag.ExtraAttributes.price") &&
							item.containsKey("tag.ExtraAttributes.auction") &&
							item.containsKey("tag.ExtraAttributes.bid")
						) {
							itemInfo.setShensAuctionPrice(item.getLong("tag.ExtraAttributes.price", -1));
						}

						if (item.containsTag("tag.ExtraAttributes.enchantments", TagType.COMPOUND)) {
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
								itemInfo.getEnchantsFormatted().add(enchant.getKey().toUpperCase() + ";" + enchant.getValue());
							}
						}

						if (item.containsTag("tag.ExtraAttributes.runes", TagType.COMPOUND)) {
							for (Map.Entry<String, Object> rune : item.getCompound("tag.ExtraAttributes.runes").entrySet()) {
								itemInfo.getRunesFormatted().add(rune.getKey().toUpperCase() + "_RUNE;" + rune.getValue());
							}
						}

						if (item.containsTag("tag.ExtraAttributes.attributes", TagType.COMPOUND)) {
							for (Map.Entry<String, Object> attribute : item.getCompound("tag.ExtraAttributes.attributes").entrySet()) {
								itemInfo
									.getAttributes()
									.put("ATTRIBUTE_SHARD_" + attribute.getKey().toUpperCase(), (int) attribute.getValue());
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
							itemInfo.addExtraValue(
								"TALISMAN_ENRICHMENT_" + item.getString("tag.ExtraAttributes.talisman_enrichment").toUpperCase()
							);
						}

						if (item.containsTag("tag.ExtraAttributes.ability_scroll", TagType.LIST)) {
							for (Object scroll : item.getList("tag.ExtraAttributes.ability_scroll")) {
								itemInfo.addExtraValue((String) scroll);
							}
						}

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.sack_pss", 0), "POCKET_SACK_IN_A_SACK");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.jalapeno_count", 0), "JALAPENO_BOOK");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.wood_singularity_count", 0), "WOOD_SINGULARITY");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.thunder_charge", 0) / 50000, "THUNDER_IN_A_BOTTLE");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.art_of_war_count", 0), "THE_ART_OF_WAR");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.artOfPeaceApplied", 0), "THE_ART_OF_PEACE");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.tuned_transmission", 0), "TRANSMISSION_TUNER");

						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.mana_disintegrator_count", 0), "MANA_DISINTEGRATOR");

						// Master stars
						if (
							(item.getInt("tag.ExtraAttributes.dungeon_item_level", 0) > 5 ||
								item.getInt("tag.ExtraAttributes.upgrade_level", 0) > 5)
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

						// Essence counts and essence item upgrades
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

									JsonElement itemUpgrades = higherDepth(essenceUpgrades, "items." + j);
									if (itemUpgrades != null) {
										for (JsonElement itemUpgrade : itemUpgrades.getAsJsonArray()) {
											String[] itemUpgradeSplit = itemUpgrade.getAsString().split(":");
											itemInfo.addExtraValues(Integer.parseInt(itemUpgradeSplit[1]), itemUpgradeSplit[0]);
										}
									}
								}
								itemInfo.addEssence(totalEssence, higherDepth(essenceUpgrades, "type").getAsString().toUpperCase());
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
											(gem.getValue() instanceof NBTCompound gemQualityNbt
													? gemQualityNbt.getString("quality", "UNKNOWN")
													: gem.getValue()) +
											"_" +
											gems.get(gem.getKey() + "_gem") +
											"_GEM"
										);
									} else if (!gem.getKey().equals("unlocked_slots")) { // "RUBY_0": "PERFECT"
										itemInfo.addExtraValue(
											(gem.getValue() instanceof NBTCompound gemQualityNbt
													? gemQualityNbt.getString("quality", "UNKNOWN")
													: gem.getValue()) +
											"_" +
											gem.getKey().split("_")[0] +
											"_GEM"
										);
									}
								}
							}
						}

						// Armor prestige costs
						if (isCrimsonArmor(itemInfo.getId(), true)) {
							List<String> prestigeOrder = List.of("HOT", "BURNING", "FIERY", "INFERNAL");
							for (int j = 0; j <= prestigeOrder.indexOf(itemInfo.getId().split("_")[0]); j++) {
								for (Map.Entry<String, JsonElement> entry : ARMOR_PRESTIGE_COST
									.getAsJsonObject(prestigeOrder.get(j))
									.entrySet()) {
									if (entry.getKey().equals("CRIMSON_ESSENCE")) {
										itemInfo.addEssence(entry.getValue().getAsInt(), "CRIMSON");
									} else {
										itemInfo.addExtraValues(entry.getValue().getAsInt(), entry.getKey());
									}
								}
							}
						}

						// Gemstone slots for armors other than divan
						itemInfo.addExtraValues(item.getInt("tag.ExtraAttributes.gemstone_slots", 0), "GEMSTONE_CHAMBER");

						// Power scrolls (any right click abilities)
						if (item.containsKey("tag.ExtraAttributes.power_ability_scroll")) {
							itemInfo.addExtraValue(item.getString("tag.ExtraAttributes.power_ability_scroll").toUpperCase());
						}

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

	public static Collection<InvItem> nbtToItems(String rawContents) {
		try {
			return getGenericInventoryMap(NBTReader.readBase64(rawContents)).values();
		} catch (Exception e) {
			return null;
		}
	}

	public static void cacheApplyGuildUsers() {
		if (!isMainBot()) {
			return;
		}

		log.info("Caching apply users");
		long startTime = System.currentTimeMillis();
		for (Map.Entry<String, AutomaticGuild> automaticGuild : guildMap.entrySet()) {
			List<ApplyGuild> applySettings = automaticGuild.getValue().applyGuilds;
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

					if (!applyUserList.isEmpty()) {
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
						"Error caching ApplyUser - guildId={" +
						automaticGuild.getKey() +
						"}, name={" +
						higherDepth(applySetting.currentSettings, "guildName", null) +
						"}",
						e
					);
				}
			}
		}
		log.info("Cached apply users in " + roundAndFormat((System.currentTimeMillis() - startTime) / 1000.0) + "s");
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
		return new Permission[] { Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS };
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

	public static void updateDataRepo() {
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
				.setBranch(getNeuBranch())
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
		Map<String, JsonArray> idToSkins = new HashMap<>();

		for (File child : Arrays
			.stream(new File("src/main/java/com/skyblockplus/json/neu/items").listFiles())
			.sorted(Comparator.comparing(File::getName))
			.collect(Collectors.toCollection(ArrayList::new))) {
			try {
				JsonElement itemJson = JsonParser.parseReader(new FileReader(child));
				String itemName = cleanMcCodes(higherDepth(itemJson, "displayname").getAsString()).replace("�", "");
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
								if (countOccurrencesOf(buyCostArrItem, ":") == 0) {
									buyCostArr.set(i, new JsonPrimitive(buyCostArrItem + ":1"));
								} else if (buyCostArrItem.endsWith(":")) {
									buyCostArr.set(i, new JsonPrimitive(buyCostArrItem + "1"));
								}
							}
							buyCostObj.add("cost", buyCostArr);
							if (result.length == 2) {
								buyCostObj.addProperty("count", (int) Double.parseDouble(result[1]));
							}
							npcBuyCosts.put(result[0], buyCostObj);
						}
					}
					continue;
				}

				if (itemName.startsWith("[Lvl")) {
					itemName =
						capitalizeString(NUMBER_TO_RARITY_MAP.get(Integer.parseInt(itemId.split(";")[1]))) + " " + itemName.split("] ")[1];
				}

				if (itemName.equals("Enchanted Book")) {
					itemName = cleanMcCodes(higherDepth(itemJson, "lore.[0]").getAsString());
				}

				if (itemId.contains("-")) {
					itemId = itemId.replace("-", ":");
				}

				JsonObject properties = new JsonObject();
				properties.addProperty("name", itemName);
				if (higherDepth(itemJson, "recipe") != null) {
					properties.add("recipe", higherDepth(itemJson, "recipe"));
				}

				if (PET_NAMES.contains(itemId.split(";")[0])) {
					try {
						Matcher matcher = neuTexturePattern.matcher(higherDepth(itemJson, "nbttag").getAsString());
						if (matcher.find()) {
							properties.addProperty(
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
						properties.addProperty("wiki", wikiUrl);
						// Allows for falling back on unofficial wiki if official wiki link doesn't exist
						if (wikiUrl.startsWith("https://wiki.hypixel.net")) {
							break;
						}
					}
				}

				if (higherDepth(itemJson, "recipes") != null) {
					for (JsonElement recipe : higherDepth(itemJson, "recipes").getAsJsonArray()) {
						String recipeType = higherDepth(recipe, "type", "");
						if (recipeType.equals("forge") && !properties.has("forge")) {
							properties.add("forge", higherDepth(recipe, "duration"));
						} else if (recipeType.equals("crafting") && !properties.has("recipe")) {
							// TODO: handle multiple recipes (this just uses the first one)
							recipe.getAsJsonObject().remove("type");
							properties.add("recipe", recipe);
						}
					}
				}

				JsonArray lore = higherDepth(itemJson, "lore").getAsJsonArray();
				for (int i = 0; i < lore.size(); i++) {
					String line = lore.get(i).getAsString();
					if (line.equals("§7§7This skin can be applied to")) {
						String nextLine = lore.get(i + 1).getAsString();
						String finalItemId = itemId;
						if (nextLine.equals("§7§aRadiant Power Orb§7, §9Mana")) {
							for (String powerOrb : List.of(
								"RADIANT_POWER_ORB",
								"MANA_FLUX_POWER_ORB",
								"OVERFLUX_POWER_ORB",
								"PLASMAFLUX_POWER_ORB"
							)) {
								idToSkins.compute(
									powerOrb,
									(k, v) -> {
										(v = v != null ? v : new JsonArray()).add(finalItemId);
										return v;
									}
								);
							}
						} else {
							String id = nameToId(cleanMcCodes(nextLine), true);
							if (id != null) {
								idToSkins.compute(
									id,
									(k, v) -> {
										(v = v != null ? v : new JsonArray()).add(finalItemId);
										return v;
									}
								);
							}
						}
					}

					for (String rarity : raritiesWithColorCode) {
						if (line.startsWith(rarity)) {
							String baseRarity = cleanMcCodes(rarity).replace(" ", "_");
							properties.addProperty("base_rarity", baseRarity);
						}
					}

					if (i == lore.size() - 1 && !properties.has("base_rarity")) {
						String baseRarity = cleanMcCodes(lore.get(lore.size() - 1).getAsString()).trim().split("\\s+")[0];
						baseRarity += baseRarity.startsWith("VERY") ? "_SPECIAL" : "";
						if (rarityToMagicPower.containsKey(baseRarity)) {
							properties.addProperty("base_rarity", baseRarity);
						}
					}

					if (line.matches("§6Ability: (.*) §e§lRIGHT CLICK")) {
						properties.addProperty("scrollable", true);
					}
				}

				outputObj.add(itemId, properties);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		for (Map.Entry<String, JsonElement> entry : npcBuyCosts.entrySet()) {
			if (outputObj.has(entry.getKey())) {
				outputObj.getAsJsonObject(entry.getKey()).add("npc_buy", entry.getValue());
			}
		}

		for (Map.Entry<String, JsonArray> entry : idToSkins.entrySet()) {
			if (outputObj.has(entry.getKey())) {
				outputObj.getAsJsonObject(entry.getKey()).add("skins", entry.getValue());
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
					(higherDepth(itemJson, "vanilla", false) ||
						(higherDepth(itemJson, "lore.[0]", "").equals("§8Furniture") &&
							!higherDepth(itemJson, "internalname", "").startsWith("EPOCH_CAKE_")))
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

	public static MessageEditBuilder withApiHelpButton(EmbedBuilder eb) {
		return new MessageEditBuilder().setEmbeds(eb.build()).setActionRow(Button.primary("enable_api_help_button", "Help Enabling APIs"));
	}

	public static String getStackTrace(Throwable e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw, true);
		e.printStackTrace(pw);
		return sw.getBuffer().toString();
	}

	public static <E> SetUtils.SetView<E> setTriUnion(final Set<? extends E> a, final Set<? extends E> b, final Set<? extends E> c) {
		return SetUtils.union(SetUtils.union(a, b), c);
	}
}
