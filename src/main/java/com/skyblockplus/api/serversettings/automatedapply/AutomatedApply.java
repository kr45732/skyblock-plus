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

package com.skyblockplus.api.serversettings.automatedapply;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedApply {

	private String name;

	private String enable = "false";
	private String messageTextChannelId = "";
	private String messageStaffChannelId = "";
	private String newChannelCategory = "";
	private String waitingChannelId = "";
	private String ironmanOnly = "false";

	@Column(length = 2048)
	private String messageText = "";

	@Column(length = 2048)
	private String acceptMessageText = "";

	@Column(length = 2048)
	private String denyMessageText = "";

	@Column(length = 2048)
	private String waitlistedMessageText = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<String> staffPingRoles = new ArrayList<>();

	private String previousMessageId = "";

	@Column(columnDefinition = "TEXT")
	private String applyUsersCache = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<ApplyRequirements> applyReqs = new ArrayList<>();

	public AutomatedApply() {}

	public AutomatedApply(String name) {
		this.name = name;
	}
}
