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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.tool.ExecutableTool;

public class ExecTest extends TestCase {

	private final String execName;
	private final ExecutableFinder executableFinder;

	public ExecTest() {
		execName = "java";
		executableFinder = new ExecutableFinder();
	}

	public void testGetVarsToInject() throws FileNotFoundException {
		final Exec exec = new Exec(execName, executableFinder);
		Assert.assertNotNull(exec.getVarsToInject());
		Assert.assertEquals(0, exec.getVarsToInject().size());
	}

	public void testIsRemoveParamsIfNoVarToInject() throws FileNotFoundException {
		final Exec exec = new Exec(execName, executableFinder);
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	public void testSetRemoveParamsIfNoVarToInject() throws FileNotFoundException {
		final Exec exec = new Exec(execName, executableFinder);
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(true);
		Assert.assertTrue(exec.isRemoveParamsIfNoVarToInject());
		exec.setRemoveParamsIfNoVarToInject(false);
		Assert.assertFalse(exec.isRemoveParamsIfNoVarToInject());
	}

	public void testGetParameters() throws FileNotFoundException {
		final Exec exec = new Exec(execName, executableFinder);
		Assert.assertNotNull(exec.getParameters());
		Assert.assertTrue(exec.getParameters().getParameters().isEmpty());
	}

	public void testGetParametersViaExecutableTool() throws FileNotFoundException {
		final Parameters parameters = new Parameters("-p");
		final Exec exec = new Exec(new ExecutableTool() {

			@Override
			public Parameters getReadyToRunParameters() {
				return parameters;
			}

			@Override
			public String getExecutableName() {
				return execName;
			}

		}, executableFinder);

		Assert.assertNotNull(exec.getParameters());
		Assert.assertEquals(parameters, exec.getParameters());
		Assert.assertEquals(1, exec.getParameters().getParameters().size());
	}

	public void testGetReadyToRunParameters() throws FileNotFoundException {
		final Exec exec = new Exec(execName, executableFinder);
		Assert.assertNotNull(exec.getReadyToRunParameters());
		Assert.assertTrue(exec.getReadyToRunParameters().getParameters().isEmpty());
		Assert.assertNotSame(exec.getReadyToRunParameters(), exec.getParameters());
	}

	public void testRunWaitGetText() throws IOException {
		final Exec exec = new Exec(execName, executableFinder);
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

	public void testRunWaitGetTextViaExecutableTool() throws IOException {
		final LinkedBlockingQueue<ProcesslauncherBuilder> callBack1 = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<Class<?>> orderCallback = new LinkedBlockingQueue<>();

		final Exec exec = new Exec(new ExecutableTool() {

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

		final LinkedBlockingQueue<ProcesslauncherBuilder> callBack2 = new LinkedBlockingQueue<>();
		final Consumer<ProcesslauncherBuilder> beforeRun = pb -> {
			orderCallback.offer(ExecTest.class);
			callBack2.add(pb);
		};

		exec.runWaitGetText(beforeRun);

		Assert.assertEquals(1, callBack1.size());
		Assert.assertEquals(1, callBack2.size());
		Assert.assertEquals(callBack1.poll(), callBack2.poll());

		Assert.assertEquals(2, orderCallback.size());
		Assert.assertEquals(ExecutableTool.class, orderCallback.poll());
		Assert.assertEquals(ExecTest.class, orderCallback.poll());
	}

	public void testRunCatchVerbosedError() throws IOException {
		final Exec exec = new Exec(execName, executableFinder);
		exec.getParameters().addParameters("a");

		try {
			exec.runWaitGetText();
			Assert.fail("Not thown an InvalidExecution");
		} catch (final InvalidExecution e) {
			final String stdErr = e.getStdErr();
			Assert.assertTrue(stdErr.contains("ClassNotFoundException"));
		}
	}

	public void testGetExecutableName() throws FileNotFoundException {
		Assert.assertEquals(execName, new Exec(execName, executableFinder).getExecutableName());
	}

	public void testGetExecutableFinder() throws FileNotFoundException {
		Assert.assertEquals(executableFinder, new Exec(execName, executableFinder).getExecutableFinder());
	}

	public void testGetExecutableFile() throws FileNotFoundException {
		Assert.assertEquals(executableFinder.get(execName), new Exec(execName, executableFinder).getExecutableFile());
	}

}
