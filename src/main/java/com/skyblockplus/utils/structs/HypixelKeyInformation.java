package com.skyblockplus.utils.structs;

import java.util.concurrent.atomic.AtomicInteger;

public class HypixelKeyInformation {

	public AtomicInteger remainingLimit = new AtomicInteger(120);
	public AtomicInteger timeTillReset = new AtomicInteger(0);
}
