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

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CapturedStdOutErrTextRetention implements CapturedStdOutErrTextObserver {

	private final CapturedStreams streamToKeep;
	private final LinkedBlockingQueue<LineEntry> lineEntries;
	private final AtomicBoolean closedStdoutProcess;
	private final AtomicBoolean closedStderrProcess;

	public CapturedStdOutErrTextRetention(final CapturedStreams streamToKeep) {
		this.streamToKeep = Objects.requireNonNull(streamToKeep, "\"streamToKeep\" can't to be null");
		lineEntries = new LinkedBlockingQueue<>();
		closedStdoutProcess = new AtomicBoolean(streamToKeep.canCaptureStdout() == false);
		closedStderrProcess = new AtomicBoolean(streamToKeep.canCaptureStderr() == false);
	}

	@Override
	public void onProcessCloseStream(final ProcesslauncherLifecycle source,
	                                 final boolean isStdErr,
	                                 final CapturedStreams streamToKeepPolicy) {
		if (isStdErr) {
			closedStderrProcess.set(true);
		} else {
			closedStdoutProcess.set(true);
		}
	}

	/**
	 * Blocking
	 */
	public void waitForClosedStream(final ProcesslauncherLifecycle source) {
		while (closedStdoutProcess.get() == false && closedStderrProcess.get() == false) {
			Thread.onSpinWait();
		}
	}

	/**
	 * With BOTH_STDOUT_STDERR
	 */
	public CapturedStdOutErrTextRetention() {
		this(CapturedStreams.BOTH_STDOUT_STDERR);
	}

	@Override
	public void onText(final LineEntry lineEntry) {
		if (lineEntry.canUseThis(streamToKeep) == false) {
			return;
		}
		if (lineEntry.isStdErr() && closedStderrProcess.get()) {
			throw new IllegalStateException("A new line try to be added in the internal stderr list: \""
			                                + lineEntry.getLine() + "\"");
		} else if (lineEntry.isStdErr() == false && closedStdoutProcess.get()) {
			throw new IllegalStateException("A new line try to be added in the internal stdout list: \""
			                                + lineEntry.getLine() + "\"");
		}
		lineEntries.add(lineEntry);
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 */
	public Stream<String> getStdoutLines(final boolean keepEmptyLines) {
		return lineEntries.stream().filter(le -> {
			if (keepEmptyLines) {
				return true;
			}
			return le.getLine().equals("") == false;
		}).filter(le -> {
			return le.isStdErr() == false;
		}).map(le -> {
			return le.getLine();
		});
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 * @param keepEmptyLines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStderrLines(final boolean keepEmptyLines) {
		return lineEntries.stream().filter(le -> {
			if (keepEmptyLines) {
				return true;
			}
			return le.getLine().equals("") == false;
		}).filter(le -> {
			return le.isStdErr();
		}).map(le -> {
			return le.getLine();
		});
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 * @param keepEmptyLines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStdouterrLines(final boolean keepEmptyLines) {
		return lineEntries.stream().filter(le -> {
			if (keepEmptyLines) {
				return true;
			}
			return le.getLine().equals("") == false;
		}).map(le -> {
			return le.getLine();
		});
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keepEmptyLines if set false, discard all empty trimed lines
	 * @param newLineSeparator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdout(final boolean keepEmptyLines, final String newLineSeparator) {
		return getStdoutLines(keepEmptyLines).collect(Collectors.joining(newLineSeparator));
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keepEmptyLines if set false, discard all empty trimed lines
	 * @param newLineSeparator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStderr(final boolean keepEmptyLines, final String newLineSeparator) {
		return getStderrLines(keepEmptyLines).collect(Collectors.joining(newLineSeparator));
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keepEmptyLines if set false, discard all empty trimed lines
	 * @param newLineSeparator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdouterr(final boolean keepEmptyLines, final String newLineSeparator) {
		return getStdouterrLines(keepEmptyLines).collect(Collectors.joining(newLineSeparator));
	}

}
