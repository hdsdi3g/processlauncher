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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableCommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CaptureStandardOutputStreams;
import tv.hd3g.processlauncher.io.CaptureStandardOutputText;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

/**
 * Shortcut for alls classes
 */
public class Exec {
	private static Logger log = LogManager.getLogger();

	private final String execName;
	private final ExecutableFinder executableFinder;
	private final Parameters parameters;
	private final Map<String, String> varsToInject;
	private boolean removeParamsIfNoVarToInject;
	
	public Exec(final String execName, final ExecutableFinder executableFinder) {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		parameters = new Parameters();
		varsToInject = new HashMap<>();
	}
	
	public Map<String, String> getVarsToInject() {
		return varsToInject;
	}

	public Exec setRemoveParamsIfNoVarToInject(final boolean removeParamsIfNoVarToInject) {
		this.removeParamsIfNoVarToInject = removeParamsIfNoVarToInject;
		return this;
	}

	public boolean isRemoveParamsIfNoVarToInject() {
		return removeParamsIfNoVarToInject;
	}
	
	public Parameters getParameters() {
		return parameters;
	}
	
	/**
	 * Blocking
	 */
	public CapturedStdOutErrTextRetention run(final Consumer<ProcesslauncherBuilder> beforeRun) throws IOException {
		final CommandLine commandLine = new CommandLine(execName, parameters);
		final List<String> params = commandLine.getParametersInjectVars(varsToInject, removeParamsIfNoVarToInject);
		final ExecutableCommandLine executableCommandLine = new ExecutableCommandLine(execName, params, executableFinder);
		final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(executableCommandLine);
		
		final ExecutorService outStreamWatcher = Executors.newFixedThreadPool(2);
		final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention(CaptureStandardOutputStreams.BOTH_STDOUT_STDERR);
		builder.setCaptureStandardOutput(new CaptureStandardOutputText(outStreamWatcher, textRetention));
		
		beforeRun.accept(builder);
		builder.setExecutionCallbacker(new ExecutionCallbacker() {
			@Override
			public void onEndExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
				outStreamWatcher.shutdown();
			}
		});
		
		final Processlauncher processlauncher = new Processlauncher(builder);
		final ProcesslauncherLifecycle processlauncherLifecycle = new ProcesslauncherLifecycle(processlauncher);
		processlauncherLifecycle.checkExecution();
		
		return textRetention;
	}
	
}
