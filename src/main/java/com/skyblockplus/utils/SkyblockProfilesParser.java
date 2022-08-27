/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2022 kr45732
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

package com.skyblockplus.utils;

import com.google.gson.*;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class SkyblockProfilesParser {

	private static final List<String> IGNORED_SB_PROFILE = List.of(
		"community_upgrades",
		"objectives",
		"tutorial",
		"quests",
		"visited_zones",
		"experimentation",
		"unlocked_coll_tiers",
		"backpack_icons",
		"slayer_quest",
		"achievement_spawned_island_types",
		"active_effects",
		"paused_effects",
		"disabled_potion_effects",
		"visited_modes",
		"fishing_bag",
		"potion_bag",
		"candy_inventory_contents",
		"quiver",
		"autopet",
		// Slayer
		"claimed_levels",
		// Dungeons
		"dungeon_journal",
		"best_runs",
		"dungeons_blah_blah",
		"daily_runs"
	);
	private static final List<String> IGNORED_SB_PROFILE_LB = List.of("bestiary");

	/**
	 * Tries to begin reading a JSON array or JSON object, returning {@code null} if
	 * the next element is neither of those.
	 */
	private static JsonElement tryBeginNesting(JsonReader in, JsonToken peeked) throws IOException {
		switch (peeked) {
			case BEGIN_ARRAY:
				in.beginArray();
				return new JsonArray();
			case BEGIN_OBJECT:
				in.beginObject();
				return new JsonObject();
			default:
				return null;
		}
	}

	/**
	 * Reads a {@link JsonElement} which cannot have any nested elements
	 */
	private static JsonElement readTerminal(JsonReader in, JsonToken peeked) throws IOException {
		switch (peeked) {
			case STRING:
				return new JsonPrimitive(in.nextString());
			case NUMBER:
				String number = in.nextString();
				return new JsonPrimitive(new LazilyParsedNumber(number));
			case BOOLEAN:
				return new JsonPrimitive(in.nextBoolean());
			case NULL:
				in.nextNull();
				return JsonNull.INSTANCE;
			default:
				// When read(JsonReader) is called with JsonReader in invalid state
				throw new IllegalStateException("Unexpected token: " + peeked);
		}
	}

	public static JsonElement parse(JsonReader in) {
		return parse(in, null);
	}

	public static JsonElement parse(JsonReader in, String uuid) {
		try {
			// Either JsonArray or JsonObject
			JsonElement current;
			JsonToken peeked = in.peek();

			current = tryBeginNesting(in, peeked);
			if (current == null) {
				return readTerminal(in, peeked);
			}

			Deque<JsonElement> stack = new ArrayDeque<>();

			while (true) {
				while (in.hasNext()) {
					String name = null;
					// Name is only used for JSON object members
					if (current instanceof JsonObject) {
						name = in.nextName();
					}

					if (
						name != null &&
						(
							IGNORED_SB_PROFILE.contains(name) ||
							(
								uuid != null &&
								(IGNORED_SB_PROFILE_LB.contains(name) || (!name.equals(uuid) && in.getPath().endsWith("].members." + name)))
							)
						)
					) {
						in.skipValue();
						continue;
					}

					peeked = in.peek();
					JsonElement value = tryBeginNesting(in, peeked);
					boolean isNesting = value != null;

					if (value == null) {
						value = readTerminal(in, peeked);
					}

					if (current instanceof JsonArray) {
						((JsonArray) current).add(value);
					} else {
						((JsonObject) current).add(name, value);
					}

					if (isNesting) {
						stack.addLast(current);
						current = value;
					}
				}

				// End current element
				if (current instanceof JsonArray) {
					in.endArray();
				} else {
					in.endObject();
				}

				if (stack.isEmpty()) {
					return current;
				} else {
					// Continue with enclosing element
					current = stack.removeLast();
				}
			}
		} catch (Exception e) {
			return null;
		}
	}
}
