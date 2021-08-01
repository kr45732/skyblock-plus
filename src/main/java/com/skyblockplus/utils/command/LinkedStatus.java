package com.skyblockplus.utils.command;

public enum LinkedStatus {
	LINKED,
	NOT_LINKED,
	NO_MENTION;

	public String id;

	public LinkedStatus setId(String id) {
		this.id = id;
		return this;
	}
}
