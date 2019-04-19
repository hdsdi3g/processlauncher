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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

public class ExecTest extends TestCase {

	private final String execName;
	private final ExecutableFinder executableFinder;

	public ExecTest() {
		execName = "java";
		executableFinder = new ExecutableFinder();
	}

	private Exec exec;

	@Override
	protected void setUp() throws Exception {
		exec = new Exec(execName, executableFinder);
	}

	public void testGetVarsToInject() {
		Assert.assertNotNull(exec.getVarsToInject());
		Assert.assertEquals(0, exec.getVarsToInject().size());
	}

	public void testIsRemoveParamsIfNoVarToInject() {
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	public void testSetRemoveParamsIfNoVarToInject() {
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(true);
		Assert.assertTrue(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(false);
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	public void testGetParameters() {
		Assert.assertNotNull(exec.getParameters());
		Assert.assertTrue(exec.getParameters().getParameters().isEmpty());
	}

	public void testGetReadyToRunParameters() {
		Assert.assertNotNull(exec.getReadyToRunParameters());
		Assert.assertTrue(exec.getReadyToRunParameters().getParameters().isEmpty());
		Assert.assertNotSame(exec.getReadyToRunParameters(), exec.getParameters());
	}

	public void testRunWaitGetText() throws IOException {
		exec.getParameters().addParameters("-version");

		final LinkedBlockingQueue<ProcesslauncherBuilder> callBack = new LinkedBlockingQueue<>();
		final Consumer<ProcesslauncherBuilder> beforeRun = pb -> {
			callBack.add(pb);
		};

		final CapturedStdOutErrTextRetention capturedStdOutErrTextRetention = exec.runWaitGetText(beforeRun);
		Assert.assertEquals(1, callBack.size());
		Assert.assertNotNull(callBack.poll());

		Assert.assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false).anyMatch(line -> {
			return line.contains("version");
		}));
	}
}
