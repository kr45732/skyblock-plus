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
			description += "**Output stream:** [link](" + higherDepth(jsonBody, "data.output_stream_url").getAsString() + ")\n";
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
