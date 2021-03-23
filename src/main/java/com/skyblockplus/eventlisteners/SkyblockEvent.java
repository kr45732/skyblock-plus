package com.skyblockplus.eventlisteners;

import com.google.gson.JsonElement;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import java.util.Map;
import java.util.TreeMap;

import static com.skyblockplus.utils.Utils.*;

public class SkyblockEvent {
    private EmbedBuilder eb;
    private TextChannel announcementChannel;
    private Map<Integer, String>  prizeListMap;

    public boolean isEnable() {
        return enable;
    }

    private boolean enable = false;
    private CommandEvent commandEvent;
    private int state = 0;
    private JsonElement guildJson;
    private String eventType;

    public SkyblockEvent() {
        this.enable = false;
    }

    public SkyblockEvent(CommandEvent commandEvent) {
        this.enable = true;
        this.commandEvent = commandEvent;
        this.eb = defaultEmbed("Create a Skyblock competition");

    }

    private void sendEmbedMessage(EmbedBuilder eb){
        commandEvent.getChannel().sendMessage(eb.build()).queue();
    }

    public String onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if(!enable){
            return "false";
        }

        if(!commandEvent.getChannel().equals(event.getChannel())){
            return "false";
        }

        if(!commandEvent.getAuthor().equals(event.getAuthor())){
            return "false";
        }

        if(event.getMessage().getContentRaw().equalsIgnoreCase("exit")){
            sendEmbedMessage(defaultEmbed("Create a Skyblock competition").setDescription("Canceling..."));
            return "delete";
        }

        switch (state) {
            case 0:
                try {
                    guildJson = getJson("https://api.hypixel.net/guild?key=" + HYPIXEL_API_KEY + "&name=" + event.getMessage().getContentRaw().replace(" ", "%20"));
                    eb.addField("Guild", "Name: " + higherDepth(higherDepth(guildJson, "guild"), "name").getAsString() + "\nMembers: " + higherDepth(higherDepth(guildJson, "guild"), "members").getAsJsonArray().size(), false).setDescription("Is this a __catacombs__, __slayer__, or __skills__ event?");
                    state++;
                } catch (Exception e) {
                    eb.setDescription("Invalid guild name: " + event.getMessage().getContentRaw());
                }
                sendEmbedMessage(eb);
                break;
            case 1:
                String replyMessage = event.getMessage().getContentRaw().toLowerCase();
                if (replyMessage.equals("catacombs") || replyMessage.equals("slayer") || replyMessage.equals("skills")) {
                    if (replyMessage.equals("catacombs")) {
                        eb.addField("Event Type", "Catacombs", false);
                        eventType = "catacombs";
                    } else if (replyMessage.equals("slayer")) {
                        eb.addField("Event Type", "Slayer", false);
                        eventType = "slayer";
                    } else {
                        eb.addField("Event Type", "Skills", false);
                        eventType = "skills";
                    }
                    eb.setDescription("If there are no prizes please type \"none\", else please enter the prizes in one message following the format in the example below (place number - prize):\n1 - 15 mil coins\n2 - 10 mil\n3 - 500k");
                    state ++;
                } else {
                    eb.setDescription(replyMessage + " is an invalid option\nPlease choose from catacombs, slayer, or skills");
                }
                sendEmbedMessage(eb);
                break;
            case 2:
                replyMessage = event.getMessage().getContentRaw().toLowerCase();
                if(replyMessage.equals("none")){
                    eb.addField("Prizes", "None", false);
                }else{
                    String[] prizeList = replyMessage.split("\n");
                    prizeListMap = new TreeMap<>();
                    for(String prizeLevel: prizeList){
                        try{
                            String[] prizeLevelArr = prizeLevel.split("-");
                            prizeListMap.put(Integer.parseInt(prizeLevelArr[0].trim()), prizeLevelArr[1].trim());
                        } catch (Exception ignored){}
                    }

                    System.out.println(prizeListMap);

                    StringBuilder ebString = new StringBuilder();
                    for(Map.Entry<Integer, String> prize:prizeListMap.entrySet()){
                        ebString.append("• ").append(prize.getKey()).append(") - ").append(prize.getValue()).append("\n");
                    }


                    eb.addField("Prizes", ebString.toString(), false);
                    eb.setDescription("Please enter the channel for the announcement or \"none\" for no announcement");
                }
                state ++;
                sendEmbedMessage(eb);
                break;
            case 3:
                replyMessage = event.getMessage().getContentRaw().toLowerCase();
                if(replyMessage.equals("none")) {
                    eb.addField("Announcement Channel", "None", false);
                    eb.setDescription("Please confirm the event by replying with \"yes\" or anything else to cancel");
                    sendEmbedMessage(eb);
                    state ++;
                }else{
                    try{
                        System.out.println(replyMessage.replaceAll("[<#>]", ""));
                        announcementChannel = event.getGuild().getTextChannelById(replyMessage.replaceAll("[<#>]", ""));
                        eb.addField("Announcement Channel", "None", false);
                        eb.setDescription("Please confirm the event by replying with \"yes\" or anything else to cancel");
                        state ++;
                    }catch (Exception e){
                        eb.setDescription("Invalid channel. Please make sure the bot can see the channel and it is a valid channel!");
                    }
                    sendEmbedMessage(eb);
                }
                break;
            case 4:
                if(event.getMessage().getContentRaw().equalsIgnoreCase("yes")){
                    sendEmbedMessage(eb.setDescription("Creating event..."));
                }else{
                    sendEmbedMessage(defaultEmbed("Create a Skyblock competition").setDescription("Canceling..."));
                }
                return "delete";
        }
        return "true";
    }
}
