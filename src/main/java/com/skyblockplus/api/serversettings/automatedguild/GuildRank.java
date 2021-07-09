package com.skyblockplus.api.serversettings.automatedguild;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class GuildRank {

	public String minecraftRoleName;
	public String discordRoleId;

	public GuildRank() {}
}
