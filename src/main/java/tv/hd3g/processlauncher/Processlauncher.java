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

import tv.hd3g.processlauncher.io.CaptureStandardOutput;

public class Processlauncher { // TODO test
	
	/*
	InteractiveExecProcessHandler ->  onText(ExecProcessTextResult source, String line, boolean is_std_err)
	
	ExecProcessText extends ExecProcess
	ExecProcessTextResult extends ExecProcessResult
	
	TODO refactor: eclipse coll, Objects.requireNonNull, exec_name/execName
	 */
	
	private final boolean execCodeMustBeZero;
	private final Optional<EndExecutionCallbacker> endExecutionCallbacker;
	private final Optional<ExecutionTimeLimiter> executionTimeLimiter;
	private final Optional<CaptureStandardOutput> captureStandardOutput;
	private final ProcessBuilder processBuilder;
	private final String fullCommandLine;
	private final ProcesslauncherBuilder processlauncherBuilder;

	public Processlauncher(final ProcesslauncherBuilder processlauncherBuilder) {
		this.processlauncherBuilder = processlauncherBuilder;
		if (processlauncherBuilder == null) {
			throw new NullPointerException("\"processlauncherBuilder\" can't to be null");
		}
		
		execCodeMustBeZero = processlauncherBuilder.isExecCodeMustBeZero();
		endExecutionCallbacker = processlauncherBuilder.getEndExecutionCallbacker();
		executionTimeLimiter = processlauncherBuilder.getExecutionTimeLimiter();
		captureStandardOutput = processlauncherBuilder.captureStandardOutput();
		processBuilder = processlauncherBuilder.makeProcessBuilder();
		fullCommandLine = processlauncherBuilder.getFullCommandLine();
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
	
	public Optional<CaptureStandardOutput> getCaptureStandardOutput() {
		return captureStandardOutput;
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

	public ProcesslauncherBuilder getProcesslauncherBuilder() {
		return processlauncherBuilder;
	}
}
