/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience create Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms create the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 create the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty create
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy create the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.api.serversettings.automatedguild;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.skyblockplus.api.serversettings.automatedroles.RoleObject;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class AutomatedGuild {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "server_settings_id")
	@JsonIgnore
	@Expose(serialize = false, deserialize = false)
	@ToString.Exclude
	private ServerSettingsModel serverSettings;

	private String guildName;
	private String guildId;

	// Apply
	private String applyEnable = "false";
	private String applyMessageChannel = "";
	private String applyStaffChannel = "";
	private String applyCategory = "";
	private String applyWaitingChannel = "";
	private String applyGamemode = "";
	private String applyScammerCheck = "false";
	private String applyCheckApi = "false";

	@Column(length = 2048)
	private String applyMessage = "";

	@Column(length = 2048)
	private String applyAcceptMessage = "";

	@Column(length = 2048)
	private String applyDenyMessage = "";

	@Column(length = 2048)
	private String applyWaitlistMessage = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<String> applyStaffRoles = new ArrayList<>();

	private String applyPrevMessage = "";

	@Column(columnDefinition = "TEXT")
	private String applyUsersCache = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<ApplyRequirements> applyReqs = new ArrayList<>();

	// Guild Member Roles
	private String guildMemberRoleEnable = "false";
	private String guildMemberRole = "";

	// Guild Rank
	private String guildRanksEnable = "false";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<RoleObject> guildRanks = new ArrayList<>();

	private String guildCounterEnable = "false";
	private String guildCounterChannel = "";

	public AutomatedGuild(String guildName, String guildId) {
		this.guildName = guildName;
		this.guildId = guildId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
			return false;
		}
		return id != null && Objects.equals(id, ((AutomatedGuild) o).id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
