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
package tv.hd3g.processlauncher;

import java.io.IOException;

import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

public class ProcesslauncherLifecycleIT extends TestCase {

	private final ExecutableFinder executableFinder;
	
	public ProcesslauncherLifecycleIT() {
		executableFinder = new ExecutableFinder();
	}
	
	public ProcesslauncherBuilder createExec(final Class<?> exec_class) throws IOException {// TODO rename
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), exec_class.getName());
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		return new ProcesslauncherBuilder(cmd);
	}

	public void testStatues() {
		// TODO do IT
	}
}
