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

package com.skyblockplus;

import static com.skyblockplus.utils.utils.Utils.*;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.controller.ApiController;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import com.skyblockplus.features.event.EventHandler;
import com.skyblockplus.features.fetchur.FetchurHandler;
import com.skyblockplus.features.jacob.JacobHandler;
import com.skyblockplus.features.listeners.MainListener;
import com.skyblockplus.features.mayor.MayorHandler;
import com.skyblockplus.price.AuctionTracker;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.AuctionFlipper;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandClient;
import com.skyblockplus.utils.database.Database;
import com.skyblockplus.utils.exceptionhandler.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.oauth.OAuthClient;
import com.skyblockplus.utils.utils.HttpUtils;
import com.skyblockplus.utils.utils.Utils;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.groovy.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.firewall.HttpStatusRequestRejectedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;

@SpringBootApplication
public class Main {

	public static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws IllegalArgumentException {
		globalExceptionHandler = new GlobalExceptionHandler();
		RestAction.setDefaultFailure(e -> globalExceptionHandler.uncaughtException(Thread.currentThread(), e));
		Message.suppressContentIntentWarning();

		Utils.initialize();
		ApiHandler.initializeConstants();
		Constants.initialize();
		selfUserId = isMainBot() ? "796791167366594592" : "799042642092228658";

		SpringApplication springApplication = new SpringApplication(Main.class);
		springApplication.setDefaultProperties(Maps.of("spring.datasource.url", DATABASE_URL));
		springContext = springApplication.run(args);
		database = springContext.getBean(Database.class);
		waiter = new EventWaiter(scheduler, true);
		client =
			new CommandClientBuilder()
				.setPrefix(DEFAULT_PREFIX)
				.setAlternativePrefix("@mention")
				.setOwnerId("385939031596466176")
				.setEmojis("<:yes:948359788889251940>", "⚠️", "<:no:948359781125607424>")
				.useHelpBuilder(false)
				.setListener(
					new CommandListener() {
						@Override
						public void onCommandException(CommandEvent event, Command command, Throwable throwable) {
							globalExceptionHandler.uncaughtException(event, command, throwable);
						}
					}
				)
				.setCommandPreProcessBiFunction((event, command) -> event.isFromGuild())
				.setActivity(Activity.playing("Loading..."))
				.setManualUpsert(true)
				.addCommands(springContext.getBeansOfType(Command.class).values().toArray(new Command[0]))
				.build();

		slashCommandClient =
			new SlashCommandClient().setOwnerId(client.getOwnerId()).addCommands(springContext.getBeansOfType(SlashCommand.class).values());

		oAuthClient = new OAuthClient(selfUserId, CLIENT_SECRET);

		log.info(
			"Loaded " + client.getCommands().size() + " prefix commands and " + slashCommandClient.getCommands().size() + " slash commands"
		);

		allServerSettings =
			isMainBot()
				? database.getAllServerSettings().stream().collect(Collectors.toMap(ServerSettingsModel::getServerId, Function.identity()))
				: Stream
					.of("796790757947867156", "782154976243089429", "869217817680044042")
					.collect(Collectors.toMap(Function.identity(), e -> database.getServerSettingsModel(e), (e1, e2) -> e1));
		log.info("Loaded all server settings");

		jda =
			DefaultShardManagerBuilder
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
				.setEnableShutdownHook(false)
				.build();

		for (JDA shard : jda.getShards()) {
			try {
				shard.awaitReady();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		File loreRendersDir = new File("src/main/java/com/skyblockplus/json/renders/");
		if (!loreRendersDir.exists()) {
			log.info((loreRendersDir.mkdirs() ? "Successfully created" : "Failed to create") + " lore render directory");
		} else {
			File[] loreRendersDirFiles = loreRendersDir.listFiles();
			if (loreRendersDirFiles != null) {
				Arrays.stream(loreRendersDirFiles).forEach(File::delete);
			}
		}

		ApiHandler.initialize();
		AuctionTracker.initialize();
		AuctionFlipper.initialize(isMainBot());
		ApiController.initialize();
		FetchurHandler.initialize();
		scheduler.scheduleWithFixedDelay(MayorHandler::initialize, 1, 5, TimeUnit.MINUTES);
		JacobHandler.initialize();
		EventHandler.initialize();

		if (isMainBot()) {
			scheduler.scheduleWithFixedDelay(
				() -> {
					if (Runtime.getRuntime().totalMemory() > 1250000000) {
						System.gc();
					}
				},
				60,
				30,
				TimeUnit.SECONDS
			); // Sorry for the war crimes
		}

		Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
		log.info("Bot ready with " + jda.getShardsTotal() + " shards and " + jda.getGuilds().size() + " guilds");
	}

	@PreDestroy
	public void onExit() {
		log.info("Stopping");

		if (isMainBot()) {
			botStatusWebhook.send(client.getSuccess() + " Restarting for an update");
		}

		ApiHandler.updateCaches();
		HttpUtils.closeHttpClient();
		ApiHandler.leaderboardDatabase.close();

		log.info("Finished");
	}

	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HttpStatusRequestRejectedHandler();
	}
}
