package com.skyblockplus.guilds;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class GuildSlashCommand extends SlashCommand {

    public GuildSlashCommand() {
        this.name = "guild";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    String subcommandName = event.getSubcommandName();
                    EmbedBuilder eb;
                    String username = event.getOptionStr("player");

                    switch (subcommandName) {
                        case "player":
                            eb = GuildCommand.getGuildPlayer(username);
                            break;
                        case "information":
                            eb = GuildCommand.getGuildInfo(username);
                            break;
                        case "members":
                            eb = GuildCommand.getGuildMembers(username, event.getUser(), null, event.getHook());
                            break;
                        case "experience":
                            eb = GuildCommand.getGuildExp(username, event.getUser(), null, event.getHook());
                            break;
                        default:
                            eb = event.invalidCommandMessage();
                    }

                    if (eb != null) {
                        event.getHook().editOriginalEmbeds(eb.build()).queue();
                    }
                }
        )
                .start();
    }
}
