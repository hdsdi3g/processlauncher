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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import junit.framework.Assert;
import junit.framework.TestCase;
import tv.hd3g.processlauncher.cmdline.CommandLine;
import tv.hd3g.processlauncher.cmdline.ExecutableFinder;
import tv.hd3g.processlauncher.cmdline.Parameters;
import tv.hd3g.processlauncher.demo.DemoExecExitCode;
import tv.hd3g.processlauncher.demo.DemoExecIOText;
import tv.hd3g.processlauncher.demo.DemoExecInteractive;
import tv.hd3g.processlauncher.demo.DemoExecLongSleep;
import tv.hd3g.processlauncher.demo.DemoExecShortSleep;
import tv.hd3g.processlauncher.demo.DemoExecSimple;
import tv.hd3g.processlauncher.demo.DemoExecStdinInjection;
import tv.hd3g.processlauncher.demo.DemoExecSubProcess;
import tv.hd3g.processlauncher.demo.DemoExecWorkingdir;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextInteractive;
import tv.hd3g.processlauncher.io.CapturedStdOutErrTextRetention;
import tv.hd3g.processlauncher.io.CapturedStreams;
import tv.hd3g.processlauncher.io.LineEntry;

public class ProcesslauncherLifecycleITTest extends TestCase {

	private final ExecutableFinder executableFinder;
	private final ScheduledThreadPoolExecutor scheduledThreadPool;

	public ProcesslauncherLifecycleITTest() {
		executableFinder = new ExecutableFinder();
		scheduledThreadPool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
	}

