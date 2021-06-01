package com.skyblockplus.api.discordserversettings.automatedroles;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.transaction.annotation.Transactional;

@Data
@AllArgsConstructor
@Embeddable
@Transactional
public class AutomatedRoles {

	private String enable = "false";

	@Embedded
	private RoleModel sven = new RoleModel();

	@Embedded
	private RoleModel rev = new RoleModel();

	@Embedded
	private RoleModel tara = new RoleModel();

	@Embedded
	private RoleModel bank_coins = new RoleModel();

	@Embedded
	private RoleModel alchemy = new RoleModel();

	@Embedded
	private RoleModel combat = new RoleModel();

	@Embedded
	private RoleModel fishing = new RoleModel();

	@Embedded
	private RoleModel farming = new RoleModel();

	@Embedded
	private RoleModel foraging = new RoleModel();

	@Embedded
	private RoleModel carpentry = new RoleModel();

	@Embedded
	private RoleModel mining = new RoleModel();

	@Embedded
	private RoleModel taming = new RoleModel();

	@Embedded
	private RoleModel enchanting = new RoleModel();

	@Embedded
	private RoleModel catacombs = new RoleModel();

	@Embedded
	private RoleModel guild_member = new RoleModel();

	@Embedded
	private RoleModel fairy_souls = new RoleModel();

	@Embedded
	private RoleModel slot_collector = new RoleModel();

	@Embedded
	private RoleModel pet_enthusiast = new RoleModel();

	@Embedded
	private RoleModel doom_slayer = new RoleModel();

	@Embedded
	private RoleModel all_slayer_nine = new RoleModel();

	@Embedded
	private RoleModel skill_average = new RoleModel();

	@Embedded
	private RoleModel pet_score = new RoleModel();

	@Embedded
	private RoleModel dungeon_secrets = new RoleModel();

	@Embedded
	private RoleModel guild_ranks = new RoleModel();

	public AutomatedRoles() {}
}
