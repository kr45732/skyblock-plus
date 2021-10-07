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

package com.skyblockplus.api.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import club.minnced.discord.webhook.WebhookClientBuilder;
import com.google.gson.JsonElement;
import java.awt.*;
import net.dv8tion.jda.api.EmbedBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/public")
public class CommandEndpoints {

	private static final Logger log = LoggerFactory.getLogger(CommandEndpoints.class);

	/*
	@GetMapping("/essence/information")
	public ResponseEntity<?> getEssenceInformation(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			log.info("/api/public/essence/information?name=" + name);

			JsonElement essenceJson = getEssenceCostsJson();
			if (essenceJson == null) {
				log.error("Null essenceJson");
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = name;
			preFormattedItem = nameToId(preFormattedItem);

			if (higherDepth(essenceJson, preFormattedItem) == null) {
				String closestMatch = getClosestMatch(preFormattedItem, essenceItemNames);
				preFormattedItem = closestMatch != null ? closestMatch : preFormattedItem;
			}

			JsonElement itemJson = higherDepth(essenceJson, preFormattedItem);
			if (itemJson != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", idToName(preFormattedItem));
				for (String level : getJsonKeys(itemJson)) {
					template.addData(level, higherDepth(itemJson, level));
				}

				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/bin")
	public ResponseEntity<?> getLowestBin(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			log.info("/api/public/bin?name=" + name);

			JsonElement lowestBinJson = getLowestBinJson();
			if (lowestBinJson == null) {
				log.error("Null lowestBinJson");
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = nameToId(name);

			if (higherDepth(lowestBinJson, preFormattedItem) != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", idToName(preFormattedItem));
				template.addData("price", higherDepth(lowestBinJson, preFormattedItem).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			String formattedName;
			for (String i : enchantNames) {
				if (preFormattedItem.contains(i)) {
					try {
						int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
						String enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
						formattedName = i + ";" + enchantLevel;
						Template template = new Template(true);
						template.addData("id", enchantName);
						template.addData("name", idToName(enchantName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			JsonElement petJson = getPetNumsJson();
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : Constants.rarityToNumberMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(lowestBinJson, formattedName + Constants.rarityToNumberMap.get(j)) != null) {
								formattedName += Constants.rarityToNumberMap.get(j);
								break;
							}
						}
					}

					try {
						Template template = new Template(true);
						template.addData("id", formattedName);
						template.addData("name", idToName(formattedName));
						template.addData("price", higherDepth(lowestBinJson, formattedName).getAsLong());
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(lowestBinJson));

			if (closestMatch != null && higherDepth(lowestBinJson, closestMatch) != null) {
				Template template = new Template(true);
				template.addData("id", closestMatch);
				template.addData("name", idToName(closestMatch));
				template.addData("price", higherDepth(lowestBinJson, closestMatch).getAsLong());
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/average")
	public ResponseEntity<?> getAverageAuction(@RequestParam(value = "key") String key, @RequestParam(value = "name") String name) {
		try {
			log.info("/api/public/average?name=" + name);

			JsonElement avgAhJson = getAverageAuctionJson();
			if (avgAhJson == null) {
				log.error("Null averageAuctionJson");
				return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
			}

			String preFormattedItem = nameToId(name);

			if (higherDepth(avgAhJson, preFormattedItem) != null) {
				Template template = new Template(true);
				template.addData("id", preFormattedItem);
				template.addData("name", idToName(preFormattedItem));
				template.addData("price", getAvgPrice(higherDepth(avgAhJson, preFormattedItem)));
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			String formattedName;
			for (String i : enchantNames) {
				if (preFormattedItem.contains(i)) {
					try {
						int enchantLevel = Integer.parseInt(preFormattedItem.replaceAll("\\D+", ""));
						String enchantName = i.toLowerCase().replace("_", " ") + " " + enchantLevel;
						formattedName = i + ";" + enchantLevel;
						Template template = new Template(true);
						template.addData("id", enchantName);
						template.addData("name", idToName(enchantName));
						template.addData("price", getAvgPrice(higherDepth(avgAhJson, formattedName)));
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			JsonElement petJson = getPetNumsJson();
			for (String i : petNames) {
				if (preFormattedItem.contains(i)) {
					formattedName = i;
					boolean raritySpecified = false;
					for (Map.Entry<String, String> j : rarityToNumberMap.entrySet()) {
						if (preFormattedItem.contains(j.getKey())) {
							formattedName += j.getValue();
							raritySpecified = true;
							break;
						}
					}

					if (!raritySpecified) {
						List<String> petRarities = getJsonKeys(higherDepth(petJson, formattedName));
						for (String j : petRarities) {
							if (higherDepth(avgAhJson, formattedName + rarityToNumberMap.get(j)) != null) {
								formattedName += rarityToNumberMap.get(j);
								break;
							}
						}
					}

					try {
						Template template = new Template(true);
						template.addData("id", formattedName);
						template.addData("name", idToName(formattedName));
						template.addData("price", getAvgPrice(higherDepth(avgAhJson, formattedName)));
						return new ResponseEntity<>(template, HttpStatus.OK);
					} catch (Exception ignored) {}
				}
			}

			String closestMatch = getClosestMatch(preFormattedItem, getJsonKeys(avgAhJson));

			if (closestMatch != null && higherDepth(avgAhJson, closestMatch) != null) {
				Template template = new Template(true);
				template.addData("id", closestMatch);
				template.addData("name", idToName(closestMatch));
				template.addData("price", getAvgPrice(higherDepth(avgAhJson, closestMatch)));
				return new ResponseEntity<>(template, HttpStatus.OK);
			}

			return new ResponseEntity<>(new ErrorTemplate(false, "Invalid Name"), HttpStatus.OK);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return new ResponseEntity<>(new ErrorTemplate(false, "Internal Server Error"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private long getAvgPrice(JsonElement itemJson) {
		return higherDepth(itemJson, "clean_price") != null
			? higherDepth(itemJson, "clean_price").getAsLong()
			: higherDepth(itemJson, "price").getAsLong();
	}
*/

