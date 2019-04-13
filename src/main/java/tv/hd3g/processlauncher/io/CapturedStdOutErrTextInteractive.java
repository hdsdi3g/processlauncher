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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CapturedStdOutErrTextInteractive implements CapturedStdOutErrTextObserver {
	private static Logger log = LogManager.getLogger();

	private final Function<LineEntry, String> interactive;
	private final BiConsumer<ProcesslauncherLifecycle, Boolean> onDone;
	private final Charset destCharset;
	private final Executor eventExecutor;

	/**
	 * @param interactive function return null if nothing to send.
	 * @param onDone -> source, isStdErr
	 * @param destCharset used for injected String to byte[] in stream
	 */
	public CapturedStdOutErrTextInteractive(final Function<LineEntry, String> interactive, final BiConsumer<ProcesslauncherLifecycle, Boolean> onDone, final Charset destCharset, final Executor eventExecutor) {
		this.eventExecutor = Objects.requireNonNull(eventExecutor, "\"eventExecutor\" can't to be null");
		this.interactive = Objects.requireNonNull(interactive, "\"interactive\" can't to be null");
		this.onDone = Objects.requireNonNull(onDone, "\"onDone\" can't to be null");
		this.destCharset = Objects.requireNonNull(destCharset, "\"destCharset\" can't to be null");
	}

	/**
	 * @param interactive function return null if nothing to send.
	 * @param onDone -> source, isStdErr
	 * @param destCharset used for injected String to byte[] in stream
	 */
	public CapturedStdOutErrTextInteractive(final Function<LineEntry, String> interactive, final BiConsumer<ProcesslauncherLifecycle, Boolean> onDone, final Executor eventExecutor) {
		this(interactive, onDone, Charset.defaultCharset(), eventExecutor);
	}

	/**
	 * Sync (blocking) execution.
	 * @param interactive function return null if nothing to send.
	 * @param onDone -> source, isStdErr
	 */
	public CapturedStdOutErrTextInteractive(final Function<LineEntry, String> interactive, final BiConsumer<ProcesslauncherLifecycle, Boolean> onDone) {
		this(interactive, onDone, Charset.defaultCharset(), r -> r.run());
	}

	@Override
	public void onText(final LineEntry lineEntry) {
		eventExecutor.execute(() -> {
			final String result = interactive.apply(lineEntry);
			final ProcesslauncherLifecycle source = lineEntry.getSource();

			if (result != null & source.isRunning()) {
				try {
					source.getStdInInjection().println(result, destCharset);
				} catch (final IOException e) {
					log.error("Can't send some text to process", e);
				}
			}
		});
	}

	@Override
	public void onProcessCloseStream(final ProcesslauncherLifecycle source, final boolean isStdErr) {
		eventExecutor.execute(() -> {
			onDone.accept(source, isStdErr);
		});
	}

}
