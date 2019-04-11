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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.mockito.Mockito;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.ProcesslauncherLifecycle;

public class CapturedStdOutErrTextRetentionTest extends TestCase {
	
	private final ProcesslauncherLifecycle source;
	private final CapturedStdOutErrTextRetention capText;
	
	public CapturedStdOutErrTextRetentionTest() {
		source = Mockito.mock(ProcesslauncherLifecycle.class);
		capText = new CapturedStdOutErrTextRetention(CapturedStreams.BOTH_STDOUT_STDERR);
		capText.onText(createLineEntry("Out 0", false));
		capText.onText(createLineEntry("Err 0", true));
		capText.onText(createLineEntry("Out 1", false));
		capText.onText(createLineEntry("Err 1", true));
		capText.onText(createLineEntry("", false));
		capText.onText(createLineEntry("", true));
		capText.onText(createLineEntry("Out 3", false));
		capText.onText(createLineEntry("Err 3", true));
	}

	private LineEntry createLineEntry(final String line, final boolean stdErr) {
		return new LineEntry(System.currentTimeMillis(), line, stdErr, source);
	}
	
	public void testGetStdoutLines() {
		List<String> lines = Arrays.asList("Out 0", "Out 1", "", "Out 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStdoutLines(true).collect(Collectors.toList())));

		lines = Arrays.asList("Out 0", "Out 1", "Out 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStdoutLines(false).collect(Collectors.toList())));
	}
	
	public void testGetStderrLines() {
		List<String> lines = Arrays.asList("Err 0", "Err 1", "", "Err 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStderrLines(true).collect(Collectors.toList())));

		lines = Arrays.asList("Err 0", "Err 1", "Err 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStderrLines(false).collect(Collectors.toList())));
	}
	
	public void testGetStdouterrLines() {
		List<String> lines = Arrays.asList("Out 0", "Err 0", "Out 1", "Err 1", "", "", "Out 3", "Err 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStdouterrLines(true).collect(Collectors.toList())));

		lines = Arrays.asList("Out 0", "Err 0", "Out 1", "Err 1", "Out 3", "Err 3");
		Assert.assertTrue(CollectionUtils.isEqualCollection(lines, capText.getStdouterrLines(false).collect(Collectors.toList())));
	}
	
	public void testGetStdout() {
		Assert.assertEquals("Out 0,Out 1,,Out 3", capText.getStdout(true, ","));
		Assert.assertEquals("Out 0,Out 1,Out 3", capText.getStdout(false, ","));
	}
	
	public void testGetStderr() {
		Assert.assertEquals("Err 0,Err 1,,Err 3", capText.getStderr(true, ","));
		Assert.assertEquals("Err 0,Err 1,Err 3", capText.getStderr(false, ","));
	}
	
	public void testGetStdouterr() {
		Assert.assertEquals("Out 0,Err 0,Out 1,Err 1,,,Out 3,Err 3", capText.getStdouterr(true, ","));
		Assert.assertEquals("Out 0,Err 0,Out 1,Err 1,Out 3,Err 3", capText.getStdouterr(false, ","));
	}
}
