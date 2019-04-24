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

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

public class ToolRunner {

	private final ExecutableFinder executableFinder;
	private final ThreadPoolExecutor executor;

	public ToolRunner(final ExecutableFinder executableFinder, final int maximumInParallel) {
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");

		executor = new ThreadPoolExecutor(1, maximumInParallel, 1l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), r -> {
			final Thread t = new Thread(r);
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(false);
			t.setName("Executable starter");
			return t;
		});
	}

	public <T extends ExecutableTool> CompletableFuture<RunningTool<T>> execute(final T execTool) {
		final Executor executorStdOutWatchers = r -> {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Executable sysouterr watcher for " + execTool.getExecutableName());
			t.start();
		};

		return CompletableFuture.supplyAsync(() -> {
			final String executableName = execTool.getExecutableName();
			try {
				final CommandLine cmd = new CommandLine(executableName, execTool.getReadyToRunParameters(), executableFinder);
				final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(cmd);
				final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention();
				builder.setCaptureStandardOutput(executorStdOutWatchers, textRetention);
				execTool.beforeRun(builder);
				final ProcesslauncherLifecycle lifecyle = builder.start();

				class LocalRunningTool implements RunningTool<T> {

					@Override
					public CapturedStdOutErrTextRetention getTextRetention() {
						return textRetention;
					}

					@Override
					public ProcesslauncherLifecycle getLifecyle() {
						return lifecyle;
					}

					@Override
					public T getExecutableToolSource() {
						return execTool;
					}

				}
				return new LocalRunningTool();
			} catch (final IOException e) {
				throw new RuntimeException("Can't start " + executableName, e);
			}
		}, executor);
	}

	public ExecutableFinder getExecutableFinder() {
		return executableFinder;
	}
}
