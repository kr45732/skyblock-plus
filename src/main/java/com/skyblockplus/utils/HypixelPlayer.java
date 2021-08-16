package com.skyblockplus.utils;

import static com.skyblockplus.utils.Hypixel.playerFromUuid;
import static com.skyblockplus.utils.Utils.defaultEmbed;
import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyblockplus.utils.structs.HypixelResponse;
import com.skyblockplus.utils.structs.UsernameUuidStruct;
import java.time.Instant;
import net.dv8tion.jda.api.EmbedBuilder;

public class HypixelPlayer {

	private JsonObject playerJson;
	private String playerUuid;
	private String playerUsername;
	private boolean validPlayer = false;
	private String failCause = "Unknown fail cause";

	/* Constructor */
	public HypixelPlayer(String username) {
		if (usernameToUuid(username)) {
			return;
		}

		try {
			HypixelResponse response = playerFromUuid(playerUuid);
			if (response.isNotValid()) {
				failCause = response.failCause;
				return;
			}

			this.playerJson = response.response.getAsJsonObject();
		} catch (Exception e) {
			return;
		}

		this.validPlayer = true;
	}

	/* Getters */
	public boolean isValid() {
		return validPlayer;
	}

	public String getFailCause() {
		return failCause;
	}

	public String getUsername() {
		return playerUsername;
	}

	public String getUuid() {
		return playerUuid;
	}

	public EmbedBuilder getDefaultEmbed() {
		return defaultEmbed(playerUsername, "https://plancke.io/hypixel/player/stats/" + playerUuid)
			.setThumbnail("https://cravatar.eu/helmavatar/" + playerUuid + "/64.png");
	}

	/* Hypixel */
	public double getHypixelLevel() {
		return (Math.sqrt((2 * higherDepth(playerJson, "networkExp", 0L)) + 30625) / 50) - 2.5;
	}

	public boolean isOnline() {
		return higherDepth(playerJson, "lastLogin", 0) > higherDepth(playerJson, "lastLogout", 0);
	}

	public JsonObject getSocialMediaLinks() {
		return higherDepth(playerJson, "socialMedia.links") != null
			? higherDepth(playerJson, "socialMedia.links").getAsJsonObject()
			: new JsonObject();
	}

	public int getAchievementPoints() {
		return higherDepth(playerJson, "achievementPoints", 0);
	}

	public int getKarma() {
		return higherDepth(playerJson, "karma", 0);
	}

	public Instant getLastLogin() {
		if (higherDepth(playerJson, "lastLogout") == null) {
			return null;
		}
		return Instant.ofEpochMilli(higherDepth(playerJson, "lastLogout").getAsLong());
	}

	public Instant getFirstLogin() {
		if (higherDepth(playerJson, "firstLogin") == null) {
			return null;
		}
		return Instant.ofEpochMilli(higherDepth(playerJson, "firstLogin").getAsLong());
	}

	public JsonElement get(String path) {
		return higherDepth(playerJson, path);
	}

	/* Helper methods */
	private boolean usernameToUuid(String username) {
		UsernameUuidStruct response = Hypixel.usernameToUuid(username);
		if (response.isNotValid()) {
			failCause = response.failCause;
			return true;
		}

		this.playerUsername = response.playerUsername;
		this.playerUuid = response.playerUuid;
		return false;
	}

	public String getRank() {
		return "";
	}
}
