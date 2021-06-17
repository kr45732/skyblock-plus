package com.skyblockplus.miscellaneous;

import com.skyblockplus.utils.slashcommands.SlashCommand;
import com.skyblockplus.utils.slashcommands.SlashCommandExecutedEvent;
import net.dv8tion.jda.api.EmbedBuilder;

public class RolesSlashCommand extends SlashCommand {

    public RolesSlashCommand() {
        this.name = "roles";
    }

    @Override
    protected void execute(SlashCommandExecutedEvent event) {
        new Thread(
                () -> {
                    event.logCommandGuildUserCommand();

                    EmbedBuilder eb = RoleCommands.updateRoles(
                            event.getOptionStr("profile"),
                            event.getGuild(),
                            event.getUser(),
                            event.getMember()
                    );

                    event.getHook().editOriginalEmbeds(eb.build()).queue();
                }
        )
                .start();
    }
}
