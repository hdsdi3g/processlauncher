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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CaptureStdOutErrText implements CaptureStandardOutput {// TODO test me
	private static Logger log = LogManager.getLogger();

	private final CaptureOutStreamsBehavior captureOutStreamsBehavior;
	private final CapturedStdOutErrObserver observer;
	private final ExecutorService executorConsumer;

	/**
	 * @param executorConsumer each stream parser will be executed in separate thread, ensure the capacity is sufficient for 2 threads by process.
	 */
	public CaptureStdOutErrText(final CaptureOutStreamsBehavior captureOutStreamsBehavior, final CapturedStdOutErrObserver observer, final ExecutorService executorConsumer) {
		this.captureOutStreamsBehavior = captureOutStreamsBehavior;
		this.observer = observer;
		if (observer == null) {
			throw new NullPointerException("\"observer\" can't to be null");
		}
		this.executorConsumer = executorConsumer;
		if (executorConsumer == null) {
			throw new NullPointerException("\"executorConsumer\" can't to be null");
		}
	}
	
	/**
	 * @param executorConsumer each stream parser will be executed in separate thread, ensure the capacity is sufficient for 2 threads by process.
	 */
	public CaptureStdOutErrText(final CapturedStdOutErrObserver observer, final ExecutorService executorConsumer) {
		this(CaptureOutStreamsBehavior.BOTH_STDOUT_STDERR, observer, executorConsumer);
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
	
	private void parseStream(final InputStream processStream, final boolean isStdErr, final ProcesslauncherLifecycle source) {
		executorConsumer.execute(() -> {
			try {
				final BufferedReader reader = new BufferedReader(new InputStreamReader(processStream));
				try {
					String line = "";
					while ((line = reader.readLine()) != null) {
						observer.onText(source, line, isStdErr);
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
			}
		});
	}
	
}
