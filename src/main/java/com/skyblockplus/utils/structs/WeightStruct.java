package com.skyblockplus.utils.structs;

import static com.skyblockplus.utils.Utils.*;

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
