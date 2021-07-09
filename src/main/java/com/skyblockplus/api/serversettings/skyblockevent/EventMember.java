package com.skyblockplus.api.serversettings.skyblockevent;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class EventMember {

	private String username = "";
	private String uuid = "";
	private String startingAmount = "";
	private String profileName = "";

	public EventMember() {}
}
