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

import static com.skyblockplus.features.listeners.MainListener.guildMap;
import static com.skyblockplus.utils.ApiHandler.leaderboardDatabase;
import static com.skyblockplus.utils.ApiHandler.updateCacheTask;
import static com.skyblockplus.utils.Utils.*;

import club.minnced.discord.webhook.external.JDAWebhookClient;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.api.miscellaneous.PublicEndpoints;
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
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.SlashCommand;
import com.skyblockplus.utils.command.SlashCommandClient;
import com.skyblockplus.utils.database.Database;
import com.skyblockplus.utils.exceptionhandler.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
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

	public static void main(String[] args) throws LoginException, IllegalArgumentException {
		globalExceptionHandler = new GlobalExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
		RestAction.setDefaultFailure(e -> globalExceptionHandler.uncaughtException(null, e));

		Utils.initialize();
		Constants.initialize();
		selfUserId = isMainBot() ? "796791167366594592" : "799042642092228658";

		springContext = SpringApplication.run(Main.class, args);
		database = springContext.getBean(Database.class);
		waiter = new EventWaiter(scheduler, true);
		client =
			new CommandClientBuilder()
				.setOwnerId("385939031596466176")
				.setEmojis("<:yes:948359788889251940>", "⚠️", "<:no:948359781125607424>")
				.useHelpBuilder(false)
				.setPrefixFunction(event -> DEFAULT_PREFIX)
				.setListener(
					new CommandListener() {
						@Override
						public void onCommandException(CommandEvent event, Command command, Throwable throwable) {
							globalExceptionHandler.uncaughtException(event, command, throwable);
						}
					}
				)
				.setCommandPreProcessBiFunction((event, command) ->
					!event.isFromGuild() || !guildMap.get(event.getGuild().getId()).channelBlacklist.contains(event.getChannel().getId())
				)
				.setActivity(Activity.playing("Loading..."))
				.setManualUpsert(true)
				.addCommands(springContext.getBeansOfType(Command.class).values().toArray(new Command[0]))
				.build();

		slashCommandClient =
			new SlashCommandClient().setOwnerId(client.getOwnerId()).addCommands(springContext.getBeansOfType(SlashCommand.class).values());

		log.info(
			"Loaded " + client.getCommands().size() + " prefix commands and " + slashCommandClient.getCommands().size() + " slash commands"
		);

		allServerSettings =
			gson
				.toJsonTree(
					database
						.getAllServerSettings()
						.stream()
						.collect(Collectors.toMap(ServerSettingsModel::getServerId, Function.identity()))
				)
				.getAsJsonObject();

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

		MainListener.initialize();
		ApiHandler.initialize();
		AuctionTracker.initialize();
		AuctionFlipper.initialize(true);
		PublicEndpoints.initialize();
		FetchurHandler.initialize();
		MayorHandler.initialize();
		JacobHandler.initialize();
		EventHandler.initialize();

		File loreRendersDir = new File("src/main/java/com/skyblockplus/json/lore_renders/");
		if (!loreRendersDir.exists()) {
			log.info((loreRendersDir.mkdirs() ? "Successfully created" : "Failed to create") + " lore render directory");
		} else {
			File[] loreRendersDirFiles = loreRendersDir.listFiles();
			if (loreRendersDirFiles != null) {
				Arrays.stream(loreRendersDirFiles).forEach(File::delete);
			}
		}

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

			scheduler.schedule(ApiHandler::updateBotStatistics, 90, TimeUnit.SECONDS);
		}
	}

	@PreDestroy
	public void onExit() {
		if (isMainBot()) {
			try (JDAWebhookClient webhook = botStatusWebhook) {
				webhook.send(client.getSuccess() + " Restarting for an update");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		log.info("Stopping");

		log.info("Canceling cache update future: " + updateCacheTask.cancel(true));

		log.info("Canceling leaderboard update task: " + leaderboardDatabase.updateTask.cancel(true));

		log.info("Caching Apply Users");
		cacheApplyGuildUsers();

		log.info("Caching Parties");
		cacheParties();

		log.info("Caching Command Uses");
		cacheCommandUses();

		log.info("Caching Auction Tracker");
		cacheAhTracker();

		log.info("Caching Jacob Data");
		cacheJacobData();

		log.info("Closing Http Client");
		closeHttpClient();

		log.info("Closing leaderboard database");
		leaderboardDatabase.close();

		log.info("Finished");
	}

	@Bean
	public RequestRejectedHandler requestRejectedHandler() {
		return new HttpStatusRequestRejectedHandler();
	}
}
