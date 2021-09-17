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

import static com.skyblockplus.utils.Utils.roundAndFormat;

public class WeightStruct {

	public double base;
	public double overflow;

	public WeightStruct() {
		this(0, 0);
	}

	public WeightStruct(double base) {
		this(base, 0);
	}

	public WeightStruct(double base, double overflow) {
		this.base = base;
		this.overflow = overflow;
	}

	public WeightStruct add(WeightStruct o) {
		this.base += o.base;
		this.overflow += o.overflow;
		return o;
	}

	public String get() {
		return roundAndFormat(base + overflow) + (overflow > 0 ? " (" + roundAndFormat(base) + " + " + roundAndFormat(overflow) + ")" : "");
	}

	public double getRaw() {
		return base + overflow;
	}

	@Override
	public String toString() {
		return "base={" + base + "}, overflow={" + overflow + "}";
	}
}
