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
package tv.hd3g.processlauncher.tool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

	private ExecutableTool makeExecutableTool() {
		return new ExecutableTool() {

			@Override
			public String getExecutableName() {
				return execName;
			}

			@Override
			public Parameters getReadyToRunParameters() {
				return exec.getReadyToRunParameters();
			}
		};
	}

	public void testExecute() throws InterruptedException, ExecutionException, TimeoutException {
		final ToolRunner toolRun = new ToolRunner(executableFinder, 1);

		final ExecutableTool executableTool = makeExecutableTool();

		final CompletableFuture<RunningTool<ExecutableTool>> cfResult = toolRun.execute(executableTool);
		Assert.assertNotNull(cfResult);
		final RunningTool<ExecutableTool> result = cfResult.get(10, TimeUnit.SECONDS);
		Assert.assertEquals(executableTool, result.getExecutableToolSource());

		final CapturedStdOutErrTextRetention capturedStdOutErrTextRetention = result.getTextRetention();
		Assert.assertNotNull(capturedStdOutErrTextRetention);
		Assert.assertNotNull(result.getLifecyle());

		Assert.assertEquals(capturedStdOutErrTextRetention, result.checkExecutionGetText());
		Assert.assertEquals(result, result.waitForEnd(r -> r.run()).get());

		Assert.assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false).anyMatch(line -> {
			return line.contains("version");
		}));
	}

	public void testMassiveParallelExecute() throws InterruptedException, ExecutionException, TimeoutException {
		final var cpuCount = Runtime.getRuntime().availableProcessors();
		final ToolRunner toolRun = new ToolRunner(executableFinder, cpuCount);
		if (cpuCount == 1) {
			return;
		}

		final var startList = IntStream.range(0, cpuCount * 20).parallel().mapToObj(i -> {
			return toolRun.execute(makeExecutableTool());
		}).collect(Collectors.toUnmodifiableList());

		startList.stream().map(cfResult -> {
			try {
				return cfResult.get(2, TimeUnit.MINUTES).waitForEnd(r -> r.run()).get();
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new IllegalArgumentException(e);
			}
		}).forEach(result -> {
			final var content = result.getTextRetention().getStdouterrLines(false)
			        .collect(Collectors.toUnmodifiableList());
			Assert.assertTrue("Fail content: " + content, content.stream().anyMatch(line -> {
				return line.contains("version");
			}));
		});
	}

}
