/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021 kr45732
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.skyblockplus.api.serversettings.automatedroles;

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
	private RoleModel enderman = new RoleModel();

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
	private RoleModel weight = new RoleModel();

	@Embedded
	private RoleModel guild_member = new RoleModel();

	@Embedded
	private RoleModel guild_ranks = new RoleModel();

	@Embedded
	private RoleModel bank_coins = new RoleModel();

	@Embedded
	private RoleModel fairy_souls = new RoleModel();

	@Embedded
	private RoleModel slot_collector = new RoleModel();

	@Embedded
	private RoleModel pet_enthusiast = new RoleModel();

	@Embedded
	private RoleModel total_slayer = new RoleModel();

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

	public AutomatedRoles() {}
}
