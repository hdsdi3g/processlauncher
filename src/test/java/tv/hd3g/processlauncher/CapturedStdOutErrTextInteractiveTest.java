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
package tv.hd3g.processlauncher;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CapturedStdOutErrTextInteractiveTest extends TestCase {

	public void test() throws Exception {
		final ProcesslauncherLifecycle source = Mockito.mock(ProcesslauncherLifecycle.class);
		Mockito.when(source.isRunning()).thenReturn(true);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final StdInInjection stdInInject = new StdInInjection(baos);

		Mockito.when(source.getStdInInjection()).thenReturn(stdInInject);

		final List<LineEntry> capturedLe = new ArrayList<>();
		final Function<LineEntry, String> interactive = le -> {
			if (le.getSource().equals(source) == false) {
				throw new RuntimeException("Invalid source");
			}
			capturedLe.add(le);
			return le.getLine().toUpperCase();
		};

		final CapturedStdOutErrTextInteractive csoeti = new CapturedStdOutErrTextInteractive(interactive);
		final LineEntry added = new LineEntry(0, "My text", true, source);
		csoeti.onText(added);
		// csoeti.onProcessCloseStream(source, true, CapturedStreams.BOTH_STDOUT_STDERR);

		Assert.assertEquals(1, capturedLe.size());
		Assert.assertEquals(added, capturedLe.get(0));
		Assert.assertEquals("My text".toUpperCase() + System.lineSeparator(), new String(baos.toByteArray()));
	}
}
