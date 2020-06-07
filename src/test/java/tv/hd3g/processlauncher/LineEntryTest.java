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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LineEntryTest {

	private final long date;
	private final String line;
	private final boolean stdErr;
	private final ProcesslauncherLifecycle source;

	LineEntryTest() {
		line = "This is a test";
		stdErr = true;
		date = System.currentTimeMillis();

		source = Mockito.mock(ProcesslauncherLifecycle.class);
		Mockito.when(source.getStartDate()).thenReturn(date - 10000L);
	}

	private LineEntry lineEntry;

	@BeforeEach
	void setUp() {
		lineEntry = new LineEntry(date, line, stdErr, source);
	}

	@Test
	void testGetTimeAgo() {
		assertEquals(10000L, lineEntry.getTimeAgo());
	}

	@Test
	void testGetDate() {
		assertEquals(date, lineEntry.getDate());
	}

	@Test
	void testGetLine() {
		assertEquals(line, lineEntry.getLine());
	}

	@Test
	void testGetSource() {
		assertEquals(source, lineEntry.getSource());
	}

	@Test
	void testIsStdErr() {
		assertEquals(stdErr, lineEntry.isStdErr());
	}

	@Test
	void testCanUseThis() {
		assertFalse(lineEntry.canUseThis(CapturedStreams.ONLY_STDOUT));
		assertTrue(lineEntry.canUseThis(CapturedStreams.ONLY_STDERR));
		assertTrue(lineEntry.canUseThis(CapturedStreams.BOTH_STDOUT_STDERR));

		lineEntry = new LineEntry(date, line, stdErr == false, source);

		assertTrue(lineEntry.canUseThis(CapturedStreams.ONLY_STDOUT));
		assertFalse(lineEntry.canUseThis(CapturedStreams.ONLY_STDERR));
		assertTrue(lineEntry.canUseThis(CapturedStreams.BOTH_STDOUT_STDERR));
	}
}
