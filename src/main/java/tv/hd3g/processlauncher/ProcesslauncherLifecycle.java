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
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.io.StdInInjection;

public class ProcesslauncherLifecycle implements ProcesslauncherLifecycleShortcutTraits {
	private static Logger log = LogManager.getLogger();

	private final Processlauncher launcher;
	private final Process process;
	private final Thread shutdownHook;
	private final String fullCommandLine;
	private final long startDate;

	private volatile boolean processWasKilled;
	private volatile boolean processWasStoppedBecauseTooLongTime;
	private volatile long endDate;
	private StdInInjection stdInInjection;

	ProcesslauncherLifecycle(final Processlauncher launcher) throws IOException {
		this.launcher = launcher;
		processWasKilled = false;
		processWasStoppedBecauseTooLongTime = false;
		fullCommandLine = launcher.getFullCommandLine();

		final ProcessBuilder pBuilder = launcher.getProcessBuilder();

		final Optional<ExternalProcessStartup> externalProcessStartup = launcher.getExternalProcessStartup();
		if (externalProcessStartup.isPresent()) {
			process = externalProcessStartup.get().startProcess(pBuilder);
			Objects.requireNonNull(process, "Can't manage null process");
		} else {
			process = pBuilder.start();
			log.info("Start process #" + process.pid() + " " + fullCommandLine);
		}
		startDate = System.currentTimeMillis();

		shutdownHook = new Thread(() -> {
			log.warn("Try to kill " + toString());
			killProcessTree(process);
		});
		shutdownHook.setDaemon(false);
		shutdownHook.setPriority(Thread.MAX_PRIORITY);
		shutdownHook.setName("ShutdownHook for " + toString());
		Runtime.getRuntime().addShutdownHook(shutdownHook);

		launcher.getExecutionTimeLimiter().ifPresent(etl -> {
			etl.addTimesUp(this, process);
		});

		final List<ExecutionCallbacker> executionCallbackers = launcher.getExecutionCallbackers();
		executionCallbackers.forEach(ec -> {
			ec.postStartupExecution(this);
		});

		launcher.getCaptureStandardOutput().ifPresent(cso -> {
			cso.stdOutStreamConsumer(process.getInputStream(), this);
			cso.stdErrStreamConsumer(process.getErrorStream(), this);
		});

		process.onExit().thenRun(() -> {
			endDate = System.currentTimeMillis();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			externalProcessStartup.ifPresent(eps -> eps.onEndProcess(this));
			executionCallbackers.forEach(ec -> {
				ec.onEndExecution(this);
			});
		});
	}

	public long getStartDate() {
		return getProcess().info().startInstant().flatMap(i -> Optional.of(i.toEpochMilli())).orElse(startDate);
	}

	@Override
	public String toString() {
		if (process.isAlive()) {
			return "Process #" + getPID() + " " + fullCommandLine + " ; since " + getUptime(TimeUnit.SECONDS) + " sec";
		} else {
			return "Exec " + getEndStatus() + " " + fullCommandLine;
		}
	}

	private static String processHandleToString(final ProcessHandle processHandle, final boolean verbose) {
		if (verbose) {
			return processHandle.info().command().orElse("<?>") + " #" + processHandle.pid() + " by " + processHandle.info().user().orElse("<?>") + " since " + processHandle.info().totalCpuDuration().orElse(Duration.ZERO).getSeconds() + " sec";
		} else {
			return processHandle.info().commandLine().orElse("<?>") + " #" + processHandle.pid();
		}
	}

