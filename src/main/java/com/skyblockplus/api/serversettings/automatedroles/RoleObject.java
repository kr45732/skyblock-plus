package com.skyblockplus.api.serversettings.automatedroles;

import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Embeddable
public class RoleObject {

	private String value = "";
	private String roleId = "";

	public RoleObject() {}
}