	public ProcesslauncherBuilder prepareBuilder(final Class<?> execClass) throws IOException {
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), execClass.getName());
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		return new ProcesslauncherBuilder(cmd);
	}

	private CapturedStdOutErrTextRetention textRetention;

	@Override
	protected void setUp() throws Exception {
		textRetention = new CapturedStdOutErrTextRetention();
	}

	private ProcesslauncherLifecycle captureTextAndStart(final ProcesslauncherBuilder pb) throws IOException {
		pb.getSetCaptureStandardOutputAsOutputText(CapturedStreams.BOTH_STDOUT_STDERR).getObservers()
		        .add(textRetention);
		return pb.start();
	}

	public void testSimpleExec() throws IOException {
		final ProcesslauncherLifecycle result = captureTextAndStart(prepareBuilder(DemoExecSimple.class)).waitForEnd();
		Assert.assertEquals(DemoExecSimple.expected, textRetention.getStdouterr(true, ""));
		Assert.assertEquals(0, (int) result.getExitCode());
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}

	public void testWorkingDirectory() throws IOException, InterruptedException, ExecutionException {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecWorkingdir.class);
		final File wd = new File(System.getProperty("user.dir")).getCanonicalFile();
		ept.setWorkingDirectory(wd);

		Assert.assertEquals(wd, ept.getWorkingDirectory());

		final ProcesslauncherLifecycle result = captureTextAndStart(ept).waitForEnd();
		Assert.assertEquals(wd, result.getLauncher().getProcessBuilder().directory());

		Assert.assertEquals(wd.getPath(), textRetention.getStdouterr(true, ""));
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
	}

	public void testExecutionCallback() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecSimple.class);

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
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"), DemoExecIOText.class
		        .getName());
		parameters.addParameters(DemoExecIOText.expectedIn);
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		final ProcesslauncherBuilder ept = new ProcesslauncherBuilder(cmd);

		ept.setExecCodeMustBeZero(false);
		ept.setEnvironmentVar(DemoExecIOText.ENV_KEY, DemoExecIOText.ENV_VALUE);

		final ProcesslauncherLifecycle p = captureTextAndStart(ept).waitForEnd();

		Assert.assertEquals(DemoExecIOText.expectedOut, textRetention.getStdout(false, ""));
		Assert.assertEquals(DemoExecIOText.expectedErr, textRetention.getStderr(false, ""));
		Assert.assertEquals(DemoExecIOText.exitOk, (int) p.getExitCode());
		Assert.assertEquals(EndStatus.CORRECTLY_DONE, p.getEndStatus());

		Assert.assertEquals(DemoExecIOText.exitOk, p.getExitCode().intValue());
	}

	public void testMaxExecTime() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecLongSleep.class);

		ept.setExecutionTimeLimiter(DemoExecLongSleep.MAX_DURATION, TimeUnit.MILLISECONDS, scheduledThreadPool);

		final long startTime = System.currentTimeMillis();
		final ProcesslauncherLifecycle result = ept.start().waitForEnd();

		final long duration = System.currentTimeMillis() - startTime;

		MatcherAssert.assertThat(duration, Matchers.lessThan(DemoExecLongSleep.MAX_DURATION
		                                                     + 300)); /** 300 is a "startup time bonus" */
		Assert.assertEquals(EndStatus.TOO_LONG_EXECUTION_TIME, result.getEndStatus());

		Assert.assertTrue(result.isTooLongTime());
		Assert.assertFalse(result.isCorrectlyDone());
		Assert.assertFalse(result.isKilled());
		Assert.assertFalse(result.isRunning());
	}

	public void testKill() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecLongSleep.class);

		final long startTime = System.currentTimeMillis();
		final ProcesslauncherLifecycle result = ept.start();

		scheduledThreadPool.schedule(() -> {
			result.kill();
		}, DemoExecLongSleep.MAX_DURATION, TimeUnit.MILLISECONDS);

		result.waitForEnd();

		final long duration = System.currentTimeMillis() - startTime;

		MatcherAssert.assertThat(duration, Matchers.lessThan(DemoExecLongSleep.MAX_DURATION
		                                                     + 300)); /** 300 is a "startup time bonus" */
		Assert.assertEquals(EndStatus.KILLED, result.getEndStatus());

		Assert.assertFalse(result.isTooLongTime());
		Assert.assertFalse(result.isCorrectlyDone());
		Assert.assertTrue(result.isKilled());
		Assert.assertFalse(result.isRunning());
	}

	public void testKillSubProcess() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecSubProcess.class);

		final long startTime = System.currentTimeMillis();
		final ProcesslauncherLifecycle result = ept.start();

		scheduledThreadPool.schedule(() -> {
			result.kill();
		}, DemoExecLongSleep.MAX_DURATION * 4, TimeUnit.MILLISECONDS);

		Assert.assertTrue(result.isRunning());
		Thread.sleep(DemoExecLongSleep.MAX_DURATION);
		/**
		 * flacky on linux
		 * Assert.assertEquals(1, result.getProcess().children().count());
		 * Assert.assertEquals(1, result.getProcess().descendants().count());
		 */

		result.waitForEnd();

		final long duration = System.currentTimeMillis() - startTime;

		MatcherAssert.assertThat(duration, Matchers.lessThan(DemoExecLongSleep.MAX_DURATION * 4 * 2));
		Assert.assertEquals(EndStatus.KILLED, result.getEndStatus());

		Assert.assertFalse(result.isTooLongTime());
		Assert.assertFalse(result.isCorrectlyDone());
		Assert.assertTrue(result.isKilled());
		Assert.assertFalse(result.isRunning());

		Assert.assertEquals(0, result.getProcess().descendants().count());
	}

	public void testTimesAndProcessProps() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecSubProcess.class);

		ept.setExecutionTimeLimiter(DemoExecLongSleep.MAX_DURATION, TimeUnit.MILLISECONDS, scheduledThreadPool);

		final long startTime = System.currentTimeMillis();
		final ProcesslauncherLifecycle result = ept.start().waitForEnd();

		final long duration = System.currentTimeMillis() - startTime;
		MatcherAssert.assertThat(duration, Matchers.greaterThanOrEqualTo(result.getUptime(TimeUnit.MILLISECONDS)));
	}

	public void testInteractiveHandler() throws Exception {
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"),
		        DemoExecInteractive.class.getName());
		parameters.addParameters("foo");
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		final ProcesslauncherBuilder ept = new ProcesslauncherBuilder(cmd);

		ept.setExecutionTimeLimiter(500, TimeUnit.MILLISECONDS, new ScheduledThreadPoolExecutor(1));

		final LinkedBlockingQueue<Exception> errors = new LinkedBlockingQueue<>();

		final Function<LineEntry, String> interactive = lineEntry -> {
			final String line = lineEntry.getLine();
			if (lineEntry.isStdErr()) {
				System.err.println("Process say: " + line);
				errors.add(new Exception("isStdErr is true"));
				return DemoExecInteractive.QUIT;
			} else if (line.equals("FOO")) {
				return "bar";
			} else if (line.equals("foo")) {
				errors.add(new Exception("foo is in lowercase"));
				return DemoExecInteractive.QUIT;
			} else if (line.equals("BAR")) {
				return DemoExecInteractive.QUIT;
			} else if (line.equals("bar")) {
				errors.add(new Exception("bar is in lowercase"));
				return DemoExecInteractive.QUIT;
			} else {
				errors.add(new Exception("Invalid line " + line));
				return null;
			}
		};

		final AtomicInteger onProcessClosedStreamCountOut = new AtomicInteger(0);
		final AtomicInteger onProcessClosedStreamCountErr = new AtomicInteger(0);
		final BiConsumer<ProcesslauncherLifecycle, Boolean> onProcessClosedStream = (source, isStdErr) -> {
			if (isStdErr) {
				onProcessClosedStreamCountErr.incrementAndGet();
			} else {
				onProcessClosedStreamCountOut.incrementAndGet();
			}
		};
		ept.getSetCaptureStandardOutputAsOutputText(CapturedStreams.BOTH_STDOUT_STDERR).getObservers()
		        .add(new CapturedStdOutErrTextInteractive(interactive, onProcessClosedStream));

		final ProcesslauncherLifecycle result = ept.start().waitForEnd();

		if (errors.isEmpty() == false) {
			errors.forEach(e -> {
				e.printStackTrace();
			});
			Assert.fail();
		}

		Assert.assertEquals(EndStatus.CORRECTLY_DONE, result.getEndStatus());
		Assert.assertTrue(result.isCorrectlyDone());

		for (int i = 0; i < 100; i++) {
			Thread.sleep(10);
			if (onProcessClosedStreamCountOut.get() == 1) {
				break;
			}
		}
		Assert.assertEquals(1, onProcessClosedStreamCountOut.get());
		Assert.assertEquals(1, onProcessClosedStreamCountErr.get());
	}

	public void testWaitForEnd() throws Exception {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecShortSleep.class);
		Assert.assertTrue(ept.start().waitForEnd(500, TimeUnit.MILLISECONDS).isCorrectlyDone());
	}

	public void testToString() throws IOException {
		Assert.assertNotNull(prepareBuilder(DemoExecSimple.class).start().toString());
	}

	public void testCheckExecutionOk() throws InterruptedException, ExecutionException, IOException {
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"),
		        DemoExecExitCode.class.getName());
		parameters.addParameters("0");
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		final ProcesslauncherBuilder ept1 = new ProcesslauncherBuilder(cmd);

		ept1.start().waitForEnd().checkExecution();
	}

	public void testCheckExecutionError() throws InterruptedException, ExecutionException, IOException {
		final Parameters parameters = new Parameters("-cp", System.getProperty("java.class.path"),
		        DemoExecExitCode.class.getName());
		parameters.addParameters("1");
		final CommandLine cmd = new CommandLine("java", parameters, executableFinder);
		final ProcesslauncherLifecycle result = new ProcesslauncherBuilder(cmd).start();

		try {
			result.waitForEnd().checkExecution();
			Assert.fail("Missing exception");
		} catch (final Exception e) {
			Assert.assertEquals(1, result.getExitCode().intValue());
		}
	}

	public void testStdInInjection() throws IOException, InterruptedException, ExecutionException {
		final ProcesslauncherBuilder ept = prepareBuilder(DemoExecStdinInjection.class);
		ept.setExecutionTimeLimiter(500, TimeUnit.MILLISECONDS, scheduledThreadPool);

		final ProcesslauncherLifecycle result = ept.start();
		result.getStdInInjection().println(DemoExecStdinInjection.QUIT);
		result.waitForEnd();
		Assert.assertTrue(result.isCorrectlyDone());
	}

}
