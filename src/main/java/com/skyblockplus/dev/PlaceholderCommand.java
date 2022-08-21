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

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.skyblockplus.utils.Constants;
import com.skyblockplus.utils.Utils;
import com.skyblockplus.utils.command.CommandExecute;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.xml.bind.SchemaOutputResolver;
import org.springframework.http.converter.json.GsonBuilderUtils;
import org.springframework.stereotype.Component;

@Component
public class PlaceholderCommand extends Command {

	public PlaceholderCommand() {
		this.name = "d-placeholder";
		this.ownerCommand = true;
		this.aliases = new String[] { "ph" };
		this.botPermissions = defaultPerms();
	}

	@Override
	protected void execute(CommandEvent event) {
		new CommandExecute(this, event) {
			@Override
			protected void execute() {
				logCommand();

				String total = roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
				String free = roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
				String used = roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) + " MB";
				if (args.length >= 2 && args[1].equals("gc")) {
					System.gc();
					total += " ➜ " + roundAndFormat(Runtime.getRuntime().totalMemory() / 1000000.0) + " MB";
					free += " ➜ " + roundAndFormat(Runtime.getRuntime().freeMemory() / 1000000.0) + " MB";
					used +=
						" ➜ " +
						roundAndFormat((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0) +
						" MB";
				}

				embed(
					defaultEmbed("Debug")
						.addField("Total", total, false)
						.addField("Free", free, false)
						.addField("Used", used, false)
						.addField("Max", "" + roundAndFormat(Runtime.getRuntime().maxMemory() / 1000000.0) + " MB", false)
				);
			}
		}
			.queue();
	}
	/*
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Utils.initialize();
		Constants.initialize();
		for (int i = 0; i < 5; i++) {
			asyncGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=28667672039044989b0019b14a2c34d6")
					.thenApplyAsync(r -> {
						Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy() {
							@Override
							public boolean shouldSkipField(FieldAttributes f) {
								System.out.println(f.getName());
								return false;
							}

							@Override
							public boolean shouldSkipClass(Class<?> clazz) {
								return false;
							}
						}).create();
						long m = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
						long s = System.currentTimeMillis();
						try (JsonReader reader = new JsonReader(new InputStreamReader(r.body()))) {
							reader.beginObject();
							while (reader.hasNext()) {
								if (reader.peek() == JsonToken.NAME && reader.nextName().equals("profiles")) {
									reader.beginArray();
									while (reader.hasNext()) {
										JsonElement j = gson.fromJson(reader, new TypeToken<JsonElement>() {}.getType());
									}
									reader.endArray();
								} else {
									reader.skipValue();
								}
							}
							reader.endObject();
							System.out.println(System.currentTimeMillis() - s);
							System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - m);
						} catch (Exception e) {
							e.printStackTrace();
						}
						return null;
					}, executor).get();
//			asyncGet("https://api.hypixel.net/skyblock/profiles?key=" + HYPIXEL_API_KEY + "&uuid=28667672039044989b0019b14a2c34d6")
//					.thenApplyAsync(r -> {
//			Gson gson = new GsonBuilder().addDeserializationExclusionStrategy(new ExclusionStrategy() {
//				@Override
//				public boolean shouldSkipField(FieldAttributes f) {
//					System.out.println(f.getName());
//					return false;
//				}
//
//				@Override
//				public boolean shouldSkipClass(Class<?> clazz) {
//					return false;
//				}
//			}).create();
//						long m = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//						try {
//							long s = System.currentTimeMillis();
//							JsonElement data = JsonParser.parseReader(new InputStreamReader(r.body()));
////							System.out.println(System.currentTimeMillis() - s);
//							System.out.println((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - m);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						return null;
//					}, executor).get();
		}
	}
	 */
}
