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

package com.skyblockplus.api.serversettings.skyblockevent;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@Transactional
public class EventSettings {

	private String eventType = "";
	private String announcementId = "";
	private String timeEndingSeconds = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private Map<Integer, String> prizeMap = new HashMap<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<EventMember> membersList = new ArrayList<>();

	private String eventGuildId = "";

	private String minAmount = "-1";
	private String maxAmount = "-1";

	private String whitelistRole = "";

	private String announcementMessageId = "";

	public boolean isMinMaxValid() {
		int minValue = -1;
		int maxValue = -1;
		try {
			minValue = Integer.parseInt(minAmount);
		} catch (Exception ignored) {}
		try {
			maxValue = Integer.parseInt(maxAmount);
		} catch (Exception ignored) {}
		if (minValue == -1 || maxValue == -1) {
			return true;
		}
		return minValue <= maxValue;
	}
}
