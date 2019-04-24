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
import java.util.stream.Collectors;

import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

/**
 * @see ToolRun
 */
public interface RunningTool<T extends ExecutableTool> {

	CapturedStdOutErrTextRetention getTextRetention();

	ProcesslauncherLifecycle getLifecyle();

	T getExecutableToolSource();

	/**
	 * Can throw an InvalidExecution, with stderr embedded.
	 * Usage example: toolRun.execute(myExecTool).thenApply(RunningTool::checkExecutionGetText))
	 */
	default CapturedStdOutErrTextRetention checkExecutionGetText() {
		try {
			getLifecyle().checkExecution();
		} catch (final InvalidExecution e) {
			e.setStdErr(getTextRetention().getStderrLines(false).filter(getExecutableToolSource().filterOutErrorLines()).collect(Collectors.joining(" / ")));
			throw e;
		}
		return getTextRetention();
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
}
