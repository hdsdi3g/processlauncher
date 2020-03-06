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

import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.CapturedStreams;
import tv.hd3g.processlauncher.LineEntry;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class LineEntryTest extends TestCase {

	private final long date;
	private final String line;
	private final boolean stdErr;
	private final ProcesslauncherLifecycle source;

	public LineEntryTest() {
		line = "This is a test";
		stdErr = true;
		date = System.currentTimeMillis();

		source = Mockito.mock(ProcesslauncherLifecycle.class);
		Mockito.when(source.getStartDate()).thenReturn(date - 10000l);
	}

	private LineEntry lineEntry;

	@Override
	public void setUp() {
		lineEntry = new LineEntry(date, line, stdErr, source);
	}

	public void testGetTimeAgo() {
		Assert.assertEquals(10000l, lineEntry.getTimeAgo());
	}

	public void testGetDate() {
		Assert.assertEquals(date, lineEntry.getDate());
	}

	public void testGetLine() {
		Assert.assertEquals(line, lineEntry.getLine());
	}

	public void testGetSource() {
		Assert.assertEquals(source, lineEntry.getSource());
	}

	public void testIsStdErr() {
		Assert.assertEquals(stdErr, lineEntry.isStdErr());
	}

	public void testCanUseThis() {
		Assert.assertFalse(lineEntry.canUseThis(CapturedStreams.ONLY_STDOUT));
		Assert.assertTrue(lineEntry.canUseThis(CapturedStreams.ONLY_STDERR));
		Assert.assertTrue(lineEntry.canUseThis(CapturedStreams.BOTH_STDOUT_STDERR));

		lineEntry = new LineEntry(date, line, stdErr == false, source);

		Assert.assertTrue(lineEntry.canUseThis(CapturedStreams.ONLY_STDOUT));
		Assert.assertFalse(lineEntry.canUseThis(CapturedStreams.ONLY_STDERR));
		Assert.assertTrue(lineEntry.canUseThis(CapturedStreams.BOTH_STDOUT_STDERR));
	}
}
