package com.skyblockplus.api.templates;

public class ErrorTemplate {

	private final boolean success;
	private final String cause;

	public ErrorTemplate(boolean success, String cause) {
		this.success = success;
		this.cause = cause;
	}

	public boolean getSuccess() {
		return success;
	}

	public String getCause() {
		return cause;
	}
}
