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

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CapturedStdOutErrTextRetention implements CapturedStdOutErrTextObserver {
	
	private final CapturedStreams streamToKeep;
	private final LinkedBlockingQueue<LineEntry> lineEntries;
	
	public CapturedStdOutErrTextRetention(final CapturedStreams streamToKeep) {
		this.streamToKeep = Objects.requireNonNull(streamToKeep, "\"streamToKeep\" can't to be null");
		lineEntries = new LinkedBlockingQueue<>();
	}

	@Override
	public void onText(final LineEntry lineEntry) {
		if (lineEntry.canUseThis(streamToKeep) == false) {
			return;
		}
		lineEntries.add(lineEntry);
	}

	/**
	 * Only set if setKeepStdout is set (false by default), else return empty stream.
	 */
	public Stream<String> getStdoutLines(final boolean keep_empty_lines) {
		return lineEntries.stream().filter(le -> {
			if (keep_empty_lines) {
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
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStderrLines(final boolean keep_empty_lines) {
		return lineEntries.stream().filter(le -> {
			if (keep_empty_lines) {
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
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 */
	public Stream<String> getStdouterrLines(final boolean keep_empty_lines) {
		return lineEntries.stream().filter(le -> {
			if (keep_empty_lines) {
				return true;
			}
			return le.getLine().equals("") == false;
		}).map(le -> {
			return le.getLine();
		});
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdout(final boolean keep_empty_lines, final String new_line_separator) {
		return getStdoutLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStderr(final boolean keep_empty_lines, final String new_line_separator) {
		return getStderrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
	/**
	 * Only set if setKeepStdout is set (false by default), else return empty text.
	 * @param keep_empty_lines if set false, discard all empty trimed lines
	 * @param new_line_separator replace new line char by this
	 *        Use System.lineSeparator() if needed
	 */
	public String getStdouterr(final boolean keep_empty_lines, final String new_line_separator) {
		return getStdouterrLines(keep_empty_lines).collect(Collectors.joining(new_line_separator));
	}
	
}
