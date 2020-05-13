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
package tv.hd3g.processlauncher.cmdline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class CommandLineTest {

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

	@Test
	public void testGetExecutableFinder() {
		assertEquals(ef, cmd.getExecutableFinder().get());
	}

	@Test
	public void testGetExecutable() throws FileNotFoundException {
		assertEquals(ef.get("test-exec"), cmd.getExecutable());
	}

	@Test
	public void testGetParameters() {
		assertNotSame(parametersSource, cmd.getParameters());
		assertFalse(parametersSource.toString().equals(cmd.getParameters().toString()));
	}

}
