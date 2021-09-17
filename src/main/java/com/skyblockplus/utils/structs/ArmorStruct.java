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

package com.skyblockplus.utils.structs;

public class ArmorStruct {

	private String helmet;
	private String chestplate;
	private String leggings;
	private String boots;

	public ArmorStruct() {
		this.helmet = "Empty";
		this.chestplate = "Empty";
		this.leggings = "Empty";
		this.boots = "Empty";
	}

	public ArmorStruct(String helmet, String chestplate, String leggings, String boots) {
		this.helmet = helmet;
		this.chestplate = chestplate;
		this.leggings = leggings;
		this.boots = boots;
	}

	public String getHelmet() {
		return helmet;
	}

	public void setHelmet(String helmet) {
		this.helmet = helmet;
	}

	public String getChestplate() {
		return chestplate;
	}

	public void setChestplate(String chestplate) {
		this.chestplate = chestplate;
	}

	public String getLeggings() {
		return leggings;
	}

	public void setLeggings(String leggings) {
		this.leggings = leggings;
	}

	public String getBoots() {
		return boots;
	}

	public void setBoots(String boots) {
		this.boots = boots;
	}

	public ArmorStruct makeBold() {
		setHelmet("**" + getHelmet() + "**");
		setChestplate("**" + getChestplate() + "**");
		setLeggings("**" + getLeggings() + "**");
		setBoots("**" + getBoots() + "**");
		return this;
	}
}
