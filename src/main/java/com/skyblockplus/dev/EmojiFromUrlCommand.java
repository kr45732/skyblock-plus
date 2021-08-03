package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.net.URL;
import javax.script.ScriptException;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Icon;

public class EmojiFromUrlCommand extends Command {

	public static final JsonObject addedObj = new JsonObject();

	public EmojiFromUrlCommand() {
		this.name = "em";
		this.ownerCommand = true;
	}

	public static JsonElement getSkyCryptItemsJson(String name) {
		String websiteContent = getUrl("https://sky.shiiyu.moe/stats/" + name)
			.split("const items = ")[1].split("const calculated =")[0].trim();
		websiteContent = websiteContent.substring(0, websiteContent.length() - 1);

		try {
			return JsonParser.parseString(es6ScriptEngine.eval("JSON.stringify(" + websiteContent + ")").toString());
		} catch (ScriptException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				if (!event.getGuild().getName().startsWith("Skyblock Plus - Emoji Server")) {
					return;
				}

				logCommand();

				try {
					JsonElement itemsJson = getSkyCryptItemsJson(args[1]);

					JsonArray all = higherDepth(itemsJson, "armor").getAsJsonArray();
					all.addAll(higherDepth(itemsJson, "armor").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "wardrobe").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "wardrobe_inventory").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "inventory").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "enderchest").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "talisman_bag").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "storage").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "talismans").getAsJsonArray());
					all.addAll(higherDepth(itemsJson, "personal_vault").getAsJsonArray());

					StringBuilder added = new StringBuilder();
					for (JsonElement i : all) {
						try {
							if (event.getGuild().getEmotes().size() >= event.getGuild().getMaxEmotes()) {
								System.out.println("Broke out");
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

						String id;
						String path;
						try {
							id = higherDepth(i, "tag.ExtraAttributes.id").getAsString();
							path = "https://sky.shiiyu.moe" + higherDepth(i, "texture_path").getAsString();
						} catch (Exception e) {
							continue;
						}

						try {
							if (!getEmojiMap().has(id) && !addedObj.has(id)) {
								System.out.println(id + " - " + path);

								Emote emote = event
									.getGuild()
									.createEmote(id.toLowerCase(), Icon.from(new URL(path).openStream()))
									.complete();
								added.append(emote.getAsMention()).append(" ");
								addedObj.addProperty(id, emote.getAsMention());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					event.reply(added.toString());
				} catch (Exception e) {
					event.reply("Error\n" + e.getMessage());
					e.printStackTrace();
				}
			}
		}
			.submit();
	}
}
