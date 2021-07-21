package com.skyblockplus.utils.exceptionhandlers;

import static com.skyblockplus.Main.*;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecptionExecutor extends ThreadPoolExecutor {

	public ExecptionExecutor() {
		super(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
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
