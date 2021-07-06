package com.skyblockplus.utils.structs;

import java.util.concurrent.atomic.AtomicInteger;

public class HypixelKeyInformation {

	public final AtomicInteger remainingLimit = new AtomicInteger(120);
	public final AtomicInteger timeTillReset = new AtomicInteger(0);
}
