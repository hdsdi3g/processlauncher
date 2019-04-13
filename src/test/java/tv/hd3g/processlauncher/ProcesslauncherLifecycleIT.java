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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.demo.Test1;
import tv.hd3g.processlauncher.demo.Test2;
import tv.hd3g.processlauncher.demo.Test3;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;

public class ProcesslauncherLifecycleIT extends TestCase {

	private final ExecutableFinder executableFinder;
	private final ExecutorService outStreamWatcher;

	public ProcesslauncherLifecycleIT() {
		executableFinder = new ExecutableFinder();
		outStreamWatcher = Executors.newCachedThreadPool();
	}

	public ProcesslauncherBuilder createExec(final Class<?> exec_class) throws IOException {// TODO rename
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), exec_class.getName());
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		return new ProcesslauncherBuilder(cmd);
	}

	private CapturedStdOutErrTextRetention textRetention;

	@Override
	protected void setUp() throws Exception {
		textRetention = new CapturedStdOutErrTextRetention();
	}

	private ProcesslauncherLifecycle captureTextAndStart(final ProcesslauncherBuilder pb) throws IOException {
		return pb.setCaptureStandardOutput(outStreamWatcher, textRetention).start();
	}

	public void testSimpleExec() throws IOException {
		final ProcesslauncherLifecycle result = captureTextAndStart(createExec(Test1.class)).waitForEnd();
		Assert.assertEquals(Test1.expected, textRetention.getStdouterr(true, ""));
		Assert.assertEquals(0, (int) result.getExitCode());
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}

	public void testWorkingDirectory() throws IOException, InterruptedException, ExecutionException {
		final ProcesslauncherBuilder ept = createExec(Test2.class);
		final File wd = new File(System.getProperty("user.dir")).getCanonicalFile();
		ept.setWorkingDirectory(wd);

		Assert.assertEquals(wd, ept.getWorkingDirectory());

		final ProcesslauncherLifecycle result = captureTextAndStart(ept).waitForEnd();
		Assert.assertEquals(wd, result.getLauncher().getProcessBuilder().directory());

		Assert.assertEquals(wd.getPath(), textRetention.getStdouterr(true, ""));
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}

	public void testExecutionCallback() throws Exception {
		final ProcesslauncherBuilder ept = createExec(Test1.class);

		final LinkedBlockingQueue<ProcesslauncherLifecycle> onEndExecutions = new LinkedBlockingQueue<>();
		final LinkedBlockingQueue<ProcesslauncherLifecycle> onPostStartupExecution = new LinkedBlockingQueue<>();
		final AtomicBoolean isAlive = new AtomicBoolean(false);
		ept.addExecutionCallbacker(new ExecutionCallbacker() {
			@Override
			public void onEndExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
				onEndExecutions.add(processlauncherLifecycle);
			}

			@Override
			public void postStartupExecution(final ProcesslauncherLifecycle processlauncherLifecycle) {
				isAlive.set(true);
				onPostStartupExecution.add(processlauncherLifecycle);
			}
		});

		final ProcesslauncherLifecycle p = ept.start().waitForEnd();
		Assert.assertEquals(p, onEndExecutions.poll(500, TimeUnit.MILLISECONDS));
		Assert.assertEquals(p, onPostStartupExecution.poll(500, TimeUnit.MILLISECONDS));
		Assert.assertTrue(isAlive.get());
	}

	public void testResultValues() throws Exception {
		final long start_date = System.currentTimeMillis() - 1;

		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), Test3.class.getName());
		parameters.addParameters(Test3.expected_in);
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		final ProcesslauncherBuilder ept = new ProcesslauncherBuilder(cmd);

		ept.setExecCodeMustBeZero(false);
		ept.setEnvironmentVar(Test3.ENV_KEY, Test3.ENV_VALUE);

		final ProcesslauncherLifecycle p = captureTextAndStart(ept).waitForEnd();

		Assert.assertEquals(Test3.expected_out, textRetention.getStdout(false, ""));
		Assert.assertEquals(Test3.expected_err, textRetention.getStderr(false, ""));
		Assert.assertEquals(Test3.exit_ok, (int) p.getExitCode());
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, p.getEndStatus());

		Assert.assertTrue(p.getPID().get() > 0);
		Assert.assertTrue(p.getUserExec().get().endsWith(System.getProperty("user.name")));

		Assert.assertEquals(Test3.exit_ok, p.getExitCode().intValue());
		Assert.assertEquals((long) p.getPID().get(), p.getProcess().pid());
		Assert.assertFalse(p.getProcess().isAlive());

		Assert.assertTrue(p.getStartDate() > start_date);
		Assert.assertTrue(p.getStartDate() < System.currentTimeMillis());
	}

}
