/*
 * Skyblock Plus - A Skyblock focused Discord bot with many commands and customizable features to improve the experience of Skyblock players and guild staff!
 * Copyright (c) 2021-2023 kr45732
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import com.skyblockplus.api.serversettings.managers.ServerSettingsModel;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class RoleModel {

	@Id
	@GeneratedValue(generator = "role_model_seq", strategy = GenerationType.SEQUENCE)
	@SequenceGenerator(name = "role_model_seq", sequenceName = "role_model_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "server_settings_id")
	@JsonIgnore
	@Expose(serialize = false, deserialize = false)
	@ToString.Exclude
	private ServerSettingsModel serverSettings;

	private String name;

	@ElementCollection(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	private List<RoleObject> levels = new ArrayList<>();

	public RoleModel(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
		RoleModel that = (RoleModel) o;
		return id != null && Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	public void addLevel(String value, String roleId) {
		levels.removeIf(l -> l.getValue().equals(value));
		levels.add(new RoleObject(value, roleId));
		try {
			levels.sort(Comparator.comparingLong(r -> Long.parseLong(r.getValue())));
		} catch (Exception ignored) {
			// Catch for non-numeric roles (e.g. guild member, guild ranks, etc)
		}
	}
}
