package com.skyblockplus.dev;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import me.nullicorn.nedit.NBTReader;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;

import static com.skyblockplus.utils.Utils.*;

public class InvDataParsCommand extends Command {
    public InvDataParsCommand() {
        this.name = "d-parse";
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            EmbedBuilder eb = loadingEmbed();
            Message ebMessage = event.getChannel().sendMessage(eb.build()).complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split(" ");

            logCommand(event.getGuild(), event.getAuthor(), content);

            if(args.length == 2) {
                try {
                    NBTCompound decodedInvContents = NBTReader.readBase64(args[1]);
                    NBTList invSlotList = decodedInvContents.getList(".i");
                    ebMessage.editMessage(invSlotList.toString()).queue();
                } catch (Exception e) {
                    ebMessage.editMessage(defaultEmbed("Unable to decode inventory").build()).queue();
                }
                return;


            }

            ebMessage.editMessage(errorMessage(this.name).build()).queue();
        }).start();
    }
}
