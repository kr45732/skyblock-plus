package com.skyblockplus.utils.structs;

public class UsernameUuidStruct {

	public String playerUsername;
	public String playerUuid;
	public String failCause;

	public UsernameUuidStruct(String playerUsername, String playerUuid) {
		this.playerUsername = playerUsername;
		this.playerUuid = playerUuid;
	}

	public UsernameUuidStruct(String failCause) {
		this.failCause = failCause;
	}

	public UsernameUuidStruct() {
		this.failCause = "Unknown fail cause";
	}

	public boolean isNotValid() {
		return playerUsername == null || playerUuid == null;
	}

	@Override
	public String toString() {
		return "UsernameUuidStruct{" + "username='" + playerUsername + '\'' + ", uuid='" + playerUuid + '\'' + '}';
	}
}
