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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.Processlauncher;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CaptureStandardOutputTextTest extends TestCase {

	public void test() throws InterruptedException {
		final List<LineEntry> capturedlines = new ArrayList<>();
		final CapturedStdOutErrText csoeto = lineEntry -> {
			capturedlines.add(lineEntry);
		};

		final CaptureStandardOutputText csot = new CaptureStandardOutputText();
		csot.getObservers().add(csoeto);

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

		final var tOut = csot.stdOutStreamConsumer(processInputStreamOut, source);
		final var tErr = csot.stdErrStreamConsumer(processInputStreamErr, source);

		tOut.join(1000);
		tErr.join(1000);

		Assert.assertEquals(textLinesStdOut.size() + textLinesStdErr.size(), capturedlines.size());
		Assert.assertTrue(capturedlines.stream().anyMatch(le -> le.getSource().equals(source)));

		final List<String> capturedlinesOut = capturedlines.stream().filter(le -> le.isStdErr() == false).map(le -> le
		        .getLine()).collect(Collectors.toList());
		final List<String> capturedlinesErr = capturedlines.stream().filter(le -> le.isStdErr()).map(le -> le.getLine())
		        .collect(Collectors.toList());

		Assert.assertTrue(CollectionUtils.isEqualCollection(textLinesStdOut, capturedlinesOut));
		Assert.assertTrue(CollectionUtils.isEqualCollection(textLinesStdErr, capturedlinesErr));
	}

}
