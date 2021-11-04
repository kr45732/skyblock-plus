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
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.*;
import com.skyblockplus.features.listeners.MainListener;
import com.skyblockplus.features.setup.SetupCommand;
import com.skyblockplus.features.setup.SetupSlashCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.features.skyblockevent.SkyblockEventSlashCommand;
import com.skyblockplus.guilds.*;
import com.skyblockplus.help.HelpCommand;
import com.skyblockplus.help.HelpSlashCommand;
import com.skyblockplus.inventory.*;
import com.skyblockplus.link.LinkCommand;
import com.skyblockplus.link.LinkSlashCommand;
import com.skyblockplus.link.UnlinkCommand;
import com.skyblockplus.link.UnlinkSlashCommand;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.networth.NetworthCommand;
import com.skyblockplus.networth.NetworthSlashCommand;
import com.skyblockplus.price.*;
import com.skyblockplus.settings.Database;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.skills.HotmCommand;
import com.skyblockplus.skills.HotmSlashCommand;
import com.skyblockplus.skills.SkillsCommand;
import com.skyblockplus.skills.SkillsSlashCommand;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
import com.skyblockplus.utils.ApiHandler;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.exceptionhandler.ExceptionEventListener;
import com.skyblockplus.utils.exceptionhandler.GlobalExceptionHandler;
import com.skyblockplus.utils.slashcommand.SlashCommandClient;
import com.skyblockplus.weight.WeightCommand;
import com.skyblockplus.weight.WeightSlashCommand;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
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
						.addCommands(
								new InformationCommand(),
								new SlayerCommand(),
								new HelpCommand(),
								new GuildCommand(),
								new AuctionCommand(),
								new BinCommand(),
								new SkillsCommand(),
								new DungeonsCommand(),
								new ShutdownCommand(),
								new RolesCommand(),
								new GuildRanksCommand(),
								new EssenceCommand(),
								new BankCommand(),
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
								new QuickSetupTestCommand(),
								new EmojiMapServerCommand(),
								new EnderChestCommand(),
								new GetAllGuildsIn(),
								new LinkCommand(),
								new GetSettingsFile(),
								new UnlinkCommand(),
								new LinkedUserDev(),
								new BazaarCommand(),
								new AverageAuctionCommand(),
								new PetsCommand(),
								new SkyblockEventCommand(),
								new DeleteMessagesCommand(),
								new PlaceholderCommand(),
								new ProfilesCommand(),
								new NetworthCommand(),
								new QueryAuctionCommand(),
								new BidsCommand(),
								new BitsCommand(),
								new EvaluateCommand(),
								new GuildKickerCommand(),
								new MissingTalismansCommand(),
								new UpdateSlashCommands(),
								new CalculateCommand(),
								new EmojiFromUrlCommand(),
								new GuildLeaderboardCommand(),
								new ArmorCommand(),
								new FetchurCommand(),
								new CheckEmojisCommand(),
								new HarpCommand(),
								new CakesCommand(),
								new ActiveCoinsCommand(),
								new HotmCommand(),
								new VoteCommand(),
								new TrackAuctionsCommand(),
								new SkyblockCommand(),
								new GuildStatisticsCommand(),
								new GuildTrackerCommand()
						)
						.build();

		slashCommandClient =
				new SlashCommandClient()
						.addSlashCommands(
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
								new QueryAuctionsSlashCommand(),
								new BitsSlashCommand(),
								new RolesSlashCommand(),
								new BankSlashCommand(),
								new WeightSlashCommand(),
								new HypixelSlashCommand(),
								new ProfilesSlashCommand(),
								new MissingTalismansSlashCommand(),
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
								new GuildTrackerSlashCommand()
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
						.disableCache(CacheFlag.VOICE_STATE)
						.enableIntents(GatewayIntent.GUILD_MEMBERS)
						.build();

		try {
			jda.awaitReady();
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
		jda.getPresence().setActivity(Activity.watching(DEFAULT_PREFIX + "help in " + jda.getGuilds().size() + " servers"));

		if (!IS_API) {
			ApiHandler.initialize();
			TrackAuctionsCommand.initialize();
			GuildTrackerCommand.initialize();
		}
		//		AuctionFlipper.scheduleFlipper();
	}

	@PreDestroy
	public void onExit() {
		log.info("Stopping");

		log.info("Caching Apply Users");
		cacheApplyGuildUsers();

		log.info("Closing Http Client");
		closeHttpClient();

		log.info("Closing Async Http Client");
		closeAsyncHttpClient();

		log.info("Finished");
	}
}
/*
worker: java $JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseStringDeduplication -jar build/libs/SkyblockPlus-0.0.1.jar
web: java -Dserver.port=${PORT} -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -XX:+UseStringDeduplication -jar build/libs/SkyblockPlus-0.0.1.jar
*/
