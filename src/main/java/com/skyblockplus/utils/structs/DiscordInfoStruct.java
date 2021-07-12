package com.skyblockplus.utils.structs;

public class DiscordInfoStruct {

	public String discordTag;
	public String minecraftUsername;
	public String minecraftUuid;
	public String failCause;

	public DiscordInfoStruct(String discordTag, String minecraftUsername, String minecraftUuid) {
		this.discordTag = discordTag;
		this.minecraftUsername = minecraftUsername;
		this.minecraftUuid = minecraftUuid;
	}

	public DiscordInfoStruct(String failCause) {
		this.failCause = failCause;
	}

	public DiscordInfoStruct() {
		this.failCause = "Player is not linked on Hypixel";
	}

	public boolean isNotValid() {
		return discordTag == null || minecraftUsername == null || minecraftUuid == null;
	}

	@Override
	public String toString() {
		return (
			"DiscordInfoStruct{" +
			"discordTag='" +
			discordTag +
			'\'' +
			", minecraftUsername='" +
			minecraftUsername +
			'\'' +
			", minecraftUuid='" +
			minecraftUuid +
			'\'' +
			'}'
		);
	}
}
