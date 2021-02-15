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
package tv.hd3g.processlauncher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;

class ExecTest {

	private final String execName;
	private final ExecutableFinder executableFinder;

	ExecTest() {
		execName = "java";
		executableFinder = new ExecutableFinder();
	}

	@Test
	void testGetVarsToInject() throws FileNotFoundException {
		final var exec = new Exec(execName, executableFinder);
		assertNotNull(exec.getVarsToInject());
		assertEquals(0, exec.getVarsToInject().size());
	}

	@Test
	void testIsRemoveParamsIfNoVarToInject() throws FileNotFoundException {
		final var exec = new Exec(execName, executableFinder);
		assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	@Test
	void testSetRemoveParamsIfNoVarToInject() throws FileNotFoundException {
		final var exec = new Exec(execName, executableFinder);
		assertFalse(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(true);
		assertTrue(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(false);
		assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	@Test
	void testGetParameters() throws FileNotFoundException {
		final var exec = new Exec(execName, executableFinder);
		assertNotNull(exec.getParameters());
		assertTrue(exec.getParameters().getParameters().isEmpty());
	}

	@Test
	void testGetParametersViaExecutableTool() throws FileNotFoundException {
		final var parameters = new Parameters("-p");
		final var exec = new Exec(new ExecutableTool() {

			@Override
			public Parameters getReadyToRunParameters() {
				return parameters;
			}

			@Override
			public String getExecutableName() {
				return execName;
			}

		}, executableFinder);

		assertNotNull(exec.getParameters());
		assertEquals(parameters, exec.getParameters());
		assertEquals(1, exec.getParameters().getParameters().size());
	}

	@Test
	void testGetReadyToRunParameters() throws FileNotFoundException {
		final var exec = new Exec(execName, executableFinder);
		assertNotNull(exec.getReadyToRunParameters());
		assertTrue(exec.getReadyToRunParameters().getParameters().isEmpty());
		assertNotSame(exec.getReadyToRunParameters(), exec.getParameters());
	}

	@Test
	void testRunWaitGetText() throws IOException {
		final var exec = new Exec(execName, executableFinder);
		exec.getParameters().addParameters("-version");

		final var callBack = new LinkedBlockingQueue<ProcesslauncherBuilder>();
		final Consumer<ProcesslauncherBuilder> beforeRun = pb -> {
			callBack.add(pb);
		};

		final var capturedStdOutErrTextRetention = exec.runWaitGetText(beforeRun);
		assertEquals(1, callBack.size());
		assertNotNull(callBack.poll());

		assertTrue(capturedStdOutErrTextRetention.getStdouterrLines(false).anyMatch(line -> line.contains(
		        "version")));
	}

	@Test
	void testRunWaitGetTextViaExecutableTool() throws IOException {
		final var callBack1 = new LinkedBlockingQueue<ProcesslauncherBuilder>();
		final var orderCallback = new LinkedBlockingQueue<Class<?>>();

		final var exec = new Exec(new ExecutableTool() {

			@Override
			public Parameters getReadyToRunParameters() {
				return new Parameters("-version");
			}

			@Override
			public String getExecutableName() {
				return execName;
			}

			@Override
			public void beforeRun(final ProcesslauncherBuilder processBuilder) {
				orderCallback.offer(ExecutableTool.class);
				callBack1.add(processBuilder);
			}

		}, executableFinder);

		final var callBack2 = new LinkedBlockingQueue<ProcesslauncherBuilder>();
		final Consumer<ProcesslauncherBuilder> beforeRun = pb -> {
			orderCallback.offer(ExecTest.class);
			callBack2.add(pb);
		};

		exec.runWaitGetText(beforeRun);

		assertEquals(1, callBack1.size());
		assertEquals(1, callBack2.size());
		assertEquals(callBack1.poll(), callBack2.poll());

		assertEquals(2, orderCallback.size());
		assertEquals(ExecutableTool.class, orderCallback.poll());
		assertEquals(ExecTest.class, orderCallback.poll());
	}

	@Test
	void testRunCatchVerbosedError() throws IOException {
		final var exec = new Exec(execName, executableFinder);
		exec.getParameters().addParameters("a");

		try {
			exec.runWaitGetText();
			Assertions.fail("Not thown an InvalidExecution");
		} catch (final InvalidExecution e) {
			final var stdErr = e.getStdErr();
			assertTrue(stdErr.contains("ClassNotFoundException"));
		}
	}

	@Test
	void testGetExecutableName() throws FileNotFoundException {
		assertEquals(execName, new Exec(execName, executableFinder).getExecutableName());
	}

	@Test
	void testGetExecutableFinder() throws FileNotFoundException {
		assertEquals(executableFinder, new Exec(execName, executableFinder).getExecutableFinder());
	}

	@Test
	void testGetExecutableFile() throws FileNotFoundException {
		assertEquals(executableFinder.get(execName), new Exec(execName, executableFinder).getExecutableFile());
	}

}
