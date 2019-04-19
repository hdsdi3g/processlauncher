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

import junit.framework.Assert;
import junit.framework.TestCase;

public class CommandLineTest extends TestCase {

	private final ExecutableFinder ef;
	private final CommandLine cmd;
	private final Parameters parametersSource;

	public CommandLineTest() throws IOException {
		parametersSource = new Parameters("-a");
		ExecutableFinderTest.patchTestExec();

		ef = new ExecutableFinder();
		cmd = new CommandLine("test-exec", parametersSource, ef);
		parametersSource.addParameters("-b");
	}

	public void testGetExecutableFinder() {
		Assert.assertEquals(ef, cmd.getExecutableFinder().get());
	}

	public void testGetExecutable() throws FileNotFoundException {
		Assert.assertEquals(ef.get("test-exec"), cmd.getExecutable());
	}

	public void testGetParameters() {
		Assert.assertNotSame(parametersSource, cmd.getParameters());
		Assert.assertFalse(parametersSource.toString().equals(cmd.getParameters().toString()));
	}

}
