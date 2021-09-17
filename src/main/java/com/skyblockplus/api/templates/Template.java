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

package com.skyblockplus.api.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.Map;

public class Template {

	private final boolean success;
	private final LinkedHashMap<String, Object> data = new LinkedHashMap<>();

	public Template(boolean success) {
		this.success = success;
	}

	public void addData(String key, JsonElement value) {
		if (value.isJsonPrimitive()) {
			JsonPrimitive val = value.getAsJsonPrimitive();
			if (val.isString()) {
				data.put(key, val.getAsString());
				return;
			} else if (val.isBoolean()) {
				data.put(key, val.getAsBoolean());
				return;
			} else if (val.isNumber()) {
				data.put(key, val.getAsNumber());
				return;
			}
		}

		data.put(key, value.toString());
	}

	public void addData(String key, Object value) {
		data.put(key, value);
	}

	public boolean getSuccess() {
		return success;
	}

	public Map<String, Object> getData() {
		return data;
	}
}
