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

import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

/**
 * Shortcut for alls classes
 */
public class Exec {// TODO test

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
	public CapturedStdOutErrTextRetention runWaitGetText(final Consumer<ProcesslauncherBuilder> beforeRun) throws IOException {
		final CommandLine commandLine = new CommandLine(execName, parameters, executableFinder);
		final List<String> params = commandLine.getParametersInjectVars(varsToInject, removeParamsIfNoVarToInject);
		final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(commandLine.getExecutable(), params, executableFinder);

		final ExecutorService outStreamWatcher = Executors.newFixedThreadPool(2);
		final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention();
		builder.setCaptureStandardOutput(outStreamWatcher, textRetention);

		beforeRun.accept(builder);
		builder.addExecutionCallbacker(new ExecutionCallbacker() {
			@Override
			public void onEndExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
				outStreamWatcher.shutdown();
			}
		});

		builder.start().checkExecution();

		return textRetention;
	}

}
