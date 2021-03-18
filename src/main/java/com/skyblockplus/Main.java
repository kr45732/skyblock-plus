package com.skyblockplus;

import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.skyblockplus.auctionbaz.AuctionCommands;
import com.skyblockplus.auctionbaz.BazaarCommand;
import com.skyblockplus.auctionbaz.BinCommands;
import com.skyblockplus.dev.*;
import com.skyblockplus.dungeons.CatacombsCommand;
import com.skyblockplus.dungeons.EssenceCommand;
import com.skyblockplus.dungeons.PartyFinderCommand;
import com.skyblockplus.eventlisteners.AutomaticGuild;
import com.skyblockplus.eventlisteners.MainListener;
import com.skyblockplus.eventlisteners.apply.ApplyUser;
import com.skyblockplus.guilds.GuildCommands;
import com.skyblockplus.guilds.GuildLeaderboardCommand;
import com.skyblockplus.inventory.*;
import com.skyblockplus.link.LinkAccountCommand;
import com.skyblockplus.link.UnlinkAccountCommand;
import com.skyblockplus.miscellaneous.*;
import com.skyblockplus.reload.ReloadCommand;
import com.skyblockplus.roles.RoleCommands;
import com.skyblockplus.settings.SettingsCommand;
import com.skyblockplus.settings.SetupCommand;
import com.skyblockplus.settings.SpringDatabaseComponent;
import com.skyblockplus.skills.SkillsCommands;
import com.skyblockplus.slayer.SlayerCommands;
import com.skyblockplus.timeout.MessageTimeout;
import com.skyblockplus.weight.WeightCommand;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PreDestroy;
import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.skyblockplus.eventlisteners.MainListener.getGuildMap;
import static com.skyblockplus.utils.Utils.*;

@SpringBootApplication
public class Main {
    public static JDA jda;
    public static SpringDatabaseComponent database;

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

        client.addCommands(new AboutCommand(), new SlayerCommands(), new HelpCommand(waiter), new GuildCommands(waiter),
                new AuctionCommands(), new BinCommands(), new SkillsCommands(), new CatacombsCommand(),
                new ShutdownCommand(), new VersionCommand(), new RoleCommands(), new GuildLeaderboardCommand(),
                new EssenceCommand(), new BankCommand(waiter), new WardrobeCommand(waiter),
                new TalismanBagCommand(waiter), new InventoryCommand(waiter), new SacksCommand(waiter), new InviteCommand(),
                new WeightCommand(), new HypixelCommand(), new UuidCommand(), new SkyblockCommand(waiter),
                new BaldCommand(), new SettingsCommand(waiter), new ReloadCommand(), new SetupCommand(waiter),
                new CategoriesCommand(), new PartyFinderCommand(), new QuickSetupTestCommand(), new EmojiMapServerCommand(),
                new EnderChestCommand(), new InstantTimeNow(), new GetEventListenersCommand(), new GetAllGuildsIn(waiter),
                new LinkAccountCommand(), new GetSettingsFile(), new UnlinkAccountCommand(), new RemoveLinkedUserDev(),
                new BazaarCommand());

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
//        if(!BOT_PREFIX.equals("+")){
//            return;
//        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            for (Map.Entry<String, AutomaticGuild> automaticGuild : getGuildMap().entrySet()) {
                try {
                    database.updateApplyCacheSettings(automaticGuild.getKey(), new byte[]{});
                    if (automaticGuild.getValue().getApplyGuild().getApplyUserList().size() != 0) {
                        objectOutputStream.writeObject(automaticGuild.getValue().getApplyGuild().getApplyUserList());
                        database.updateApplyCacheSettings(automaticGuild.getKey(), byteArrayOutputStream.toByteArray());
                        System.out.println("Cached " + automaticGuild.getKey() + " ApplyUser (" + automaticGuild.getValue().getApplyGuild().getApplyUserList().size() + ")");
                        objectOutputStream.reset();
                        byteArrayOutputStream.reset();
                    }
                } catch (Exception e) {
                    System.out.println("Cache error " + automaticGuild.getKey());
                }
            }
            objectOutputStream.close();
        } catch (Exception e) {
            System.out.println("== Stack Trace ==");
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ApplyUser> getApplyGuildUsersCache(String guildId) {
        try {
            byte[] guildApplyCache = database.getApplyCacheSettings(guildId);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(guildApplyCache);
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            List<ApplyUser> allApplyUsers = (List<ApplyUser>) objectInputStream.readObject();

            List<ApplyUser> guildApplyUsers = new ArrayList<>();
            for (ApplyUser applyUser : allApplyUsers) {
                if (applyUser.getGuildId().equals(guildId)) {
                    guildApplyUsers.add(applyUser);
                }
            }
            objectInputStream.close();
            System.out.println("Retrieved cached " + guildId + " ApplyUser (" + guildApplyUsers.size() + ")");
            return guildApplyUsers;
        }catch (EOFException ignored){
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("== Stack Trace (" + guildId + ") ==");
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @PreDestroy
    public void onExit() {
        System.out.println("== STOPPING ==");

        System.out.println("== SAVING ==");
        long startTime = System.currentTimeMillis();
        cacheApplyGuildUsers();
        System.out.println("== SAVED IN " + ((System.currentTimeMillis() - startTime) / 1000) + "s ==");

        System.out.println("== FINISHED ==");
    }
}
