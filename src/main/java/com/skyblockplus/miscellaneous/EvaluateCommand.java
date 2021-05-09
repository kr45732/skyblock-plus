package com.skyblockplus.miscellaneous;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import static com.skyblockplus.utils.Utils.*;

public class EvaluateCommand extends Command {
    private final ScriptEngine engine;

    public EvaluateCommand() {
        this.name = "evaluate";
        this.cooldown = globalCooldown;
        this.aliases = new String[]{"eval"};

        engine = new ScriptEngineManager().getEngineByName("nashorn");
        try
        {
            engine.eval("var imports = new JavaImporter(" +
                    "java.io," +
                    "java.lang," +
                    "java.util," +
                    "Packages.net.dv8tion.jda.api," +
                    "Packages.net.dv8tion.jda.api.entities," +
                    "Packages.net.dv8tion.jda.api.entities.impl," +
                    "Packages.net.dv8tion.jda.api.managers," +
                    "Packages.net.dv8tion.jda.api.managers.impl," +
                    "Packages.net.dv8tion.jda.api.utils);");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void execute(CommandEvent event) {
        new Thread(() -> {
            Message ebMessage = event.getChannel().sendMessage("Loading").complete();
            String content = event.getMessage().getContentRaw();
            String[] args = content.split("\\s+");

            logCommand(event.getGuild(), event.getAuthor(), content);
            MessageReceivedEvent jdaEvent = event.getEvent();
            try
            {
                engine.put("event", jdaEvent);
                engine.put("message", jdaEvent.getMessage());
                engine.put("channel", jdaEvent.getChannel());
                engine.put("args", args);
                engine.put("api", jdaEvent.getJDA());
                if (jdaEvent.isFromType(ChannelType.TEXT))
                {
                    engine.put("guild", jdaEvent.getGuild());
                    engine.put("member", jdaEvent.getMember());
                }

                Object out = engine.eval(
                        "(function() {" +
                                "with (imports) {" +
                                jdaEvent.getMessage().getContentDisplay().substring(args[0].length()) +
                                "}" +
                                "})();");
                ebMessage.editMessage(out == null ? "Executed without error." : out.toString()).queue();
            }
            catch (Exception e)
            {
                ebMessage.editMessage(e.getMessage()).queue();
            }
        }).start();
    }
}
