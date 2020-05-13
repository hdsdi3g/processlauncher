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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CaptureStandardOutputTextTest {

	@Test
	public void test() throws InterruptedException {
		final List<LineEntry> capturedlines = new ArrayList<>();

		final CountDownLatch cdl = new CountDownLatch(2);
		final CapturedStdOutErrText csoeto = new CapturedStdOutErrText() {

			@Override
			public void onText(final LineEntry lineEntry) {
				capturedlines.add(lineEntry);
			}

		};

		final CaptureStandardOutputText csot = new CaptureStandardOutputText();
		csot.addObserver(csoeto);

		final List<String> textLinesStdOut = Arrays.asList("Line 1", "Line 2", "", "\tline 4");
		final ByteArrayInputStream processInputStreamOut = new ByteArrayInputStream(textLinesStdOut.stream().collect(
		        Collectors.joining("\n")).getBytes());

		final List<String> textLinesStdErr = Arrays.asList("Line 5", "Line 6", "", "\tline 8");
		final ByteArrayInputStream processInputStreamErr = new ByteArrayInputStream(textLinesStdErr.stream().collect(
		        Collectors.joining("\r\n")).getBytes());

		final ProcesslauncherLifecycle source = Mockito.mock(ProcesslauncherLifecycle.class);
		final Processlauncher launcher = Mockito.mock(Processlauncher.class);
		Mockito.when(source.getLauncher()).thenReturn(launcher);
		Mockito.when(launcher.getExecutableName()).thenReturn("some-exec");

		csot.stdOutStreamConsumer(processInputStreamOut, source);
		csot.stdErrStreamConsumer(processInputStreamErr, source);

		cdl.await(1, TimeUnit.SECONDS);

		assertEquals(textLinesStdOut.size() + textLinesStdErr.size(), capturedlines.size());
		assertTrue(capturedlines.stream().anyMatch(le -> le.getSource().equals(source)));

		final List<String> capturedlinesOut = capturedlines.stream().filter(le -> le.isStdErr() == false).map(
		        LineEntry::getLine).collect(Collectors.toList());
		final List<String> capturedlinesErr = capturedlines.stream().filter(LineEntry::isStdErr).map(LineEntry::getLine)
		        .collect(Collectors.toList());

		assertTrue(CollectionUtils.isEqualCollection(textLinesStdOut, capturedlinesOut));
		assertTrue(CollectionUtils.isEqualCollection(textLinesStdErr, capturedlinesErr));
	}

}
