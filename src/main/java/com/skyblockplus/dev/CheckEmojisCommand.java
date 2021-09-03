package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.getEmojiMap;
import static com.skyblockplus.utils.Utils.makeHastePost;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.command.CommandExecute;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;

public class CheckEmojisCommand extends Command {

	public CheckEmojisCommand() {
		this.name = "d-check";
		this.ownerCommand = true;
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event, false) {
			@Override
			protected void execute() {
				logCommand();

				try {
					File neuDir = new File("src/main/java/com/skyblockplus/json/neu_emoji");
					if (neuDir.exists()) {
						FileUtils.deleteDirectory(neuDir);
					}
					neuDir.mkdir();

					Git neuRepo = Git
						.cloneRepository()
						.setURI("https://github.com/Moulberry/NotEnoughUpdates-REPO.git")
						.setDirectory(neuDir)
						.call();

					File dir = new File("src/main/java/com/skyblockplus/json/neu_emoji/items");
					List<String> validIds = Arrays
						.stream(dir.listFiles())
						.map(file -> file.getName().replace(".json", ""))
						.collect(Collectors.toList());
					System.out.println(makeHastePost(new Gson().toJson(validIds)));
					FileUtils.deleteDirectory(neuDir);

					JsonArray invalidEmojis = new JsonArray();
					JsonObject emojis = getEmojiMap();
					for (String emojiEntry : emojis.keySet()) {
						if (!validIds.contains(emojiEntry)) {
							invalidEmojis.add(emojiEntry);
						}
					}

					event.reply(makeHastePost(invalidEmojis.toString()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
			.submit();
	}
}
