package com.skyblockplus.api.discordserversettings.mee6bypasser;

import com.skyblockplus.api.discordserversettings.automatedroles.RoleModel;
import com.skyblockplus.api.discordserversettings.automatedroles.RoleObject;
import java.util.ArrayList;
import java.util.List;
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
public class Mee6Data {

	private String enable = "false";

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<RoleObject> mee6Ranks = new ArrayList<>();

	public Mee6Data() {}
}
