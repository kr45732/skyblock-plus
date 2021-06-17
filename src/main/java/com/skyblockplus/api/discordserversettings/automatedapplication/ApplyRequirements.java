package com.skyblockplus.api.discordserversettings.automatedapplication;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class ApplyRequirements {

	private String slayerReq = "";
	private String skillsReq = "";
	private String catacombsReq = "";
	private String weightReq = "";

	public ApplyRequirements() {}
}
