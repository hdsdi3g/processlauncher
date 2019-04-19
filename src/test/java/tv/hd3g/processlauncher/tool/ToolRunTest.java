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
package tv.hd3g.processlauncher.tool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

public class ToolRunTest extends TestCase {

	private final String execName;
	private final ExecutableFinder executableFinder;

	public ToolRunTest() {
		execName = "java";
		executableFinder = new ExecutableFinder();
	}

	private Exec exec;

	@Override
	protected void setUp() throws Exception {
		exec = new Exec(execName, executableFinder);
		exec.getParameters().addParameters("-version");
	}

	public void testExecute() throws InterruptedException, ExecutionException, TimeoutException {
		final ToolRun toolRun = new ToolRun(executableFinder, 1);

		final ExecutableTool executableTool = new ExecutableTool() {

			@Override
			public String getExecutableName() {
				return execName;
			}

			@Override
			public Parameters getParameters() {
				return exec.getParameters();
			}
		};

		final CompletableFuture<RunningTool<ExecutableTool>> cfResult = toolRun.execute(executableTool);
		Assert.assertNotNull(cfResult);
		final RunningTool<ExecutableTool> result = cfResult.get(10, TimeUnit.SECONDS);
		Assert.assertEquals(executableTool, result.getExecutableToolSource());

		final CapturedStdOutErrTextRetention capturedStdOutErrTextRetention = result.getTextRetention();
		Assert.assertNotNull(capturedStdOutErrTextRetention);
		Assert.assertNotNull(result.getLifecyle());

		Assert.assertEquals(capturedStdOutErrTextRetention, result.checkExecutionGetText(r -> r.run()).get());
		Assert.assertEquals(result, result.waitForEnd(r -> r.run()).get());

		Assert.assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false).anyMatch(line -> {
			return line.contains("version");
		}));
	}
}
