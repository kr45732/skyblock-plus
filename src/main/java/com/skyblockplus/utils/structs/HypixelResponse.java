package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Utils.higherDepth;

import com.google.gson.JsonElement;

public class HypixelResponse {

	public JsonElement response;
	public String failCause;

	public HypixelResponse(JsonElement response) {
		this.response = response;
	}

	public HypixelResponse(String failCase) {
		this.failCause = failCase;
	}

	public HypixelResponse() {
		this.failCause = "Unknown Fail Cause";
	}

	public boolean isNotValid() {
		return response == null;
	}

	public JsonElement get(String path) {
		if (isNotValid()) {
			return null;
		}
		return higherDepth(response, path);
	}
}