	@PostMapping("/heroku")
	public ResponseEntity<?> herokuToDiscordWebhook(
		@RequestBody Object body,
		@RequestHeader(value = "Heroku-Webhook-Hmac-SHA256") String herokuHMAC
	) {
		log.info("/api/public/heroku");

		JsonElement jsonBody = gson.toJsonTree(body);
		System.out.println("Heroku-Webhook-Hmac-SHA256: " + herokuHMAC);
		System.out.println(jsonBody);

		String appName = higherDepth(jsonBody, "data.app.name", "Null");
		String actorEmail = higherDepth(jsonBody, "actor.email", "Null");
		actorEmail = actorEmail.contains("@gmail.com") ? "kr45732" : actorEmail;
		String description = "";

		EmbedBuilder eb = defaultEmbed(appName, "https://dashboard.heroku.com/apps/" + appName).setAuthor(actorEmail).setColor(Color.GREEN);

		try {
			description += "**Version:** " + higherDepth(jsonBody, "data.release.version").getAsInt() + "\n";
		} catch (Exception ignored) {}

		try {
			description += "**Description:** " + higherDepth(jsonBody, "data.description").getAsString() + "\n";
		} catch (Exception ignored) {}

		try {
			description += "**Output streamJsonArray:** [link](" + higherDepth(jsonBody, "data.output_stream_url").getAsString() + ")\n";
		} catch (Exception ignored) {}

		try {
			description += "**Resource:** " + higherDepth(jsonBody, "resource").getAsString() + "\n";
		} catch (Exception ignored) {}

		try {
			description += "**Action:** " + higherDepth(jsonBody, "action").getAsString() + "\n";
		} catch (Exception ignored) {}

		try {
			description += "**Status:** " + higherDepth(jsonBody, "data.status").getAsString() + "\n";
		} catch (Exception ignored) {}

		if (webhookClient == null) {
			webhookClient =
				new WebhookClientBuilder(
					"https://discord.com/api/webhooks/870080904758952037/mI2Xoa5av_Y3CIKofCua-K_zDrMJX1O3KjXG65sgRTW52eMXbY3geN8fQVLKd2DH75Xf"
				)
					.setExecutorService(scheduler)
					.setHttpClient(okHttpClient)
					.buildJDA();
		}

		webhookClient.send(eb.setDescription(description).build());

		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}
