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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CaptureStandardOutputText implements CaptureStandardOutput {
	private static Logger log = LogManager.getLogger();

	private final CapturedStreams captureOutStreamsBehavior;
	private final List<CapturedStdOutErrTextObserver> observers;
	private final Executor executorConsumer;

	/**
	 * @param executorConsumer each stream parser will be executed in separate thread, ensure the capacity is sufficient for 2 threads by process.
	 */
	public CaptureStandardOutputText(final CapturedStreams captureOutStreamsBehavior, final Executor executorConsumer) {
		this.captureOutStreamsBehavior = captureOutStreamsBehavior;
		observers = new ArrayList<>();
		this.executorConsumer = executorConsumer;
		if (executorConsumer == null) {
			throw new NullPointerException("\"executorConsumer\" can't to be null");
		}
	}

	/**
	 * @param executorConsumer each stream parser will be executed in separate thread, ensure the capacity is sufficient for 2 threads by process.
	 */
	public CaptureStandardOutputText(final Executor executorConsumer) {
		this(CapturedStreams.BOTH_STDOUT_STDERR, executorConsumer);
	}

	public synchronized List<CapturedStdOutErrTextObserver> getObservers() {
		return observers;
	}

	@Override
	public void stdOutStreamConsumer(final InputStream processInputStream, final ProcesslauncherLifecycle source) {
		if (captureOutStreamsBehavior.canCaptureStdout()) {
			parseStream(processInputStream, false, source);
		}
	}

	@Override
	public void stdErrStreamConsumer(final InputStream processInputStream, final ProcesslauncherLifecycle source) {
		if (captureOutStreamsBehavior.canCaptureStderr()) {
			parseStream(processInputStream, true, source);
		}
	}

	private void parseStream(final InputStream processStream,
	                         final boolean isStdErr,
	                         final ProcesslauncherLifecycle source) {
		final List<CapturedStdOutErrTextObserver> finalObservers;
		synchronized (this) {
			finalObservers = Collections.unmodifiableList(new ArrayList<>(observers));
		}

		executorConsumer.execute(() -> {
			try {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(processStream));
				try {
					String line = "";
					while ((line = reader.readLine()) != null) {
						final LineEntry lineEntry = new LineEntry(System.currentTimeMillis(), line, isStdErr, source);
						finalObservers.forEach(observer -> {
							try {
								observer.onText(lineEntry);
							} catch (final RuntimeException e) {
								log.error("Can't callback process text event ", e);
							}
						});
					}
				} catch (final IOException ioe) {
					if (ioe.getMessage().equalsIgnoreCase("Bad file descriptor")) {
						if (log.isTraceEnabled()) {
							log.trace("Bad file descriptor, " + toString());
						}
					} else if (ioe.getMessage().equalsIgnoreCase("Stream closed")) {
						if (log.isTraceEnabled()) {
							log.trace("Stream closed, " + toString());
						}
					} else {
						throw ioe;
					}
				} catch (final Exception e) {
					log.error("Trouble during process " + toString(), e);
				} finally {
					reader.close();
				}
			} catch (final IOException ioe) {
				log.error("Trouble opening process streams: " + toString(), ioe);
			} finally {
				finalObservers.forEach(observer -> {
					observer.onProcessCloseStream(source, isStdErr, captureOutStreamsBehavior);
				});
			}
		});
	}

}
