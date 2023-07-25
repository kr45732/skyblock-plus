/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import static com.skyblockplus.utils.Constants.*;
import static com.skyblockplus.utils.utils.StringUtils.cleanMcCodes;
import static com.skyblockplus.utils.utils.StringUtils.idToName;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import me.nullicorn.nedit.type.NBTCompound;
import me.nullicorn.nedit.type.NBTList;

@Data
public class InvItem {

	private String name;
	private String id;
	private int count = 1;
	private int hpbCount = 0;
	private int fumingCount = 0;
	private boolean recombobulated = false;
	private String modifier;
	private String rarity;
	private String skin;
	private List<String> lore;
	private String creationTimestamp;
	private List<String> enchantsFormatted = new ArrayList<>();
	private Map<String, Integer> attributes = new TreeMap<>();
	private List<String> runesFormatted = new ArrayList<>();
	private Map<String, Integer> extraStats = new HashMap<>();
	private List<InvItem> backpackItems = new ArrayList<>();
	private int essenceCount;
	private String essenceType;
	private long darkAuctionPrice = -1;
	private long shensAuctionPrice = -1;
	private int dungeonFloor = -1;
	private boolean soulbound = false;
	private NBTCompound nbtTag;

	public void addEssence(int essenceCount, String essenceType) {
		this.essenceCount += essenceCount;
		this.essenceType = essenceType;
	}

	/**
	 * Will only set it for midas sword or midas staff
	 */
	public void setDarkAuctionPrice(long darkAuctionPrice) {
		if (id != null && (id.equals("MIDAS_SWORD") || id.equals("MIDAS_STAFF"))) {
			this.darkAuctionPrice = darkAuctionPrice;
		}
	}

	public void setModifier(String modifier) {
		// Fix Hypixel still showing hyper as warped reforge
		if (getName().startsWith("Hyper ") && modifier.equals("warped")) {
			this.modifier = "hyper";
		} else {
			this.modifier = modifier;
		}
	}

	public void setShensAuctionPrice(long shensAuctionPrice) {
		this.shensAuctionPrice = shensAuctionPrice;
	}

	/**
	 * @return item name with color codes stripped off
	 */
	public String getName() {
		return getName(true);
	}

	public String getName(boolean parseMcCodes) {
		return parseMcCodes ? cleanMcCodes(name) : name;
	}

	public String getFormattedId() {
		if (id.equals("PET") && getName().contains("] ")) {
			return getName().split("] ")[1].toUpperCase().replace(" ", "_").replace("_✦", "") + RARITY_TO_NUMBER_MAP.get(getPetRarity());
		} else if (id.equals("ENCHANTED_BOOK")) {
			return enchantsFormatted.isEmpty() ? id : enchantsFormatted.get(0).toUpperCase();
		} else if (id.equals("RUNE")) {
			return runesFormatted.isEmpty() ? id : runesFormatted.get(0).toUpperCase();
		} else {
			return id;
		}
	}

	public String getNameFormatted() {
		return id.equals("ENCHANTED_BOOK") && !enchantsFormatted.isEmpty() ? idToName(enchantsFormatted.get(0)) : getName();
	}

	public void setHpbCount(int hpbCount) {
		if (hpbCount > 10) {
			this.fumingCount = hpbCount - 10;
			this.hpbCount = 10;
		} else {
			this.hpbCount = hpbCount;
		}
	}

	public void addExtraValue(String itemId) {
		addExtraValues(1, itemId);
	}

	public void addExtraValues(int count, String itemId) {
		if (count > 0 && itemId != null) {
			extraStats.compute(itemId, (k, v) -> (v == null ? 0 : v) + count);
		}
	}

	public void setLore(NBTList lore) {
		if (lore != null) {
			this.lore = lore.stream().map(line -> (String) line).collect(Collectors.toCollection(ArrayList::new));
			rarity = cleanMcCodes(this.lore.get(this.lore.size() - 1)).trim().split("\\s+")[0];
			rarity += rarity.startsWith("VERY") ? "_SPECIAL" : "";
			soulbound = soulbound || this.lore.contains("§8§l* §8Co-op Soulbound §8§l*") || this.lore.contains("§8§l* §8Soulbound §8§l*");
		}
	}

	public void setMuseum(boolean isMuseum) {
		soulbound = soulbound || isMuseum;
	}

	public void setBackpackItems(Collection<InvItem> backpackItems) {
		this.backpackItems.clear();
		this.backpackItems.addAll(backpackItems);
	}

	public String getPetApiName() {
		return ((getName() + "_" + rarity).toUpperCase().replace(" ", "_") + (isTierBoosted() ? "_TB" : ""));
	}

	public boolean isTierBoosted() {
		return id.equals("PET") && Objects.equals(getPetItem(), "PET_ITEM_TIER_BOOST");
	}

	public String getPetItem() {
		if (id.equals("PET")) {
			for (String extraItem : getExtraStats().keySet()) {
				if (PET_ITEM_NAMES.contains(extraItem)) {
					return extraItem;
				}
			}
		}
		return null;
	}

	/**
	 * Converts tier boosted pets to the post-boosted rarity
	 */
	public String getPetRarity() {
		if (id.equals("PET") && isTierBoosted()) {
			return NUMBER_TO_RARITY_MAP.get("" + (Integer.parseInt(RARITY_TO_NUMBER_MAP.get(rarity).replace(";", "")) + 1));
		}
		return rarity;
	}

	public void setSkin(String skin) {
		this.skin = skin;
		addExtraValue(skin);
	}
}
