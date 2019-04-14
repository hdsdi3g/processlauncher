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
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.demo.DemoExecEmpty;

public class ProcesslauncherLifecycleTest extends TestCase {

	private final long beforeStartDate;
	private final long afterEndDate;
	private final Processlauncher launcher;
	private final ProcesslauncherBuilder processlauncherBuilder;
	private final ProcesslauncherLifecycle p;

	public ProcesslauncherLifecycleTest() throws IOException {
		beforeStartDate = System.currentTimeMillis() - 100;
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), DemoExecEmpty.class.getName());
		final CommandLine cmd = new CommandLine("java", parameters, new ExecutableFinder());
		processlauncherBuilder = new ProcesslauncherBuilder(cmd);
		launcher = new Processlauncher(processlauncherBuilder);
		p = new ProcesslauncherLifecycle(launcher).waitForEnd(500, TimeUnit.MILLISECONDS);
		afterEndDate = System.currentTimeMillis() + 100;
	}

	public void testStatues() {
		Assert.assertEquals(launcher, p.getLauncher());
		Assert.assertNotNull(p.getProcess());
		Assert.assertFalse(p.getProcess().isAlive());
		Assert.assertFalse(p.isRunning());
		Assert.assertFalse(p.isKilled());
		Assert.assertFalse(p.isTooLongTime());
		Assert.assertEquals(0, p.getExitCode().intValue());
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, p.getEndStatus());
		Assert.assertTrue(p.isCorrectlyDone());
		Assert.assertTrue(beforeStartDate <= p.getStartDate());
		Assert.assertTrue(afterEndDate >= p.getEndDate());
		Assert.assertTrue(p.getUptime(TimeUnit.NANOSECONDS) > 0l);
		Assert.assertTrue(p.getCPUDuration(TimeUnit.MILLISECONDS) > 0l);
		Assert.assertTrue(p.getUserExec().isPresent());
		Assert.assertTrue(p.getPID().isPresent());
		Assert.assertTrue(p.getPID().get() > 0l);

		/**
		 * Should do nothing
		 */
		p.kill();
		p.waitForEnd();
		p.checkExecution();
	}
}
