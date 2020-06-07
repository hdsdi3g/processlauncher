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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * Copyright (C) hdsdi3g for hd3g.tv 2019
 *
 */
package tv.hd3g.processlauncher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.ExecutableTool;

/**
 * Shortcut for some classes
 * Reusable
 */
public class Exec implements ExecutableTool {

	private final String execName;
	private final File execFile;
	private final ExecutableFinder executableFinder;
	private final Parameters parameters;
	private final Map<String, String> varsToInject;
	private boolean removeParamsIfNoVarToInject;
	private final Consumer<ProcesslauncherBuilder> preBeforeRun;

	public Exec(final String execName, final ExecutableFinder executableFinder) throws FileNotFoundException {
		this.execName = Objects.requireNonNull(execName, "\"execName\" can't to be null");
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		execFile = executableFinder.get(execName);
		parameters = new Parameters();
		varsToInject = new HashMap<>();
		preBeforeRun = processBuilder -> {
		};
	}

	public Exec(final ExecutableTool tool, final ExecutableFinder executableFinder) throws FileNotFoundException {
		execName = Objects.requireNonNull(tool.getExecutableName(), "\"tool#getExecutableName\" can't to be null");
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		execFile = executableFinder.get(execName);
		parameters = tool.getReadyToRunParameters();
		varsToInject = new HashMap<>();
		preBeforeRun = tool::beforeRun;
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

	@Override
	public Parameters getReadyToRunParameters() {
		if (varsToInject.isEmpty()) {
			return parameters.duplicate().removeVariables(removeParamsIfNoVarToInject);
		} else {
			return parameters.duplicate().injectVariables(varsToInject, removeParamsIfNoVarToInject);
		}
	}

	@Override
	public String getExecutableName() {
		return execName;
	}

	public ExecutableFinder getExecutableFinder() {
		return executableFinder;
	}

	public File getExecutableFile() {
		return execFile;
	}

	/**
	 * Blocking
	 * @throws InvalidExecution
	 */
	public CapturedStdOutErrTextRetention runWaitGetText() throws IOException {
		return runWaitGetText(null);
	}

	/**
	 * Blocking
	 * @param beforeRun can be null
	 * @throws InvalidExecution
	 */
	public CapturedStdOutErrTextRetention runWaitGetText(final Consumer<ProcesslauncherBuilder> beforeRun) throws IOException {
		final CommandLine commandLine = new CommandLine(execName, getReadyToRunParameters(), executableFinder);
		final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(commandLine);

		final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention();
		builder.getSetCaptureStandardOutputAsOutputText(CapturedStreams.BOTH_STDOUT_STDERR)
		        .addObserver(textRetention);

		preBeforeRun.accept(builder);
		if (beforeRun != null) {
			beforeRun.accept(builder);
		}

		final var lifcycle = builder.start();
		textRetention.waitForClosedStreams();
		try {
			lifcycle.checkExecution();
		} catch (final InvalidExecution e) {
			throw e.injectStdErr(textRetention.getStderr(false, " / "));
		}

		return textRetention;
	}

}
