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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import tv.hd3g.processlauncher.ProcesslauncherBuilder;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.io.CapturedStreams;

public class ToolRunner {

	private final ExecutableFinder executableFinder;
	private final ThreadPoolExecutor executor;
	private final AtomicLong executorWatcherId;

	public ToolRunner(final ExecutableFinder executableFinder, final int maximumInParallel) {
		this.executableFinder = Objects.requireNonNull(executableFinder, "\"executableFinder\" can't to be null");
		executorWatcherId = new AtomicLong();
		executor = new ThreadPoolExecutor(1, maximumInParallel, 1l, TimeUnit.SECONDS,
		        new LinkedBlockingQueue<Runnable>(), r -> {
			        final Thread t = new Thread(r);
			        t.setPriority(Thread.MIN_PRIORITY);
			        t.setDaemon(false);
			        t.setName("Executable starter");
			        return t;
		        });
	}

	private class LocalRunningTool<T extends ExecutableTool> implements RunningTool<T> {

		private final CapturedStdOutErrTextRetention textRetention;
		private final ProcesslauncherLifecycle lifecyle;
		private final T execTool;

		private LocalRunningTool(final CapturedStdOutErrTextRetention textRetention,
		                         final ProcesslauncherLifecycle lifecyle,
		                         final T execTool) {
			this.textRetention = textRetention;
			this.lifecyle = lifecyle;
			this.execTool = execTool;
		}

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

	public <T extends ExecutableTool> CompletableFuture<RunningTool<T>> execute(final T execTool) {
		final Executor executorStdOutWatchers = r -> {
			final Thread t = new Thread(r);
			t.setDaemon(true);
			t.setName("Executable sysouterr watcher for "
			          + execTool.getExecutableName()
			          + " TId#" + executorWatcherId.getAndAdd(1));
			t.start();
		};

		return CompletableFuture.supplyAsync(() -> {
			final String executableName = execTool.getExecutableName();
			try {
				final CommandLine cmd = new CommandLine(executableName, execTool.getReadyToRunParameters(),
				        executableFinder);
				final ProcesslauncherBuilder builder = new ProcesslauncherBuilder(cmd);
				final CapturedStdOutErrTextRetention textRetention = new CapturedStdOutErrTextRetention();
				builder.getSetCaptureStandardOutputAsOutputText(
				        CapturedStreams.BOTH_STDOUT_STDERR, executorStdOutWatchers)
				        .getObservers()
				        .add(textRetention);

				execTool.beforeRun(builder);
				final ProcesslauncherLifecycle lifecyle = builder.start();

				return new LocalRunningTool<>(textRetention, lifecyle, execTool);
			} catch (final IOException e) {
				throw new RuntimeException("Can't start " + executableName, e);
			}
		}, executor);
	}

	public ExecutableFinder getExecutableFinder() {
		return executableFinder;
	}
}
