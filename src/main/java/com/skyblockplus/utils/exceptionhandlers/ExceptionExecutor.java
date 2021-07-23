package com.skyblockplus.utils.exceptionhandlers;

import static com.skyblockplus.Main.globalExceptionHandler;

import java.util.concurrent.*;

public class ExceptionExecutor extends ThreadPoolExecutor {

	public ExceptionExecutor() {
		super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
	}

	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);
		if (t == null && r instanceof Future<?>) {
			try {
				Future<?> future = (Future<?>) r;
				if (future.isDone()) {
					future.get();
				}
			} catch (CancellationException ce) {
				t = ce;
			} catch (ExecutionException ee) {
				t = ee.getCause();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}

		if (t != null) {
			globalExceptionHandler.uncaughtException(null, t);
		}
	}
}
