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
package tv.hd3g.processlauncher.tool;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.CapturedStreams;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.ProcessLifeCycleException;
import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class ToolRunner {

	private final ExecutableFinder executableFinder;

	public ToolRunner(final ExecutableFinder executableFinder) {
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
	}

	public ExecutableFinder getExecutableFinder() {
		return executableFinder;
	}

	public <T extends ExecutableTool> RunningTool<T> execute(final T execTool) {
		final String executableName = execTool.getExecutableName();
		try {
			final CommandLine cmd = new CommandLine(executableName, execTool.getReadyToRunParameters(),
			        executableFinder);
			final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(cmd);
			final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention();
			builder.getSetCaptureStandardOutputAsOutputText(CapturedStreams.BOTH_STDOUT_STDERR)
			        .addObserver(textRetention);
			execTool.beforeRun(builder);
			return new RunningTool<>(textRetention, builder.start(), execTool);
		} catch (final IOException e) {
			throw new ProcessLifeCycleException("Can't start " + executableName, e);
		}
	}

	public class RunningTool<T extends ExecutableTool> {
		private final CapturedStdOutErrTextRetention textRetention;
		private final ProcesslauncherLifecycle lifecyle;
		private final T execTool;

		private RunningTool(final CapturedStdOutErrTextRetention textRetention,
		                    final ProcesslauncherLifecycle lifecyle,
		                    final T execTool) {
			this.textRetention = textRetention;
			this.lifecyle = lifecyle;
			this.execTool = execTool;
		}

		public CapturedStdOutErrTextRetention getTextRetention() {
			return textRetention;
		}

		public ProcesslauncherLifecycle getLifecyle() {
			return lifecyle;
		}

		public T getExecutableToolSource() {
			return execTool;
		}

		/**
		 * Can throw an InvalidExecution, with stderr embedded.
		 * Blocking call (with CapturedStdOutErrTextRetention::waitForClosedStream)
		 */
		public CapturedStdOutErrTextRetention checkExecutionGetText() {
			final var currentLifecyle = getLifecyle();
			try {
				currentLifecyle.checkExecution();
				final var currentTextRetention = getTextRetention();
				currentTextRetention.waitForClosedStreams();
				return currentTextRetention;
			} catch (final InvalidExecution e) {
				throw e.injectStdErr(getTextRetention().getStderrLines(false)
				        .filter(getExecutableToolSource().filterOutErrorLines())
				        .map(String::trim).collect(Collectors.joining("|")));
			}
		}

		/**
		 * Don't checks end status (ok/error).
		 * @return this
		 */
		public RunningTool<T> waitForEnd() {
			getLifecyle().waitForEnd();
			return this;
		}
	}
}
