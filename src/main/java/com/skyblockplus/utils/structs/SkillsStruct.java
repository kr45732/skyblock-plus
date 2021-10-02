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

import lombok.Data;

@Data
public class SkillsStruct {

	private final String name;
	private final int currentLevel;
	private final int maxLevel;
	private final long totalExp;
	private final long expCurrent;
	private final long expForNext;
	private final double progressToNext;

	public boolean isMaxed(){
		return currentLevel == maxLevel;
	}

	public double getProgressLevel(){
		return currentLevel + progressToNext;
	}
}