	/**
	 * Blocking
	 */
	private void killProcessTree(final Process process) {
		if (process.isAlive() == false) {
			return;
		}

		log.debug("Internal kill " + toString());
		final List<ProcessHandle> cantKill = process.descendants().filter(processHandle -> {
			return processHandle.isAlive();
		}).filter(processHandle -> {
			if (log.isDebugEnabled()) {
				log.info("Close manually process " + processHandleToString(processHandle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Close manually process " + processHandleToString(processHandle, false));
			}
			return processHandle.destroy() == false;
		}).filter(processHandle -> {
			if (log.isDebugEnabled()) {
				log.info("Force to close process " + processHandleToString(processHandle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Force to close process " + processHandleToString(processHandle, false));
			}
			return processHandle.destroyForcibly() == false;
		}).collect(Collectors.toUnmodifiableList());

		if (process.isAlive()) {
			log.info("Close manually process " + processHandleToString(process.toHandle(), true));
			if (process.toHandle().destroy() == false) {
				log.info("Force to close process " + processHandleToString(process.toHandle(), true));
				if (process.toHandle().destroyForcibly() == false) {
					throw new RuntimeException("Can't close process " + processHandleToString(process.toHandle(), true));
				}
			}
		}

		if (cantKill.isEmpty() == false) {
			cantKill.forEach(processHandle -> {
				log.error("Can't force close process " + processHandleToString(processHandle, true));
			});
			throw new RuntimeException("Can't close process " + toString() + " for PID " + cantKill.stream().map(p -> p.pid()).map(pid -> String.valueOf(pid)).collect(Collectors.joining(", ")));
		}
	}

	public Processlauncher getLauncher() {
		return launcher;
	}

	@Override
	public Process getProcess() {
		return process;
	}

	@Override
	public EndStatus getEndStatus() {
		if (process.isAlive()) {
			return EndStatus.NOT_YET_DONE;
		} else if (processWasKilled) {
			return EndStatus.KILLED;
		} else if (processWasStoppedBecauseTooLongTime) {
			return EndStatus.TOO_LONG_EXECUTION_TIME;
		} else if (launcher.isExecCodeMustBeZero() && process.exitValue() != 0) {
			return EndStatus.DONE_WITH_ERROR;
		}
		return EndStatus.CORRECTLY_DONE;
	}

	public long getEndDate() {
		return endDate;
	}

	public long getUptime(final TimeUnit unit) {
		if (endDate > 0l) {
			return unit.convert(endDate - getStartDate(), TimeUnit.MILLISECONDS);
		}
		return unit.convert(System.currentTimeMillis() - getStartDate(), TimeUnit.MILLISECONDS);
	}

	public long getCPUDuration(final TimeUnit unit) {
		return unit.convert(process.info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), TimeUnit.MILLISECONDS);
	}

	public boolean isKilled() {
		return processWasKilled;
	}

	public boolean isTooLongTime() {
		return processWasStoppedBecauseTooLongTime;
	}

	ProcesslauncherLifecycle runningTakesTooLongTimeStopIt() {
		processWasStoppedBecauseTooLongTime = true;
		killProcessTree(process);
		return this;
	}

	public ProcesslauncherLifecycle kill() {
		if (process.isAlive() == false) {
			return this;
		}
		processWasKilled = true;
		killProcessTree(process);
		return this;
	}

	public ProcesslauncherLifecycle waitForEnd() {
		try {
			process.waitFor();
		} catch (final InterruptedException e) {
			throw new RuntimeException("Can't wait the end of " + fullCommandLine, e);
		}
		return this;
	}

	public ProcesslauncherLifecycle waitForEnd(final long timeout, final TimeUnit unit) {
		try {
			process.waitFor(timeout, unit);
		} catch (final InterruptedException e) {
			throw new RuntimeException("Can't wait the end of " + fullCommandLine, e);
		}
		return this;
	}

	/**
	 * waitForEnd and checks isCorrectlyDone
	 * @throws InvalidExecution
	 */
	public ProcesslauncherLifecycle checkExecution() {
		waitForEnd();
		if (isCorrectlyDone() == false) {
			throw new InvalidExecution(this);
		}
		return this;
	}

	public synchronized StdInInjection getStdInInjection() {
		if (stdInInjection == null) {
			stdInInjection = new StdInInjection(process.getOutputStream());
		}
		return stdInInjection;
	}

	String getFullCommandLine() {
		return fullCommandLine;
	}
}
