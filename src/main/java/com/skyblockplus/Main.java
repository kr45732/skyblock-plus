package com.skyblockplus;

import static com.skyblockplus.utils.MainClassUtils.*;
import static com.skyblockplus.utils.Utils.*;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.*;
import com.skyblockplus.eventlisteners.MainListener;
import com.skyblockplus.eventlisteners.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.guilds.*;
import com.skyblockplus.help.HelpCommand;
import com.skyblockplus.help.HelpSlashCommand;
import com.skyblockplus.inventory.*;
import com.skyblockplus.link.LinkAccountCommand;
import com.skyblockplus.link.LinkSlashCommand;
import com.skyblockplus.link.UnlinkAccountCommand;
import com.skyblockplus.link.UnlinkSlashCommand;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.networth.NetworthCommand;
import com.skyblockplus.price.*;
import com.skyblockplus.settings.Database;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.settings.SetupCommand;
import com.skyblockplus.skills.SkillsCommand;
import com.skyblockplus.skills.SkillsSlashCommand;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.slayer.SlayerSlashCommand;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.utils.slashcommands.SlashCommandClient;
import com.skyblockplus.weight.WeightCommand;
import com.skyblockplus.weight.WeightSlashCommand;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main {

	public static final AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();
	public static final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	public static final ExecutorService executor = Executors.newCachedThreadPool();
	public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
	public static JDA jda;
	public static Database database;
	public static EventWaiter waiter;

	public static void main(String[] args) throws LoginException, IllegalArgumentException {
		setApplicationSettings();

		Main.database = SpringApplication.run(Main.class, args).getBean(Database.class);

		Main.waiter = new EventWaiter();
		CommandClientBuilder client = new CommandClientBuilder();
		client.setActivity(Activity.watching(BOT_PREFIX + "help"));
		client.setOwnerId("385939031596466176");
		client.setEmojis("✅", "⚠️", "❌");
		client.useHelpBuilder(false);
		client.setPrefix(BOT_PREFIX);
		client.addCommands(
			new InformationCommand(),
			new SlayerCommand(),
			new HelpCommand(),
			new GuildCommand(),
			new AuctionCommand(),
			new BinCommand(),
			new SkillsCommand(),
			new DungeonsCommand(),
			new ShutdownCommand(),
			new RoleCommands(),
			new GuildRanksCommand(),
			new EssenceCommand(),
			new BankCommand(),
			new WardrobeCommand(),
			new TalismanBagCommand(),
			new InventoryCommand(),
			new SacksCommand(),
			new InviteCommand(),
			new WeightCommand(),
			new HypixelCommand(),
			new UuidCommand(),
			new SkyblockCommand(),
			new BaldCommand(),
			new SettingsCommand(),
			new ReloadCommand(),
			new SetupCommand(),
			new CategoriesCommand(),
			new PartyFinderCommand(),
			new QuickSetupTestCommand(),
			new EmojiMapServerCommand(),
			new EnderChestCommand(),
			new InstantTimeNow(),
			new GetEventListenersCommand(),
			new GetAllGuildsIn(),
			new LinkAccountCommand(),
			new GetSettingsFile(),
			new UnlinkAccountCommand(),
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
			new GetThreadPools(),
			new BitsCommand(),
			new EvaluateCommand(),
			new GuildRequirementsCommand(),
			new GuildKickerCommand(),
			new MissingTalismansCommand(),
			new UpdateSlashCommands(),
			new PriceCommand(),
			new EmojiFromUrlCommand(),
			new GuildLeaderboardsCommand(),
			new ArmorCommand()
		);

		SlashCommandClient slashCommands = new SlashCommandClient();
		slashCommands.addSlashCommands(
			new InviteSlashCommand(),
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
			new PriceSlashCommand()
		);

		jda =
			JDABuilder
				.createDefault(BOT_TOKEN)
				.setStatus(OnlineStatus.DO_NOT_DISTURB)
				.addEventListeners(waiter, client.build(), new MessageTimeout(), new MainListener(), slashCommands)
				.setActivity(Activity.playing("Loading..."))
				.build();

		try {
			jda.awaitReady();
		} catch (Exception ignored) {}

		jda.getPresence().setActivity(Activity.watching(BOT_PREFIX + "help in " + jda.getGuilds().size() + " servers"));
		//		scheduleUpdateLinkedAccounts();
		//		AuctionFlipper.scheduleFlipper();
	}

	@PreDestroy
	public void onExit() {
		System.out.println("== Stopping ==");

		System.out.println("== Caching Apply Users ==");
		cacheApplyGuildUsers();

		System.out.println("== Closing Http Client ==");
		closeHttpClient();

		System.out.println("== Closing Async Http Client ==");
		closeAsyncHttpClient();

		System.out.println("== Finished ==");
	}
}
