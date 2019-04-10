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
package tv.hd3g.processlauncher.cmdline;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

public class ExecutableCommandLineTest extends TestCase {
	
	private ExecutableCommandLine ecl;
	private final ExecutableFinder ef;
	
	public ExecutableCommandLineTest() {
		ef = new ExecutableFinder();
	}
	
	@Override
	public void setUp() throws IOException {
		ecl = new ExecutableCommandLine("test-exec", Arrays.asList("-a", "-b"), ef);
	}
	
	public void testGetExecutableFinder() {
		Assert.assertEquals(ef, ecl.getExecutableFinder());
	}
	
	public void testGetExecutable() throws FileNotFoundException {
		Assert.assertEquals(ef.get("test-exec"), ecl.getExecutable());
	}
	
	public void testGetParameters() {
		Assert.assertTrue(Arrays.equals(Arrays.asList("-a", "-b").toArray(), ecl.getParameters().toArray()));
	}
}
