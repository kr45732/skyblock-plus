/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.dev;

import static com.skyblockplus.utils.Utils.*;

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
					System.out.println(makeHastePost(gson.toJson(validIds)));
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
