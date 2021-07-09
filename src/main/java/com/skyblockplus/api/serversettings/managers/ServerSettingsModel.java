package com.skyblockplus.api.serversettings.managers;

import com.skyblockplus.api.serversettings.automatedapply.AutomatedApply;
import com.skyblockplus.api.serversettings.automatedguild.GuildRole;
import com.skyblockplus.api.serversettings.automatedroles.AutomatedRoles;
import com.skyblockplus.api.serversettings.automatedverify.AutomatedVerify;
import com.skyblockplus.api.serversettings.mee6roles.Mee6Data;
import com.skyblockplus.api.serversettings.skyblockevent.SbEvent;
import javax.persistence.*;
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
	private SbEvent sbEvent = new SbEvent();

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
