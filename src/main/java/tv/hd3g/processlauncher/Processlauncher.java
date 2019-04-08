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

public class Processlauncher { // TODO test
	
	/*
	enum CaptureOutStreamsBehavior { BOTH_STDOUT_STDERR ONLY_STDOUT ONLY_STDERR
	StdOutErrObserver onText(ExecProcessTextResult source, String line, boolean is_std_err)
	StdOutErrCallback onStdout(ExecProcessTextResult source, String line) ...
	InteractiveExecProcessHandler ->  onText(ExecProcessTextResult source, String line, boolean is_std_err)

	ExecProcessText extends ExecProcess
	ExecProcessTextResult extends ExecProcessResult

	TODO refactor: eclipse coll, Objects.requireNonNull, exec_name/execName
	 */

	private final boolean execCodeMustBeZero;
	private final Optional<EndExecutionCallbacker> endExecutionCallbacker;
	private final Optional<ExecutionTimeLimiter> executionTimeLimiter;
	private final ProcessBuilder processBuilder;
	private final String fullCommandLine;

	public Processlauncher(final ProcesslauncherBuilder builder) {
		execCodeMustBeZero = builder.isExecCodeMustBeZero();
		endExecutionCallbacker = builder.getEndExecutionCallbacker();
		executionTimeLimiter = builder.getExecutionTimeLimiter();
		processBuilder = builder.makeProcessBuilder();
		fullCommandLine = builder.getFullCommandLine();
	}
	
	public ProcesslauncherLifecycle start() throws IOException {
		return new ProcesslauncherLifecycle(this);
	}
	
	public Optional<EndExecutionCallbacker> getEndExecutionCallbacker() {
		return endExecutionCallbacker;
	}

	public Optional<ExecutionTimeLimiter> getExecutionTimeLimiter() {
		return executionTimeLimiter;
	}

	public boolean isExecCodeMustBeZero() {
		return execCodeMustBeZero;
	}

	public ProcessBuilder getProcessBuilder() {
		return processBuilder;
	}

	/**
	 * @return getFullCommandLine()
	 */
	@Override
	public String toString() {
		return fullCommandLine;
	}

	public String getFullCommandLine() {
		return fullCommandLine;
	}
}
