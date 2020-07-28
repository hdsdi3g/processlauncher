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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tv.hd3g.processlauncher.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.Exec;
import tv.hd3g.processlauncher.InvalidExecution;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.tool.ToolRunner.RunningTool;

class ToolRunTest {

	private final String execName;
	private final ExecutableFinder executableFinder;

	public ToolRunTest() {
		execName = "java";
		executableFinder = new ExecutableFinder();
	}

	private Exec exec;

	@BeforeEach
	void setUp() throws Exception {
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

	@Test
	void testExecute() throws InterruptedException, ExecutionException, TimeoutException {
		final ToolRunner toolRun = new ToolRunner(executableFinder);

		final ExecutableTool executableTool = makeExecutableTool();

		final RunningTool<ExecutableTool> result = toolRun.execute(executableTool);
		assertEquals(executableTool, result.getExecutableToolSource());

		final CapturedStdOutErrTextRetention capturedStdOutErrTextRetention = result.getTextRetention();
		assertNotNull(capturedStdOutErrTextRetention);
		assertNotNull(result.getLifecyle());

		assertEquals(capturedStdOutErrTextRetention, result.checkExecutionGetText());
		assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false).anyMatch(line -> line.contains(
		        "version")));
	}

	@Test
	void checkExecutionGetText_withError() {
		exec.getParameters().clear().addBulkParameters("-thiswillneverexec");
		final var toolRun = new ToolRunner(executableFinder);
		final var executableTool = makeExecutableTool();
		final var result = toolRun.execute(executableTool);

		final var afterWait = result.waitForEnd();
		assertNotNull(afterWait);
		assertThrows(InvalidExecution.class, () -> afterWait.checkExecutionGetText());
	}

	@Test
	void waitForEnd_isReallydone() {
		final var toolRun = new ToolRunner(executableFinder);
		final var executableTool = makeExecutableTool();
		final var result = toolRun.execute(executableTool);
		assertTrue(result.waitForEnd().getLifecyle().isCorrectlyDone());
	}

	@Test
	void waitForEndAndCheckExecution_ok() {
		final var toolRun = new ToolRunner(executableFinder);
		final var executableTool = makeExecutableTool();
		final var result = toolRun.execute(executableTool).waitForEndAndCheckExecution();
		assertNotNull(result);
		assertTrue(result.getLifecyle().isCorrectlyDone());

		final CapturedStdOutErrTextRetention capturedStdOutErrTextRetention = result.getTextRetention();
		assertNotNull(capturedStdOutErrTextRetention);
		assertNotNull(result.getLifecyle());

		assertEquals(capturedStdOutErrTextRetention, result.checkExecutionGetText());
		assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false)
		        .anyMatch(line -> line.contains("version")));
	}

	@Test
	void waitForEndAndCheckExecution_error() {
		exec.getParameters().clear().addBulkParameters("-thiswillneverexec");
		final var toolRun = new ToolRunner(executableFinder);
		final var executableTool = makeExecutableTool();
		final var result = toolRun.execute(executableTool);
		assertThrows(InvalidExecution.class, () -> result.waitForEndAndCheckExecution());
	}

	@Test
	void getExecutableFinder() {
		final var toolRun = new ToolRunner(executableFinder);
		assertEquals(executableFinder, toolRun.getExecutableFinder());
	}
}
