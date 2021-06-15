package com.skyblockplus.utils.structs;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

public class HypixelGuildCache {

	public Instant lastUpdated;
	public List<String> membersCache = new ArrayList<>(); // Username=:=Slayer=:=Skills=:=Catacombs=:=Weight

	public HypixelGuildCache(Instant lastUpdated, List<String> membersCache) {
		this.lastUpdated = lastUpdated;
		this.membersCache = membersCache;
	}
	// [0] - username
	// [1] - slayer
	// [2] - skills
	// [3] - catacombs
	// [4] - weight
}
