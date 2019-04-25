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

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tv.hd3g.processlauncher.io.CaptureStandardOutput;
import tv.hd3g.processlauncher.io.CaptureStandardOutputText;
import tv.hd3g.processlauncher.io.CapturedStreams;

public interface ProcesslauncherBuilderShortcutTraits {

	ProcesslauncherBuilder setCaptureStandardOutput(final CaptureStandardOutput captureStandardOutput);

	Optional<CaptureStandardOutput> getCaptureStandardOutput();

	/**
	 * Shortcut for CaptureStandardOutputText. Set if missing or not a CaptureStandardOutputText.
	 */
	default CaptureStandardOutputText getSetCaptureStandardOutputAsOutputText(final CapturedStreams defaultCaptureOutStreamsBehavior, final Executor defaultExecutorConsumer) {
		final CaptureStandardOutputText csot = getCaptureStandardOutput().filter(cso -> cso instanceof CaptureStandardOutputText).map(cso -> (CaptureStandardOutputText) cso).orElseGet(() -> {
			return new CaptureStandardOutputText(defaultCaptureOutStreamsBehavior, defaultExecutorConsumer);
		});

		setCaptureStandardOutput(csot);
		return csot;
	}

	/**
	 * Shortcut for CaptureStandardOutputText. Set if missing or not a CaptureStandardOutputText.
	 * Capture all, in the ForkJoinPool.
	 */
	default CaptureStandardOutputText getSetCaptureStandardOutputAsOutputText() {
		return getSetCaptureStandardOutputAsOutputText(CapturedStreams.BOTH_STDOUT_STDERR, ForkJoinPool.commonPool());
	}

	/**
	 * @return new Processlauncher(this)
	 */
	Processlauncher toProcesslauncher();

	/**
	 * @return toProcesslauncher().start()
	 */
	default ProcesslauncherLifecycle start() throws IOException {
		return toProcesslauncher().start();
	}

	ProcesslauncherBuilder setExecutionTimeLimiter(final ExecutionTimeLimiter executionTimeLimiter);

	/**
	 * Shortcut for setExecutionTimeLimiter
	 */
	default ProcesslauncherBuilder setExecutionTimeLimiter(final long maxExecTime, final TimeUnit unit, final ScheduledExecutorService maxExecTimeScheduler) {
		return setExecutionTimeLimiter(new ExecutionTimeLimiter(maxExecTime, unit, maxExecTimeScheduler));
	}

}
