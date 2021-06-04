package com.skyblockplus.utils.structs;

public class UsernameUuidStruct {

	public final String playerUsername;
	public final String playerUuid;

	public UsernameUuidStruct(String playerUsername, String playerUuid) {
		this.playerUsername = playerUsername;
		this.playerUuid = playerUuid;
	}

	@Override
	public String toString() {
		return "UsernameUuidStruct{" +
				"playerUsername='" + playerUsername + '\'' +
				", playerUuid='" + playerUuid + '\'' +
				'}';
	}
}
