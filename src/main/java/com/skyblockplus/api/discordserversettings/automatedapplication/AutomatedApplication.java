package com.skyblockplus.api.discordserversettings.automatedapplication;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Data
@AllArgsConstructor
@Embeddable
public class AutomatedApplication {

	private String name;

	private String enable = "false";
	private String messageTextChannelId = "";
	private String messageStaffChannelId = "";
	private String newChannelPrefix = "";
	private String newChannelCategory = "";
	private String waitingChannelId = "";
	private String ironmanOnly = "false";

	@Column(length = 2048)
	private String messageText = "";

	@Column(length = 2048)
	private String acceptMessageText = "";

	@Column(length = 2048)
	private String denyMessageText = "";

	@Column(length = 2048)
	private String waitlistedMessageText = "";

	private String staffPingRoleId = "";

	private String previousMessageId = "";

	@Column(columnDefinition = "TEXT")
	private String applyUsersCache = "";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<ApplyRequirements> applyReqs = new ArrayList<>();

	public AutomatedApplication() {}

	public AutomatedApplication(String name) {
		this.name = name;
	}
}
