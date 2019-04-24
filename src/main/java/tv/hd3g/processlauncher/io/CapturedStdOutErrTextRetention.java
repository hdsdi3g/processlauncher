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
package tv.hd3g.processlauncher.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CapturedStdOutErrTextRetention implements CapturedStdOutErrTextObserver {
	private static Logger log = LogManager.getLogger();

	private final CapturedStreams streamToKeep;
	private final LinkedBlockingQueue<LineEntry> lineEntries;
	@Deprecated
	private final List<CapturedStreamsObserver> observers;

	public CapturedStdOutErrTextRetention(final CapturedStreams streamToKeep) {
		this.streamToKeep = Objects.requireNonNull(streamToKeep, "\"streamToKeep\" can't to be null");
		lineEntries = new LinkedBlockingQueue<>();
		observers = Collections.synchronizedList(new ArrayList<>());
	}

	/**
	 * @return synchronizedList
	 */
	@Deprecated
	public List<CapturedStreamsObserver> getObservers() { // TODO test
		return observers;
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
		lineEntries.add(lineEntry);

		observers.forEach(o -> {
			try {
				o.onTextCaptured(lineEntry);
			} catch (final Exception e) {
				log.error("Can't callback observer " + o, e);
			}
		});
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
