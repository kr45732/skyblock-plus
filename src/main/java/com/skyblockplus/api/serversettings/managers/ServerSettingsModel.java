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

package com.skyblockplus.api.serversettings.managers;

import com.skyblockplus.api.serversettings.automatedguild.ApplyBlacklist;
import com.skyblockplus.api.serversettings.automatedguild.AutomatedGuild;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.jacob.JacobSettings;
import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Transactional
public class ServerSettingsModel {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	private String serverName;
	private String serverId;

	@Embedded
	private AutomatedVerify automatedVerify = new AutomatedVerify();

	@Embedded
	private AutomatedGuild automatedGuildOne = null;

	@Embedded
	private AutomatedGuild automatedGuildTwo = null;

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<ApplyBlacklist> applicationBlacklist = new ArrayList<>();

	@Embedded
	private AutomatedRoles automatedRoles = new AutomatedRoles();

	@Embedded
	private EventSettings sbEvent = new EventSettings();

	@Embedded
	private JacobSettings jacobSettings = new JacobSettings();

	private String hypixelApiKey = "";

	private String prefix = null;

	private String applyGuestRole = "";

	private String fetchurChannel = "";
	private String fetchurRole = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<String> botManagerRoles = new ArrayList<>();

	public ServerSettingsModel(String serverName, String serverId) {
		this.serverName = serverName;
		this.serverId = serverId;
	}

	public String getHypixelApiKey() {
		return null;
	}

	public String getHypixelApiKeyInt() {
		return hypixelApiKey;
	}

	public ServerSettingsModel copy(boolean nullHypixelApiKey) {
		ServerSettingsModel copy = new ServerSettingsModel(serverName, serverId);
		copy.setAutomatedVerify(automatedVerify);
		copy.setAutomatedGuildOne(automatedGuildOne);
		copy.setAutomatedGuildTwo(automatedGuildTwo);
		copy.setApplicationBlacklist(applicationBlacklist);
		copy.setAutomatedRoles(automatedRoles);
		copy.setSbEvent(sbEvent);
		copy.setJacobSettings(jacobSettings);
		copy.setHypixelApiKey(nullHypixelApiKey ? null : hypixelApiKey);
		copy.setPrefix(prefix);
		copy.setApplyGuestRole(applyGuestRole);
		copy.setFetchurChannel(fetchurChannel);
		copy.setFetchurRole(fetchurRole);
		copy.setBotManagerRoles(botManagerRoles);
		return copy;
	}
}
