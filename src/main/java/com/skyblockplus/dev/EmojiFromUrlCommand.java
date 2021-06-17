package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Icon;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.skyblockplus.utils.Utils.logCommand;

public class EmojiFromUrlCommand extends Command {

    private final List<String> allowedUsers = Arrays.asList("385939031596466176", "409889861441421315");

    public EmojiFromUrlCommand() {
        this.name = "em";
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(
                () -> {
                    if (!allowedUsers.contains(event.getAuthor().getId())) {
                        return;
                    }

                    if (!event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
                        return;
                    }

                    String content = event.getMessage().getContentRaw();
                    String[] args = content.split(" ");
                    logCommand(event.getGuild(), event.getAuthor(), content);

                    try {
                        String[] itemNamesArr = args[1].split("/");
                        String itemName = args.length == 2 ? itemNamesArr[itemNamesArr.length - 1].split("\\.")[0] : args[2];
                        URL url = new URL("https://sky.shiiyu.moe" + args[1].replace("https://sky.shiiyu.moe", ""));
                        Emote emote = event.getGuild().createEmote(itemName, Icon.from(url.openStream())).complete();
                        event.getChannel().sendMessage(emote.getAsMention()).queue();
                    } catch (Exception e) {
                        event.getChannel().sendMessage("Error\n" + e.getMessage()).queue();
                        e.printStackTrace();
                    }
                }
        )
                .start();
    }
}
