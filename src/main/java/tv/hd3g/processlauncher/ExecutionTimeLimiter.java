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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutionTimeLimiter {
	
	protected ScheduledExecutorService max_exec_time_scheduler;
	protected long max_exec_time = Long.MAX_VALUE;

	public ExecutionTimeLimiter(final long max_exec_time, final TimeUnit unit, final ScheduledExecutorService max_exec_time_scheduler) {
		if (max_exec_time == 0) {
			throw new RuntimeException("Invalid max_exec_time value: " + max_exec_time);
		}
		if (max_exec_time_scheduler == null) {
			throw new NullPointerException("\"max_exec_time_scheduler\" can't to be null");
		}

		this.max_exec_time_scheduler = max_exec_time_scheduler;
		this.max_exec_time = unit.toMillis(max_exec_time);
	}

	public long getMaxExecTime(final TimeUnit unit) {
		return unit.convert(max_exec_time, TimeUnit.MILLISECONDS);
	}
	
	void addTimesUp(final ProcesslauncherLifecycle toCallBack, final Process process) {
		// TODO
		/*if (max_exec_time < Long.MAX_VALUE) {
			max_exec_time_stopper = max_exec_time_scheduler.schedule(() -> {
				process_was_stopped_because_too_long_time = true;
				killProcessTree(process);
			}, max_exec_time, TimeUnit.MILLISECONDS);
		
		}*/
		
		/*process.onExit().thenRunAsync(() -> {
			max_exec_time_stopper.cancel(false);
		}, max_exec_time_scheduler);*/
	}

}
