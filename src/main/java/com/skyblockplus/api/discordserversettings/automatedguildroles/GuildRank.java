package com.skyblockplus.api.discordserversettings.automatedguildroles;

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
