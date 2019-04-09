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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tv.hd3g.processlauncher.io.StdInInjection;

public class ProcesslauncherLifecycle {
	private static Logger log = LogManager.getLogger();
	
	private final Processlauncher launcher;
	private final Process process;
	private final Thread shutdown_hook;
	private final long start_date;
	private final String fullCommandLine;

	private volatile boolean process_was_killed;
	private volatile boolean process_was_stopped_because_too_long_time;
	private StdInInjection std_in_injection;

	ProcesslauncherLifecycle(final Processlauncher launcher) throws IOException {
		this.launcher = launcher;
		process_was_killed = false;
		process_was_stopped_because_too_long_time = false;
		fullCommandLine = launcher.getFullCommandLine();

		process = launcher.getProcessBuilder().start();
		start_date = System.currentTimeMillis();

		/*if (log.isTraceEnabled()) {
			String env = "";
			if (environment.isEmpty() == false) {
				env = " ; with environment: " + environment;
			}
			String met = "";
			if (max_exec_time > 0) {
				met = " and max exec time: " + getMaxExecTime(TimeUnit.SECONDS) + " sec";
			}
			log.info("Start process #" + process.pid() + " by " + process.info().user().orElse("(?)") + " " + getCommandline() + env + met);
		} else {*/
		log.info("Start process #" + process.pid() + " " + fullCommandLine);

		shutdown_hook = new Thread(() -> {
			log.warn("Try to kill " + toString());
			killProcessTree(process);
		});
		shutdown_hook.setDaemon(false);
		shutdown_hook.setPriority(Thread.MAX_PRIORITY);
		shutdown_hook.setName("ShutdownHook for " + toString());
		Runtime.getRuntime().addShutdownHook(shutdown_hook);

		launcher.getExecutionTimeLimiter().ifPresent(etl -> {
			etl.addTimesUp(this, process);
		});
		
		launcher.getEndExecutionCallbacker().ifPresent(eec -> {
			eec.postStartupAction(this);
		});

		launcher.getCaptureStandardOutput().ifPresent(cso -> {
			cso.stdOutStreamConsumer(process.getInputStream(), this);
			cso.stdErrStreamConsumer(process.getErrorStream(), this);
		});
		
		process.onExit().thenRun(() -> {
			Runtime.getRuntime().removeShutdownHook(shutdown_hook);
			launcher.getEndExecutionCallbacker().ifPresent(eec -> {
				eec.onEndExecution(this);
			});
		});

	}
	
	@Override
	public String toString() {
		if (process.isAlive()) {
			return "Process #" + getPID() + " " + fullCommandLine + " ; since " + getUptime(TimeUnit.SECONDS) + " sec";
		} else {
			return "Exec " + getEndStatus() + " " + fullCommandLine;
		}
	}

	private static String processHandleToString(final ProcessHandle process_handle, final boolean verbose) {
		if (verbose) {
			return process_handle.info().command().orElse("<?>") + " #" + process_handle.pid() + " by " + process_handle.info().user().orElse("<?>") + " since " + process_handle.info().totalCpuDuration().orElse(Duration.ZERO).getSeconds() + " sec";
		} else {
			return process_handle.info().commandLine().orElse("<?>") + " #" + process_handle.pid();
		}
	}

	/**
	 * Blocking
	 */
	private void killProcessTree(final Process process) {
		log.debug("Internal kill " + toString());
		final List<ProcessHandle> cant_kill = process.descendants().filter(process_handle -> {
			return process_handle.isAlive();
		}).filter(process_handle -> {
			if (log.isDebugEnabled()) {
				log.info("Close manually process " + processHandleToString(process_handle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Close manually process " + processHandleToString(process_handle, false));
			}
			return process_handle.destroy() == false;
		}).filter(process_handle -> {
			if (log.isDebugEnabled()) {
				log.info("Force to close process " + processHandleToString(process_handle, true));
			} else if (log.isInfoEnabled()) {
				log.info("Force to close process " + processHandleToString(process_handle, false));
			}
			return process_handle.destroyForcibly() == false;
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
		
		if (cant_kill.isEmpty() == false) {
			cant_kill.forEach(process_handle -> {
				log.error("Can't force close process " + processHandleToString(process_handle, true));
			});
			throw new RuntimeException("Can't close process " + toString() + " for PID " + cant_kill.stream().map(p -> p.pid()).map(pid -> String.valueOf(pid)).collect(Collectors.joining(", ")));
		}
	}
	
	public Processlauncher getLauncher() {
		return launcher;
	}
	
	public Process getProcess() {
		return process;
	}
	
	public EndStatus getEndStatus() {
		if (process.isAlive()) {
			return EndStatus.NOT_YET_DONE;
		} else if (process_was_killed) {
			return EndStatus.KILLED;
		} else if (process_was_stopped_because_too_long_time) {
			return EndStatus.TOO_LONG_EXECUTION_TIME;
		} else if (launcher.isExecCodeMustBeZero() && process.exitValue() != 0) {
			return EndStatus.DONE_WITH_ERROR;
		}
		return EndStatus.CORRECTLY_DONE;
	}
	
	public boolean isCorrectlyDone() {
		return getEndStatus().equals(EndStatus.CORRECTLY_DONE);
	}
	
	public Integer getExitCode() {
		return process.exitValue();
	}
	
	public long getStartDate() {
		return process.info().startInstant().orElse(Instant.EPOCH).toEpochMilli();
	}
	
	public long getUptime(final TimeUnit unit) {
		return unit.convert(System.currentTimeMillis() - start_date, TimeUnit.MILLISECONDS);
	}
	
	public long getCPUDuration(final TimeUnit unit) {
		return unit.convert(process.info().totalCpuDuration().orElse(Duration.ZERO).toMillis(), TimeUnit.MILLISECONDS);
	}
	
	/**
	 * on Windows, return like "HOST_or_DOMAIN"\"username"
	 */
	public Optional<String> getUserExec() {
		return process.info().user();
	}
	
	public Optional<Long> getPID() {
		try {
			return Optional.of(process.pid());
		} catch (final UnsupportedOperationException e) {
			return Optional.empty();
		}
	}

	public Boolean isRunning() {
		return process.isAlive();
	}
	
	public boolean isKilled() {
		return process_was_killed;
	}

	public boolean isTooLongTime() {
		return process_was_stopped_because_too_long_time;
	}
	
	public ProcesslauncherLifecycle kill() {
		process_was_killed = true;
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
	
	public ProcesslauncherLifecycle checkExecution() {
		waitForEnd();
		if (isCorrectlyDone() == false) {
			throw new RuntimeException("Can't execute correcly " + fullCommandLine + "; " + getEndStatus() + " [" + getExitCode() + "]");
		}
		return this;
	}

	public synchronized StdInInjection getStdInInjection() {
		if (std_in_injection == null) {
			std_in_injection = new StdInInjection(process.getOutputStream());
		}
		return std_in_injection;
	}
	
}
