package com.skyblockplus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.auctionbaz.AuctionCommand;
import com.skyblockplus.auctionbaz.AverageAuctionCommand;
import com.skyblockplus.auctionbaz.BazaarCommand;
import com.skyblockplus.auctionbaz.BinCommand;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.CatacombsCommand;
import com.skyblockplus.dungeons.EssenceCommand;
import com.skyblockplus.dungeons.PartyFinderCommand;
import com.skyblockplus.eventlisteners.AutomaticGuild;
import com.skyblockplus.eventlisteners.MainListener;
import com.skyblockplus.eventlisteners.apply.ApplyUser;
import com.skyblockplus.eventlisteners.skyblockevent.SkyblockEventCommand;
import com.skyblockplus.guilds.GuildCommand;
import com.skyblockplus.guilds.GuildLeaderboardCommand;
import com.skyblockplus.inventory.*;
import com.skyblockplus.link.LinkAccountCommand;
import com.skyblockplus.link.UnlinkAccountCommand;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.settings.SetupCommand;
import com.skyblockplus.settings.SpringDatabaseComponent;
import com.skyblockplus.skills.SkillsCommand;
import com.skyblockplus.slayer.SlayerCommand;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.weight.WeightCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.skyblockplus.eventlisteners.MainListener.getGuildMap;
import static com.skyblockplus.utils.Utils.*;

@SpringBootApplication
public class Main {
    public static JDA jda;
    public static SpringDatabaseComponent database;
    public static AsyncHttpClient asyncHttpClient = Dsl.asyncHttpClient();

    public static void main(String[] args) throws LoginException, IllegalArgumentException {
        setApplicationSettings();

        Main.database = SpringApplication.run(Main.class, args).getBean(SpringDatabaseComponent.class);

        EventWaiter waiter = new EventWaiter();
        CommandClientBuilder client = new CommandClientBuilder();
        client.setActivity(Activity.watching(BOT_PREFIX + "help"));
        client.setOwnerId("385939031596466176");
        client.setEmojis("✅", "⚠️", "❌");
        client.useHelpBuilder(false);
        client.setPrefix(BOT_PREFIX);

        client.addCommands(new InformationCommand(), new SlayerCommand(), new HelpCommand(waiter), new GuildCommand(waiter),
                new AuctionCommand(), new BinCommand(), new SkillsCommand(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand(), new RoleCommands(), new GuildLeaderboardCommand(),
                new EssenceCommand(), new BankCommand(waiter), new WardrobeCommand(waiter),
                new TalismanBagCommand(waiter), new InventoryCommand(waiter), new SacksCommand(waiter), new InviteCommand(),
                new WeightCommand(), new HypixelCommand(), new UuidCommand(), new SkyblockCommand(waiter),
                new BaldCommand(), new SettingsCommand(waiter), new ReloadCommand(), new SetupCommand(waiter),
                new CategoriesCommand(), new PartyFinderCommand(), new QuickSetupTestCommand(), new EmojiMapServerCommand(),
                new EnderChestCommand(), new InstantTimeNow(), new GetEventListenersCommand(), new GetAllGuildsIn(waiter),
                new LinkAccountCommand(), new GetSettingsFile(), new UnlinkAccountCommand(), new LinkedUserDev(),
                new BazaarCommand(), new AverageAuctionCommand(), new PetsCommand(waiter), new SkyblockEventCommand(),
                new DeleteMessagesCommand(), new PlaceholderCommand(), new ProfilesCommand(waiter), new NetworthCommand());

        if (BOT_PREFIX.equals("+")) {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new MessageTimeout(), new MainListener())
                    .build();
        } else {
            jda = JDABuilder.createDefault(BOT_TOKEN).setStatus(OnlineStatus.DO_NOT_DISTURB)
                    .setChunkingFilter(ChunkingFilter.ALL).setMemberCachePolicy(MemberCachePolicy.ALL)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS).setActivity(Activity.playing("Loading..."))
                    .addEventListeners(waiter, client.build(), new MessageTimeout(), new MainListener())
                    .build();
        }
    }

    public static void cacheApplyGuildUsers() {
        if (!BOT_PREFIX.equals("+")) {
            return;
        }

        for (Map.Entry<String, AutomaticGuild> automaticGuild : getGuildMap().entrySet()) {
            try {
                database.deleteApplyCacheSettings(automaticGuild.getKey());
                List<ApplyUser> applyUserList = automaticGuild.getValue().getApplyGuild().getApplyUserList();
                if (applyUserList.size() > 0) {
                    int code = database.updateApplyCacheSettings(automaticGuild.getKey(), new Gson().toJson(applyUserList));

                    if (code == 200) {
                        System.out.println("Successfully cached ApplyUser | " + automaticGuild.getKey() + " | " + applyUserList.size());
                    }
                }
            } catch (Exception e) {
                System.out.println("== Stack Trace (Cache ApplyUser - " + automaticGuild.getKey() + ")");
                e.printStackTrace();
            }
        }

    }

    public static List<ApplyUser> getApplyGuildUsersCache(String guildId) {
        if (!BOT_PREFIX.equals("+")) {
            return new ArrayList<>();
        }

        try {
            JsonArray applyUsersCache = database.getApplyCacheSettings(guildId).getAsJsonArray();

            List<ApplyUser> applyUsersCacheList = new ArrayList<>();
            for (JsonElement applyUserCache : applyUsersCache) {
                ApplyUser currentApplyUserCache = new Gson().fromJson(applyUserCache, ApplyUser.class);
                applyUsersCacheList.add(currentApplyUserCache);
            }
            if (applyUsersCacheList.size() > 0) {
                System.out.println("Retrieved cache (" + applyUsersCacheList.size() + ") - " + guildId);
                return applyUsersCacheList;
            }

            database.deleteApplyCacheSettings(guildId);
        } catch (Exception e) {
            System.out.println("== Stack Trace (Get cache ApplyUser - " + guildId + ")");
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    @PreDestroy
    public void onExit() {
        System.out.println("== Stopping ==");

        System.out.println("== Saving ==");
        long startTime = System.currentTimeMillis();
        cacheApplyGuildUsers();
        System.out.println("== Saved In " + ((System.currentTimeMillis() - startTime) / 1000) + "s ==");

        System.out.println("== Closing Async Http Client ==");
        try {
            asyncHttpClient.close();
            System.out.println("== Successfully Closed Async Http Client ==");
        } catch (Exception e) {
            System.out.println("== Stack Trace (Close Async Http Client)");
            e.printStackTrace();
        }

        System.out.println("== Finished ==");
    }
}
