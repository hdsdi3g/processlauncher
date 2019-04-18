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
package tv.hd3g.processlauncher.tool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

public interface RunningTool<T> {

	CapturedStdOutErrTextRetention getTextRetention();

	ProcesslauncherLifecycle getLifecyle();

	default CompletableFuture<CapturedStdOutErrTextRetention> checkExecutionGetText(final Executor executor) {
		return CompletableFuture.runAsync(() -> {
			getLifecyle().checkExecution();
		}, executor).thenApplyAsync(v -> {
			return getTextRetention();
		}, executor);
	}

	/**
	 * Don't checks end status (ok/error).
	 */
	default CompletableFuture<RunningTool<T>> waitForEnd(final Executor executor) {
		return CompletableFuture.runAsync(() -> {
			getLifecyle().waitForEnd();
		}, executor).thenApplyAsync(v -> {
			return this;
		}, executor);
	}

	T getExecutableToolSource();
}
