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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.EndStatus;
import tv.hd3g.processlauncher.Processlauncher;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CapturedStdOutErrToPrintStreamTest extends TestCase {

	private static final String execName = "launchedexec";
	private final Processlauncher launcher;

	public CapturedStdOutErrToPrintStreamTest() {
		launcher = Mockito.mock(Processlauncher.class);
		Mockito.when(launcher.getExecutableName()).thenReturn(execName);
	}

	private long pid;
	private CapturedStdOutErrToPrintStream capture;
	private PrintStream printStreamStdOut;
	private PrintStream printStreamStdErr;
	private ByteArrayOutputStream outStreamContent;
	private ByteArrayOutputStream errStreamContent;
	private ProcesslauncherLifecycle source;

	@Override
	protected void setUp() throws Exception {
		pid = Math.floorMod(Math.abs(new Random().nextLong()), 1000l);
		outStreamContent = new ByteArrayOutputStream();
		errStreamContent = new ByteArrayOutputStream();
		printStreamStdOut = new PrintStream(outStreamContent);
		printStreamStdErr = new PrintStream(errStreamContent);
		capture = new CapturedStdOutErrToPrintStream(printStreamStdOut, printStreamStdErr);
		source = Mockito.mock(ProcesslauncherLifecycle.class);
		Mockito.when(source.getLauncher()).thenReturn(launcher);
		Mockito.when(source.getPID()).thenReturn(Optional.of(pid));
	}

	public void testGetFilter() {
		Assert.assertTrue(capture.getFilter().isEmpty());
	}

	public void testSetFilter() {
		final Predicate<LineEntry> filter = l -> true;
		capture.setFilter(filter);
		Assert.assertEquals(filter, capture.getFilter().get());
	}

	private static final String outSep = CapturedStdOutErrToPrintStream.stdOutSeparator;
	private static final String errSep = CapturedStdOutErrToPrintStream.stdErrSeparator;

	public void testOnText() {
		final LineEntry le1out = new LineEntry(System.currentTimeMillis(), "content stdout", false, source);
		final LineEntry le1err = new LineEntry(System.currentTimeMillis(), "content stderr", true, source);

		capture.onText(le1out);
		capture.onText(le1err);

		Assert.assertEquals(execName + "#" + pid + outSep + le1out.getLine() + System.lineSeparator(), outStreamContent
		        .toString(StandardCharsets.UTF_8));
		Assert.assertEquals(execName + "#" + pid + errSep + le1err.getLine() + System.lineSeparator(), errStreamContent
		        .toString(StandardCharsets.UTF_8));
	}

	public void testOnFilteredText() {
		capture.setFilter(l -> l.isStdErr() == false);
		capture.onText(new LineEntry(System.currentTimeMillis(), "content", true, source));
		Assert.assertEquals(0, outStreamContent.size());
		Assert.assertEquals(0, errStreamContent.size());
	}

	public void testOnProcessCloseStreamExecOk() {
		Mockito.when(source.isCorrectlyDone()).thenReturn(true);
		Mockito.when(source.getEndStatus()).thenReturn(EndStatus.CORRECTLY_DONE);
		Mockito.when(source.getExitCode()).thenReturn(0);
		Mockito.when(source.getCPUDuration(null)).thenReturn(1l);
		Mockito.when(source.getUptime(null)).thenReturn(1l);

		capture.onProcessCloseStream(source, false, CapturedStreams.BOTH_STDOUT_STDERR);
		capture.onProcessCloseStream(source, true, CapturedStreams.BOTH_STDOUT_STDERR);
		Assert.assertEquals(0, outStreamContent.size());
		Assert.assertEquals(0, errStreamContent.size());

		capture.onProcessCloseStream(source, false, CapturedStreams.ONLY_STDOUT);
		Assert.assertTrue(outStreamContent.size() > 0);
		Assert.assertEquals(0, errStreamContent.size());
	}

	public void testOnProcessCloseStreamExecNok() {
		Mockito.when(source.isCorrectlyDone()).thenReturn(false);
		Mockito.when(source.getEndStatus()).thenReturn(EndStatus.DONE_WITH_ERROR);
		Mockito.when(source.getExitCode()).thenReturn(1);
		Mockito.when(source.getCPUDuration(null)).thenReturn(1l);
		Mockito.when(source.getUptime(null)).thenReturn(1l);

		capture.onProcessCloseStream(source, false, CapturedStreams.BOTH_STDOUT_STDERR);
		capture.onProcessCloseStream(source, true, CapturedStreams.BOTH_STDOUT_STDERR);
		Assert.assertTrue(outStreamContent.size() > 0);
		Assert.assertTrue(errStreamContent.size() > 0);

		capture.onProcessCloseStream(source, true, CapturedStreams.ONLY_STDERR);
		Assert.assertTrue(outStreamContent.size() > 0);
		Assert.assertTrue(errStreamContent.size() > 0);
	}

}
