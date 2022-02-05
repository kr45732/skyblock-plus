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

package com.skyblockplus;

import static com.skyblockplus.features.listeners.AutomaticGuild.getGuildPrefix;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.*;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.miscellaneous.PublicEndpoints;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.*;
import com.skyblockplus.features.fetchur.FetchurHandler;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.MainListener;
import com.skyblockplus.features.mayor.MayorHandler;
import com.skyblockplus.features.party.PartyCommand;
import com.skyblockplus.features.party.PartySlashCommand;
import com.skyblockplus.features.setup.SetupCommand;
import com.skyblockplus.features.setup.SetupSlashCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand;
import com.skyblockplus.general.*;
import com.skyblockplus.general.help.HelpCommand;
import com.skyblockplus.general.help.HelpSlashCommand;
import com.skyblockplus.guild.*;
import com.skyblockplus.inventory.*;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.miscellaneous.networth.NetworthCommand;
import com.skyblockplus.miscellaneous.networth.NetworthSlashCommand;
import com.skyblockplus.miscellaneous.weight.WeightCommand;
import com.skyblockplus.miscellaneous.weight.WeightSlashCommand;
import com.skyblockplus.price.*;
import com.skyblockplus.settings.*;
import com.skyblockplus.skills.*;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.AuctionFlipper;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.exceptionhandler.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.slashcommand.SlashCommandClient;
import java.io.File;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static final Logger log = LoggerFactory.getLogger(Main.class);
	public static JDA jda;
	public static Database database;
	public static EventWaiter waiter;
	public static GlobalExceptionHandler globalExceptionHandler;
	public static CommandClient client;
	public static SlashCommandClient slashCommandClient;

	public static void main(String[] args) throws LoginException, IllegalArgumentException {
		Main.globalExceptionHandler = new GlobalExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
		RestAction.setDefaultFailure(e -> globalExceptionHandler.uncaughtException(null, e));

		Utils.initialize();
		Constants.initialize();

		Main.database = SpringApplication.run(Main.class, args).getBean(Database.class);
		Main.waiter = new EventWaiter(scheduler, true);
		Main.client =
			new CommandClientBuilder()
				.setOwnerId("385939031596466176")
				.setEmojis("✅", "⚠️", "❌")
				.useHelpBuilder(false)
				.setPrefixFunction(event -> event.isFromGuild() ? getGuildPrefix(event.getGuild().getId()) : DEFAULT_PREFIX)
				.setListener(
					new CommandListener() {
						@Override
						public void onCommandException(CommandEvent event, Command command, Throwable throwable) {
							globalExceptionHandler.uncaughtException(event, command, throwable);
						}
					}
				)
				.setActivity(Activity.playing("Loading..."))
				.setManualUpsert(true)
				.addCommands(
					new InformationCommand(),
					new SlayerCommand(),
					new HelpCommand(),
					new GuildCommand(),
					new AuctionCommand(),
					new BinCommand(),
					new SkillsCommand(),
					new DungeonsCommand(),
					new RolesCommand(),
					new GuildRanksCommand(),
					new EssenceCommand(),
					new CoinsCommand(),
					new WardrobeCommand(),
					new TalismanBagCommand(),
					new InventoryCommand(),
					new SacksCommand(),
					new WeightCommand(),
					new HypixelCommand(),
					new UuidCommand(),
					new SettingsCommand(),
					new ReloadCommand(),
					new SetupCommand(),
					new CategoriesCommand(),
					new PartyFinderCommand(),
					new DevSettingsCommand(),
					new GetServerEmojisCommand(),
					new EnderChestCommand(),
					new LinkCommand(),
					new GetSettingsCommand(),
					new UnlinkCommand(),
					new LinkedUserCommand(),
					new BazaarCommand(),
					new AverageAuctionCommand(),
					new PetsCommand(),
					new SkyblockEventCommand(),
					new DeleteMessagesCommand(),
					new PlaceholderCommand(),
					new ProfilesCommand(),
					new NetworthCommand(),
					new PriceCommand(),
					new BidsCommand(),
					new BitsCommand(),
					new EvaluateCommand(),
					new GuildKickerCommand(),
					new MissingCommand(),
					new UpdateSlashCommands(),
					new CalculateCommand(),
					new GuildLeaderboardCommand(),
					new ArmorCommand(),
					new FetchurCommand(),
					new HarpCommand(),
					new CakesCommand(),
					new ActiveCoinsCommand(),
					new HotmCommand(),
					new VoteCommand(),
					new TrackAuctionsCommand(),
					new SkyblockCommand(),
					new GuildStatisticsCommand(),
					new PartyCommand(),
					new TimeCommand(),
					new ForgeCommand(),
					new ScammerCommand(),
					new FixApplicationCommand(),
					new NucleusCommand(),
					new MayorCommand(),
					new LeaderboardCommand(),
					new JacobCommand()
				)
				.build();

		slashCommandClient =
			new SlashCommandClient()
				.setOwnerId("385939031596466176")
				.addCommands(
					new InviteSlashCommand(),
					new VoteSlashCommand(),
					new HotmSlashCommand(),
					new InformationSlashCommand(),
					new LinkSlashCommand(),
					new UnlinkSlashCommand(),
					new SlayerSlashCommand(),
					new SkillsSlashCommand(),
					new DungeonsSlashCommand(),
					new EssenceSlashCommand(),
					new PartyFinderSlashCommand(),
					new GuildSlashCommand(),
					new HelpSlashCommand(),
					new AuctionsSlashCommand(),
					new BinSlashCommand(),
					new BazaarSlashCommand(),
					new AverageAuctionSlashCommand(),
					new BidsSlashCommand(),
					new PriceSlashCommand(),
					new BitsSlashCommand(),
					new RolesSlashCommand(),
					new CoinsSlashCommand(),
					new WeightSlashCommand(),
					new HypixelSlashCommand(),
					new ProfilesSlashCommand(),
					new MissingSlashCommand(),
					new CalculateSlashCommand(),
					new SetupSlashCommand(),
					new SkyblockEventSlashCommand(),
					new FetchurSlashCommand(),
					new NetworthSlashCommand(),
					new InventorySlashCommand(),
					new ArmorSlashCommand(),
					new EnderChestSlashCommand(),
					new TalismanBagSlashCommand(),
					new SacksSlashCommand(),
					new WardrobeSlashCommand(),
					new HarpSlashCommand(),
					new CakesSlashCommand(),
					new ActiveCoinsSlashCommand(),
					new GuildLeaderboardSlashCommand(),
					new GuildRanksSlashCommand(),
					new GuildKickerSlashCommand(),
					new PetsSlashCommand(),
					new UuidSlashCommand(),
					new TrackAuctionsSlashCommand(),
					new SkyblockSlashCommand(),
					new GuildStatisticsSlashCommand(),
					new PartySlashCommand(),
					new SettingsSlashCommand(),
					new TimeSlashCommand(),
					new ReloadSlashCommand(),
					new ForgeSlashCommand(),
					new ScammerSlashCommand(),
					new CategoriesSlashCommand(),
					new FixApplicationSlashCommand(),
					new MayorSlashCommand(),
					new LeaderboardSlashCommand(),
					new JacobSlashCommand()
				);

		jda =
			JDABuilder
				.createDefault(BOT_TOKEN)
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.addEventListeners(
					new ExceptionEventListener(waiter),
					client,
					new ExceptionEventListener(slashCommandClient),
					new ExceptionEventListener(new MainListener())
				)
				.setActivity(Activity.playing("Loading..."))
				.setMemberCachePolicy(MemberCachePolicy.ALL)
				.disableCache(CacheFlag.VOICE_STATE)
				.enableIntents(GatewayIntent.GUILD_MEMBERS)
				.build();

		try {
			jda.awaitReady();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}

		ApiHandler.initialize();
		TrackAuctionsCommand.initialize();
		AuctionFlipper.setEnable(true);
		AuctionFlipper.scheduleHerokuUpdate();
		PublicEndpoints.initialize();
		FetchurHandler.initialize();
		MayorHandler.initialize();
		JacobHandler.initialize();
		File transcriptDir = new File("src/main/java/com/skyblockplus/json/application_transcripts/");
		if (!transcriptDir.exists()) {
			log.info((transcriptDir.mkdirs() ? "Successfully created" : "Failed to create") + " application transcript directory");
		}
	}

	@PreDestroy
	public void onExit() {
		log.info("Stopping");

		log.info("Caching Apply Users");
		cacheApplyGuildUsers();

		log.info("Caching Parties");
		cacheParties();

		log.info("Caching Command Uses");
		cacheCommandUses();

		log.info("Caching Jacob Data");
		cacheJacobData();

		log.info("Closing Http Client");
		closeHttpClient();

		log.info("Closing Async Http Client");
		closeAsyncHttpClient();

		log.info("Finished");
	}
}
/*
public static JsonObject idToEmoji = new JsonObject();
public static void runEmojis() {
	String last = "";

	try {
		JsonObject all2 = getJson("https://hst.sh/raw/opozagadaf.json").getAsJsonObject();
		Set<String> added = JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/emojis.json")).getAsJsonObject().keySet();
		JsonObject all = new JsonObject();
		for (Map.Entry<String, JsonElement> entry : all2.entrySet()) {
			if (!added.contains(entry.getKey())) {
				all.add(entry.getKey(), entry.getValue());
			}
		}

		List<Guild> guildList = jda.getGuilds().stream().filter(g -> {
			try {
				return Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]) > 0;
			} catch (Exception e) {
				return false;
			}
		}).sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]))).collect(Collectors.toList());

		int guildCount = 71;
		for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
			try {
				last = entry.toString();
				String name = idToName(entry.getKey()).toLowerCase().replace(" ", "_");

				Guild curGuild = guildList.get(guildCount);
				if (curGuild.getEmotes().size() >= curGuild.getMaxEmotes()) {
					guildCount++;
					curGuild = guildList.get(guildCount);
					TimeUnit.SECONDS.sleep(10);
					System.out.println("Switched to g = " + guildCount + " - " + curGuild.getName());
				}

				String url = entry.getValue().getAsString();
				Emote emoji;
				if (url.startsWith("src/main/java/com/skyblockplus/json")) {
					emoji = curGuild.createEmote(name, Icon.from(new File(url))).complete();
				} else {
					URLConnection urlConn = new URL(url).openConnection();
					urlConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

					emoji = curGuild.createEmote(name, Icon.from(urlConn.getInputStream())).complete();
				}
				System.out.println("Created emoji - " + last);
				idToEmoji.addProperty(entry.getKey(), emoji.getAsMention());
				TimeUnit.MILLISECONDS.sleep(100);

			} catch (Exception e) {
				System.out.println("Failed - " + last);
				e.printStackTrace();
			}
		}
	} catch (Exception e) {
		System.out.println("Failed - " + last);
		e.printStackTrace();
	}
}

public static JsonElement getMissing(String url){
	Type listString = new TypeToken<Map<String, String>>() {}.getType();
	Map<String, String> processedItems = gson.fromJson(getJson(url), listString);
	Set<String> processedItemsSet = processedItems.keySet();

	Set<String> allItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json").getAsJsonObject().keySet();
	allItems.removeIf(o -> processedItemsSet.contains(o));
	return gson.toJsonTree(allItems);
}

public static JsonObject processAll() {
	try {
		JsonArray sbItems = getSkyblockItemsJson();

		JsonObject out = new JsonObject();
		for (JsonElement i : sbItems) {
			if (higherDepth(i, "skin") != null) {
				out.addProperty(higherDepth(i, "id").getAsString(), "https://sky.shiiyu.moe/head/" + higherDepth(JsonParser.parseString(new String(Base64.getDecoder().decode(higherDepth(i, "skin").getAsString()))), "textures.SKIN.url", "").split("://textures.minecraft.net/texture/")[1]);
			}
		}

		JsonObject processedImages = processDir(new File("src/main/java/com/skyblockplus/json/cit"));
		for (Map.Entry<String, JsonElement> entry : processedImages.entrySet()) {
			out.add(entry.getKey(), entry.getValue());
		}

		Set<String> allSbItems = getJson("https://raw.githubusercontent.com/kr45732/skyblock-plus-data/main/InternalNameMappings.json").getAsJsonObject().keySet();
		allSbItems.removeIf(out::has);

		for (String sbItem : allSbItems) {
			String split = sbItem.split(";")[0];
			if (PET_NAMES.contains(split)) {
				String petUrl = getPetUrl(split);
				out.addProperty(sbItem, petUrl);
			} else if (ENCHANT_NAMES.contains(split)) {
				out.addProperty(sbItem, "https://sky.shiiyu.moe/item/ENCHANTED_BOOK");
			}
		}

//			allSbItems.removeIf(out::has);

//			File skyCryptFiles = new File("src/main/java/com/skyblockplus/json/skycrypt_images");
//			if (skyCryptFiles.exists()) {
//				skyCryptFiles.delete();
//			}
//			skyCryptFiles.mkdirs();
//
//			int count = 0;
//			for (String sbItem : allSbItems) {
//				try {
//					URL imgUrl = new URL("https://sky.shiiyu.moe/item/" + sbItem);
//					File imgFile = new File(skyCryptFiles.getPath() + "/" + sbItem + ".png");
//					ImageIO.write(ImageIO.read(imgUrl), "png", imgFile);
//					out.addProperty(sbItem, imgFile.getPath());
//					TimeUnit.MILLISECONDS.sleep(250);
//				}catch (Exception e){
//					e.printStackTrace();
//				}
//				count ++;
//				if(count % 50 == 0){
//					System.out.println(count);
//				}
//			}

		return out;
	} catch (Exception e) {
		e.printStackTrace();
	}
	return null;
}

public static JsonObject processDir(File dir) {
	JsonObject out = new JsonObject();
	try {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				if (!file.getName().equals("model")) {
					JsonObject o = processDir(file);
					for (Map.Entry<String, JsonElement> e : o.entrySet()) {
						out.add(e.getKey(), e.getValue());
					}
				}
			} else {
				if (file.getName().endsWith(".properties")) {
					Properties appProps = new Properties();

					try {
						appProps.load(new FileInputStream(file));
					} catch (Exception e) {
						continue;
					}

					Object id = appProps.getOrDefault("nbt.ExtraAttributes.id", null);


					if (id == null) {
						continue;
					}
					File textureFile = Arrays.stream(file.getParentFile().listFiles()).filter(file1 -> file1.getName().equals(file.getName().replace(".properties", ".png"))).findFirst().orElse(null);
					if (textureFile == null) {
						continue;
					}

					String sId = ((String) id);
					if (getInternalJsonMappings().keySet().contains(sId)) {
						BufferedImage image = ImageIO.read(textureFile);
						if(image.getWidth() != 16){
							throw new IllegalArgumentException("Image width is not 16 pixels: " + file.getPath());
						}

						if (image.getHeight() != 16) {
							int rows = image.getHeight() / 16;
							int subImageHeight = image.getHeight() / rows;
							BufferedImage[] images = new BufferedImage[rows];

							for (int i = 0; i < rows; i++) {
								images[i] = new BufferedImage(32, 32, image.getType());
								Graphics2D imgCreator = images[i].createGraphics();

								int srcFirstY = subImageHeight * i;
								int dstCornerY = subImageHeight * i + subImageHeight;


								imgCreator.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
								imgCreator.drawImage(image, 0, 0, 32, 32, 0, srcFirstY, image.getWidth(), dstCornerY, null);
								imgCreator.dispose();
							}

							File gifFile = new File(textureFile.getParentFile().getPath() + "/" + sId + "_gif.gif");
							if(gifFile.exists()){
								gifFile.delete();
							}
							ImageIO.write(images[images.length - 1], "gif", gifFile);

							BufferedImage firstImage = images[0];
							try (ImageOutputStream output = new FileImageOutputStream(gifFile)) {
								GifWriter writer = new GifWriter(output, firstImage.getType(), 1, true);

								writer.writeToSequence(firstImage);
								for (int i = 1; i < images.length - 1; i++) {
									writer.writeToSequence(images[i]);
								}

								writer.close();
							}

							out.addProperty(sId, gifFile.getPath());
						} else {
							File scaledFile = new File(textureFile.getParentFile().getPath() + "/" + sId + "_scaled.png");
							if(scaledFile.exists()){
								scaledFile.delete();
							}

							BufferedImage scaledImage = new BufferedImage(32, 32, image.getType());
							Graphics2D graphics2D = scaledImage.createGraphics();
							graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
							graphics2D.drawImage(image, 0, 0, 32, 32, null);
							graphics2D.dispose();

							ImageIO.write(scaledImage, "png", scaledFile);
							out.addProperty(sId, scaledFile.getPath());
						}

					}
				}
			}
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	return out;
}

public static JsonObject idToEmoji = new JsonObject();
public static void runEmojis() {
	idToEmoji = new JsonObject();
	String last = "";

	try {
		JsonObject all2 = getJson("https://hst.sh/raw/opozagadaf.json").getAsJsonObject();
		Set<String> added = JsonParser.parseReader(new FileReader("src/main/java/com/skyblockplus/json/IdToEmojiMappings.json")).getAsJsonObject().keySet();
		JsonObject all = new JsonObject();
		for (Map.Entry<String, JsonElement> entry : all2.entrySet()) {
			if (!added.contains(entry.getKey())) {
				all.add(entry.getKey(), entry.getValue());
			}
		}

		List<Guild> guildList = jda.getGuilds().stream().filter(g -> {
			try {
				return Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]) > 0;
			} catch (Exception e) {
				return false;
			}
		}).sorted(Comparator.comparingInt(g -> Integer.parseInt(g.getName().split("Skyblock Plus - Emoji Server ")[1]))).collect(Collectors.toList());

		int guildCount = 77;
		for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
			try {
				last = entry.toString();
				String name = idToName(entry.getKey()).toLowerCase()
						.replace("⚚ ", "starred ")
						.replace(" ", "_").replace("'", "").replace("-", "_");

				Guild curGuild = guildList.get(guildCount);

				if (curGuild.getEmotes().size() >= curGuild.getMaxEmotes()) {
					guildCount++;
					curGuild = guildList.get(guildCount);
					TimeUnit.SECONDS.sleep(10);
					System.out.println("Switched to g = " + guildCount + " - " + curGuild.getName());
				}

				String url = entry.getValue().getAsString();
				Emote emoji;
				if (url.startsWith("src/main/java/com/skyblockplus/json")) {
					emoji = curGuild.createEmote(name, Icon.from(new File(url))).complete();
				} else {
					URLConnection urlConn = new URL(url).openConnection();
					urlConn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

					emoji = curGuild.createEmote(name, Icon.from(urlConn.getInputStream())).complete();
				}
				System.out.println("Created emoji - " + last);
				idToEmoji.addProperty(entry.getKey(), emoji.getAsMention());
				TimeUnit.MILLISECONDS.sleep(100);

			} catch (Exception e) {
				System.out.println("Failed - " + last);
				e.printStackTrace();
			}
		}
	} catch (Exception e) {
		System.out.println("Failed - " + last);
		e.printStackTrace();
	}
}
 */
