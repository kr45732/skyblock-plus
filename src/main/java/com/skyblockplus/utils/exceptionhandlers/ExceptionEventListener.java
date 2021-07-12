package com.skyblockplus.utils.exceptionhandlers;

import static com.skyblockplus.Main.globalExceptionHandler;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;

public class ExceptionEventListener implements EventListener {

	private final EventListener listener;

	public ExceptionEventListener(EventListener listener) {
		this.listener = listener;
	}

	@Override
	public void onEvent(@NotNull GenericEvent event) {
		try {
			listener.onEvent(event);
		} catch (Exception e) {
			globalExceptionHandler.uncaughtException(null, e);
		}
	}
}
