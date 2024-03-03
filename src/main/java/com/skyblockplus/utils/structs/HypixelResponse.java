/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2024 kr45732
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

package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.utils.JsonUtils.higherDepth;

import com.google.gson.JsonElement;
import com.skyblockplus.utils.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;

public record HypixelResponse(JsonElement response, String failCause) {
	public HypixelResponse(JsonElement response) {
		this(response, null);
	}

	public HypixelResponse(String failCase) {
		this(null, failCase);
	}

	public boolean isValid() {
		return response != null;
	}

	public EmbedBuilder getErrorEmbed() {
		return Utils.errorEmbed(failCause);
	}

	public JsonElement get(String path) {
		return higherDepth(response, path);
	}
}
