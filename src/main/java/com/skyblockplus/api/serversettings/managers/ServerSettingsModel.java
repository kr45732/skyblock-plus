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

import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.mee6roles.Mee6Data;

import javax.persistence.*;

import com.skyblockplus.api.serversettings.skyblockevent.EventSettings;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
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
	private AutomatedApply automatedApplicationOne = null;

	@Embedded
	private AutomatedApply automatedApplicationTwo = null;

	@Embedded
	private AutomatedRoles automatedRoles = new AutomatedRoles();

	@Embedded
	private GuildRole automaticGuildRolesOne = null;

	@Embedded
	private GuildRole automaticGuildRolesTwo = null;

	@Embedded
	private EventSettings sbEvent = new EventSettings();

	private String hypixelApiKey = "";

	@Embedded
	private Mee6Data mee6Data = new Mee6Data();

	private String prefix = null;

	public ServerSettingsModel() {}

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
		copy.setAutomatedApplicationOne(automatedApplicationOne);
		copy.setAutomatedApplicationTwo(automatedApplicationTwo);
		copy.setAutomatedRoles(automatedRoles);
		copy.setAutomaticGuildRolesOne(automaticGuildRolesOne);
		copy.setAutomaticGuildRolesTwo(automaticGuildRolesTwo);
		copy.setSbEvent(sbEvent);
		copy.setHypixelApiKey(nullHypixelApiKey ? null : hypixelApiKey);

		return copy;
	}
}
