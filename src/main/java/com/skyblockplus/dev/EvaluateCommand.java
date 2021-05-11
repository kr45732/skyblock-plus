package com.skyblockplus.dev;

import static com.skyblockplus.Main.database;
import static com.skyblockplus.eventlisteners.MainListener.guildMap;
import static com.skyblockplus.utils.Utils.logCommand;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EvaluateCommand extends Command {
    private final ScriptEngine engine;
    private int count = 0;

    public EvaluateCommand() {
        this.name = "evaluate";
        this.ownerCommand = true;
        this.aliases = new String[] { "eval", "ev" };

        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("var imports = new JavaImporter(" + "java.io," + "java.lang," + "java.util,"
                    + "Packages.net.dv8tion.jda.api," + "Packages.net.dv8tion.jda.api.entities,"
                    + "Packages.net.dv8tion.jda.api.entities.impl," + "Packages.net.dv8tion.jda.api.managers,"
                    + "Packages.net.dv8tion.jda.api.managers.impl," + "Packages.net.dv8tion.jda.api.utils);");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            Message ebMessage = event.getChannel().sendMessage("Loading").complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split("\\s+");
            count++;

            logCommand(event.getGuild(), event.getAuthor(), content);
            MessageReceivedEvent jdaEvent = event.getEvent();
            try {
                engine.put("event", jdaEvent);
                engine.put("message", jdaEvent.getMessage());
                engine.put("channel", jdaEvent.getChannel());
                engine.put("args", args);
                engine.put("jda", jdaEvent.getJDA());
                engine.put("guilds", guildMap);
                engine.put("db", database);
                if (jdaEvent.isFromType(ChannelType.TEXT)) {
                    engine.put("guild", jdaEvent.getGuild());
                    engine.put("member", jdaEvent.getMember());
                }

                Object out = engine.eval("(function() {" + "with (imports) {return"
                        + jdaEvent.getMessage().getContentDisplay().substring(args[0].length()) + "}" + "})();");
                if (out == null) {
                    ebMessage.editMessage("Success (null output)").queue();
                } else if (out.toString().length() >= 2000) {
                    String pathName = "src/main/java/com/skyblockplus/json/" + count + "_eval_cmd.json";
                    File file = new File(pathName);
                    if (!file.createNewFile()) {
                        file.delete();
                        file.createNewFile();
                    }

                    Writer writer = new FileWriter(pathName);
                    try {
                        new GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(out.toString()),
                                writer);
                    } catch (Exception e) {
                        writer.write(out.toString());
                    }
                    writer.close();

                    ebMessage.delete().queue();
                    event.getChannel().sendFile(file).queue();
                    file.delete();
                } else {
                    ebMessage.editMessage(out.toString()).queue();
                }
            } catch (Exception e) {
                ebMessage.editMessage(e.getMessage()).queue();
            }
        }).start();
    }
}
