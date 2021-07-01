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
