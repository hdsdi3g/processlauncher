/*
 * This file is part of processlauncher.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
*/
package tv.hd3g.processlauncher;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ExecutionTimeLimiter {

	private final ScheduledExecutorService maxExecTimeScheduler;
	private final long maxExecTime;

	public ExecutionTimeLimiter(final long maxExecTime, final TimeUnit unit, final ScheduledExecutorService maxExecTimeScheduler) {
		if (maxExecTime == 0) {
			throw new RuntimeException("Invalid max_exec_time value: " + maxExecTime);
		}
		this.maxExecTimeScheduler = Objects.requireNonNull(maxExecTimeScheduler, "\"maxExecTimeScheduler\" can't to be null");
		this.maxExecTime = unit.toMillis(Math.abs(maxExecTime));
	}

	public long getMaxExecTime(final TimeUnit unit) {
		return unit.convert(maxExecTime, TimeUnit.MILLISECONDS);
	}

	void addTimesUp(final ProcesslauncherLifecycle toCallBack, final Process process) {
		final ScheduledFuture<?> max_exec_time_stopper = maxExecTimeScheduler.schedule(() -> {
			toCallBack.runningTakesTooLongTimeStopIt();
		}, maxExecTime, TimeUnit.MILLISECONDS);

		process.onExit().thenRunAsync(() -> {
			max_exec_time_stopper.cancel(false);
		}, maxExecTimeScheduler);
	}

}
