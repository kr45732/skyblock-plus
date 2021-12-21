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

package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Constants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Data;

@Data
public class InvItem {

	private String name;
	private String lore;
	private int count = 1;
	private String modifier;
	private String creationOrigin;
	private String id;
	private String creationTimestamp;
	private List<String> enchantsFormatted = new ArrayList<>();
	private int hbpCount = 0;
	private int fumingCount = 0;
	private boolean recombobulated = false;
	private List<String> extraStats = new ArrayList<>();
	private List<InvItem> backpackItems = new ArrayList<>();
	private String rarity;
	private int dungeonFloor = 0;
	private String nbtTag;

	public void setHbpCount(int hbpCount) {
		if (hbpCount > 10) {
			this.fumingCount = hbpCount - 10;
			this.hbpCount = 10;
		} else {
			this.hbpCount = hbpCount;
		}
	}

	public void addExtraValue(String itemId) {
		extraStats.add(itemId);
	}

	public void addExtraValues(int count, String itemId) {
		if (count > 0) {
			extraStats.addAll(Collections.nCopies(count, itemId));
		}
	}

	public void setLore(String lore) {
		this.lore = lore;
		if (lore != null) {
			String[] loreArr = lore.split("\n");
			this.rarity = loreArr[loreArr.length - 1].trim().split(" ")[0];
		}
	}

	public void setBackpackItems(Collection<InvItem> backpackItems) {
		this.backpackItems.clear();
		this.backpackItems.addAll(backpackItems);
	}

	public String getPetApiName() {
		return (
			(getName() + "_" + getRarity()).toUpperCase().replace(" ", "_") +
			(
				getPetItem() != null &&
					(
						getPetItem().equals("PET_ITEM_TIER_BOOST") ||
						getPetItem().equals("PET_ITEM_VAMPIRE_FANG") ||
						getPetItem().equals("PET_ITEM_TOY_JERRY")
					)
					? "_TB"
					: ""
			)
		);
	}

	public String getPetItem() {
		if (id.equals("PET")) {
			for (String extraItem : getExtraStats()) {
				if (PET_ITEM_NAMES.contains(extraItem)) {
					return extraItem;
				}
			}
		}
		return null;
	}

	public String getPetRarity() {
		if (id.equals("PET")) {
			if (
				extraStats.contains("PET_ITEM_TIER_BOOST") ||
				extraStats.contains("PET_ITEM_VAMPIRE_FANG") ||
				extraStats.contains("PET_ITEM_TOY_JERRY")
			) {
				return NUMBER_TO_RARITY_MAP.get("" + (Integer.parseInt(RARITY_TO_NUMBER_MAP.get(rarity).replace(";", "")) + 1));
			}
		}
		return rarity;
	}
}
