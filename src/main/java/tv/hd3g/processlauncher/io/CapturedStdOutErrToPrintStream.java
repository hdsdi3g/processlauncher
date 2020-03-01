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
package tv.hd3g.processlauncher.io;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;

public class CapturedStdOutErrToPrintStream implements CapturedStdOutErrTextObserver {

	private final PrintStream printStreamStdOut;
	private final PrintStream printStreamStdErr;
	private Optional<Predicate<LineEntry>> filter;

	public CapturedStdOutErrToPrintStream(final PrintStream printStreamStdOut, final PrintStream printStreamStdErr) {
		this.printStreamStdOut = Objects.requireNonNull(printStreamStdOut, "\"printStreamStdOut\" can't to be null");
		this.printStreamStdErr = Objects.requireNonNull(printStreamStdErr, "\"printStreamStdErr\" can't to be null");
		filter = Optional.empty();
	}

	public Optional<Predicate<LineEntry>> getFilter() {
		return filter;
	}

	public CapturedStdOutErrToPrintStream setFilter(final Predicate<LineEntry> filter) {
		this.filter = Optional.ofNullable(filter);
		return this;
	}

	private static String getExecNameWithoutExt(final ProcesslauncherLifecycle source) {
		final String execName = source.getLauncher().getExecutableName();

		if (ExecutableFinder.WINDOWS_EXEC_EXTENSIONS.stream().anyMatch(ext -> execName.toLowerCase().endsWith(ext
		        .toLowerCase()))) {
			return execName.substring(0, execName.length() - 4);
		} else {
			return execName;
		}

	}

	static final String stdOutSeparator = "\t> ";
	static final String stdErrSeparator = "\t! ";

	@Override
	public void onText(final LineEntry lineEntry) {
		if (filter.map(f -> f.test(lineEntry)).orElse(true) == false) {
			return;
		}

		final PrintStream out;
		if (lineEntry.isStdErr()) {
			out = printStreamStdErr;
		} else {
			out = printStreamStdOut;
		}

		final ProcesslauncherLifecycle source = lineEntry.getSource();
		out.print(getExecNameWithoutExt(source));
		out.print(source.getPID().map(pid -> "#" + pid).orElse(""));

		/*final long timeAgo = lineEntry.getTimeAgo();
		if (timeAgo > 1000) {
			out.print("\t");
			out.print(timeAgo / 1000);
			out.print("s ");
		}*/
		if (lineEntry.isStdErr()) {
			out.print(stdErrSeparator);
		} else {
			out.print(stdOutSeparator);
		}

		out.println(lineEntry.getLine());
		out.flush();
	}

	@Override
	public void onProcessCloseStream(final ProcesslauncherLifecycle source,
	                                 final boolean isStdErr,
	                                 final CapturedStreams streamToKeepPolicy) {
		if (CapturedStreams.BOTH_STDOUT_STDERR.equals(streamToKeepPolicy)) {
			if (source.isCorrectlyDone()) {
				return;
			}
		}

		final PrintStream out;
		if (isStdErr) {
			out = printStreamStdErr;
		} else {
			out = printStreamStdOut;
		}
		out.print(getExecNameWithoutExt(source));
		out.print(source.getPID().map(pid -> "#" + pid).orElse(""));
		out.print(" Ends ");
		out.print(source.getEndStatus().toString().toLowerCase());
		if (source.isCorrectlyDone() == false) {
			out.print(" return ");
			out.print(source.getExitCode());
		}
		out.print(" in ");
		if (source.getUptime(TimeUnit.SECONDS) == 0) {
			out.print(source.getCPUDuration(TimeUnit.MILLISECONDS));
			out.print(" msec.");
		} else {
			out.print(source.getUptime(TimeUnit.SECONDS));
			out.print(" sec.");
		}

		out.println();
		out.flush();
	}
}
